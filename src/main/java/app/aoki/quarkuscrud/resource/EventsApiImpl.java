package app.aoki.quarkuscrud.resource;

import app.aoki.quarkuscrud.entity.Event;
import app.aoki.quarkuscrud.entity.EventAttendee;
import app.aoki.quarkuscrud.entity.EventInvitationCode;
import app.aoki.quarkuscrud.entity.EventStatus;
import app.aoki.quarkuscrud.entity.User;
import app.aoki.quarkuscrud.filter.Authenticated;
import app.aoki.quarkuscrud.filter.AuthenticatedUser;
import app.aoki.quarkuscrud.generated.api.EventsApi;
import app.aoki.quarkuscrud.generated.model.CreateEvent201Response;
import app.aoki.quarkuscrud.generated.model.CreateEventRequest;
import app.aoki.quarkuscrud.generated.model.JoinEventByCode201Response;
import app.aoki.quarkuscrud.generated.model.JoinEventByCodeRequest;
import app.aoki.quarkuscrud.mapper.EventAttendeeMapper;
import app.aoki.quarkuscrud.mapper.EventInvitationCodeMapper;
import app.aoki.quarkuscrud.mapper.EventMapper;
import app.aoki.quarkuscrud.mapper.UserMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.postgresql.util.PSQLException;

@ApplicationScoped
@Path("/api")
public class EventsApiImpl implements EventsApi {

  @Inject EventMapper eventMapper;
  @Inject EventAttendeeMapper eventAttendeeMapper;
  @Inject EventInvitationCodeMapper eventInvitationCodeMapper;
  @Inject UserMapper userMapper;
  @Inject AuthenticatedUser authenticatedUser;
  @Inject ObjectMapper objectMapper;

  private static final String INVITATION_CODE_CHARS =
      "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // Excluding confusing characters
  private static final int INVITATION_CODE_LENGTH = 8;
  private static final SecureRandom RANDOM = new SecureRandom();

  @Override
  @Authenticated
  public Response createEvent(CreateEventRequest createEventRequest) {
    User user = authenticatedUser.get();

    try {
      // Create event
      Event event = new Event();
      event.setInitiatorId(user.getId());
      event.setStatus(EventStatus.CREATED);
      event.setMeta(objectMapper.writeValueAsString(createEventRequest.getMeta()));
      event.setExpiresAt(
          createEventRequest.getExpiresAt() != null
              ? createEventRequest.getExpiresAt().toLocalDateTime()
              : null);
      LocalDateTime now = LocalDateTime.now();
      event.setCreatedAt(now);
      event.setUpdatedAt(now);

      eventMapper.insert(event);

      // Generate and insert invitation code
      String invitationCode = generateUniqueInvitationCode();
      EventInvitationCode code = new EventInvitationCode();
      code.setEventId(event.getId());
      code.setInvitationCode(invitationCode);
      code.setCreatedAt(now);
      code.setUpdatedAt(now);
      eventInvitationCodeMapper.insert(code);

      return Response.status(Response.Status.CREATED)
          .entity(toEventResponse(event, invitationCode))
          .build();
    } catch (Exception e) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity(
              new app.aoki.quarkuscrud.support.ErrorResponse(
                  "Failed to create event: " + e.getMessage()))
          .build();
    }
  }

  @Override
  @Authenticated
  public Response getEventById(@PathParam("eventId") Long eventId) {
    return eventMapper
        .findById(eventId)
        .map(
            event -> {
              String invitationCode =
                  eventInvitationCodeMapper.findByEventId(eventId).stream()
                      .findFirst()
                      .map(EventInvitationCode::getInvitationCode)
                      .orElse(null);
              return Response.ok(toEventResponse(event, invitationCode)).build();
            })
        .orElse(
            Response.status(Response.Status.NOT_FOUND)
                .entity(new app.aoki.quarkuscrud.support.ErrorResponse("Event not found"))
                .build());
  }

  @Override
  @Authenticated
  public Response joinEventByCode(JoinEventByCodeRequest joinEventByCodeRequest) {
    User user = authenticatedUser.get();
    String code = joinEventByCodeRequest.getInvitationCode();

    // Find event by invitation code
    EventInvitationCode invitationCode =
        eventInvitationCodeMapper
            .findByInvitationCode(code)
            .orElse(null); // We'll return 404 if not found after checking if event is active

    if (invitationCode == null) {
      return Response.status(Response.Status.NOT_FOUND)
          .entity(
              new app.aoki.quarkuscrud.support.ErrorResponse(
                  "No active event matches the invitation code"))
          .build();
    }

    Event event = eventMapper.findById(invitationCode.getEventId()).orElse(null);

    if (event == null
        || event.getStatus() == EventStatus.DELETED
        || event.getStatus() == EventStatus.EXPIRED) {
      return Response.status(Response.Status.NOT_FOUND)
          .entity(
              new app.aoki.quarkuscrud.support.ErrorResponse(
                  "No active event matches the invitation code"))
          .build();
    }

    // Check if user already joined
    if (eventAttendeeMapper.findByEventAndAttendee(event.getId(), user.getId()).isPresent()) {
      return Response.status(Response.Status.CONFLICT)
          .entity(new app.aoki.quarkuscrud.support.ErrorResponse("User already joined the event"))
          .build();
    }

    // Add user as attendee
    EventAttendee attendee = new EventAttendee();
    attendee.setEventId(event.getId());
    attendee.setAttendeeUserId(user.getId());
    attendee.setMeta(null);
    LocalDateTime now = LocalDateTime.now();
    attendee.setCreatedAt(now);
    attendee.setUpdatedAt(now);

    try {
      eventAttendeeMapper.insert(attendee);
      return Response.status(Response.Status.CREATED).entity(toAttendeeResponse(attendee)).build();
    } catch (Exception e) {
      // Handle unique constraint violation
      if (e.getCause() instanceof PSQLException) {
        PSQLException psqlException = (PSQLException) e.getCause();
        if (psqlException.getSQLState() != null && psqlException.getSQLState().equals("23505")) {
          return Response.status(Response.Status.CONFLICT)
              .entity(
                  new app.aoki.quarkuscrud.support.ErrorResponse("User already joined the event"))
              .build();
        }
      }
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity(
              new app.aoki.quarkuscrud.support.ErrorResponse(
                  "Failed to join event: " + e.getMessage()))
          .build();
    }
  }

  @Override
  @Authenticated
  public Response listEventAttendees(@PathParam("eventId") Long eventId) {
    // Verify event exists
    if (eventMapper.findById(eventId).isEmpty()) {
      return Response.status(Response.Status.NOT_FOUND)
          .entity(new app.aoki.quarkuscrud.support.ErrorResponse("Event not found"))
          .build();
    }

    List<EventAttendee> attendees = eventAttendeeMapper.findByEventId(eventId);
    List<JoinEventByCode201Response> responses =
        attendees.stream().map(this::toAttendeeResponse).collect(Collectors.toList());
    return Response.ok(responses).build();
  }

  @Override
  @Authenticated
  public Response listEventsByUser(@PathParam("userId") Long userId) {
    // Verify user exists
    if (userMapper.findById(userId).isEmpty()) {
      return Response.status(Response.Status.NOT_FOUND)
          .entity(new app.aoki.quarkuscrud.support.ErrorResponse("User not found"))
          .build();
    }

    List<Event> events = eventMapper.findByInitiatorId(userId);
    List<CreateEvent201Response> responses =
        events.stream()
            .map(
                event -> {
                  String invitationCode =
                      eventInvitationCodeMapper.findByEventId(event.getId()).stream()
                          .findFirst()
                          .map(EventInvitationCode::getInvitationCode)
                          .orElse(null);
                  return toEventResponse(event, invitationCode);
                })
            .collect(Collectors.toList());
    return Response.ok(responses).build();
  }

  @Override
  @Authenticated
  public Response streamEventLive(@PathParam("eventId") Long eventId) {
    // For now, return a simple response. SSE implementation would require more setup
    // This is a placeholder that returns JSON instead of SSE
    if (eventMapper.findById(eventId).isEmpty()) {
      return Response.status(Response.Status.NOT_FOUND)
          .entity(new app.aoki.quarkuscrud.support.ErrorResponse("Event not found"))
          .build();
    }

    // Return a simple status response for now
    // TODO: Implement proper Server-Sent Events streaming
    return Response.ok()
        .entity(
            new app.aoki.quarkuscrud.support.ErrorResponse(
                "Event live streaming not yet implemented"))
        .build();
  }

  private CreateEvent201Response toEventResponse(Event event, String invitationCode) {
    CreateEvent201Response response = new CreateEvent201Response();
    response.setId(event.getId());
    response.setInitiatorId(event.getInitiatorId());
    response.setStatus(CreateEvent201Response.StatusEnum.fromValue(event.getStatus().getValue()));
    response.setInvitationCode(invitationCode);
    response.setExpiresAt(
        event.getExpiresAt() != null ? event.getExpiresAt().atOffset(ZoneOffset.UTC) : null);
    response.setCreatedAt(event.getCreatedAt().atOffset(ZoneOffset.UTC));
    response.setUpdatedAt(
        event.getUpdatedAt() != null ? event.getUpdatedAt().atOffset(ZoneOffset.UTC) : null);

    // Parse JSON meta
    if (event.getMeta() != null) {
      try {
        Map<String, Object> meta =
            objectMapper.readValue(event.getMeta(), new TypeReference<>() {});
        response.setMeta(meta);
      } catch (Exception e) {
        response.setMeta(new HashMap<>());
      }
    } else {
      response.setMeta(new HashMap<>());
    }

    return response;
  }

  private JoinEventByCode201Response toAttendeeResponse(EventAttendee attendee) {
    JoinEventByCode201Response response = new JoinEventByCode201Response();
    response.setId(attendee.getId());
    response.setEventId(attendee.getEventId());
    response.setAttendeeUserId(attendee.getAttendeeUserId());
    response.setCreatedAt(attendee.getCreatedAt().atOffset(ZoneOffset.UTC));
    response.setUpdatedAt(attendee.getUpdatedAt().atOffset(ZoneOffset.UTC));

    // Parse JSON meta
    if (attendee.getMeta() != null) {
      try {
        Map<String, Object> meta =
            objectMapper.readValue(attendee.getMeta(), new TypeReference<>() {});
        response.setMeta(meta);
      } catch (Exception e) {
        response.setMeta(new HashMap<>());
      }
    } else {
      response.setMeta(new HashMap<>());
    }

    return response;
  }

  private String generateUniqueInvitationCode() {
    // Simple implementation - in production, you'd want to ensure uniqueness
    StringBuilder code = new StringBuilder(INVITATION_CODE_LENGTH);
    for (int i = 0; i < INVITATION_CODE_LENGTH; i++) {
      code.append(INVITATION_CODE_CHARS.charAt(RANDOM.nextInt(INVITATION_CODE_CHARS.length())));
    }
    return code.toString();
  }
}
