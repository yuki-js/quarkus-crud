package app.aoki.quarkuscrud.resource;

import app.aoki.quarkuscrud.entity.EventInvitationCode;
import app.aoki.quarkuscrud.entity.EventStatus;
import app.aoki.quarkuscrud.entity.User;
import app.aoki.quarkuscrud.filter.Authenticated;
import app.aoki.quarkuscrud.filter.AuthenticatedUser;
import app.aoki.quarkuscrud.generated.api.EventsApi;
import app.aoki.quarkuscrud.generated.model.Event;
import app.aoki.quarkuscrud.generated.model.EventAttendee;
import app.aoki.quarkuscrud.generated.model.EventCreateRequest;
import app.aoki.quarkuscrud.generated.model.EventJoinByCodeRequest;
import app.aoki.quarkuscrud.mapper.EventAttendeeMapper;
import app.aoki.quarkuscrud.mapper.EventInvitationCodeMapper;
import app.aoki.quarkuscrud.mapper.EventMapper;
import app.aoki.quarkuscrud.mapper.UserMapper;
import app.aoki.quarkuscrud.support.ErrorResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.jboss.logging.Logger;
import org.postgresql.util.PSQLException;

@ApplicationScoped
@Path("/api")
public class EventsApiImpl implements EventsApi {

  private static final Logger LOG = Logger.getLogger(EventsApiImpl.class);

  @Inject EventMapper eventMapper;
  @Inject EventAttendeeMapper eventAttendeeMapper;
  @Inject EventInvitationCodeMapper eventInvitationCodeMapper;
  @Inject UserMapper userMapper;
  @Inject AuthenticatedUser authenticatedUser;
  @Inject ObjectMapper objectMapper;
  @Inject MeterRegistry meterRegistry;

  private static final String INVITATION_CODE_CHARS =
      "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"; // Excluding confusing characters
  private static final int INVITATION_CODE_LENGTH = 8;

  @Override
  @Authenticated
  @POST
  @Path("/events")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response createEvent(EventCreateRequest createEventRequest) {
    User user = authenticatedUser.get();
    LOG.infof("Creating event for user ID: %d", user.getId());
    Timer.Sample sample = Timer.start(meterRegistry);

    try {
      // Create event
      app.aoki.quarkuscrud.entity.Event event = new app.aoki.quarkuscrud.entity.Event();
      event.setInitiatorId(user.getId());
      event.setStatus(EventStatus.CREATED);
      event.setMeta(objectMapper.writeValueAsString(createEventRequest.getMeta()));
      event.setExpiresAt(toLocalDateTime(createEventRequest.getExpiresAt()));
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

      Counter.builder("events.created")
          .description("Number of events created")
          .register(meterRegistry)
          .increment();

      LOG.infof("Successfully created event ID: %d", event.getId());
      return Response.status(Response.Status.CREATED)
          .entity(toEventResponse(event, invitationCode))
          .build();
    } catch (Exception e) {
      LOG.errorf(e, "Failed to create event for user ID: %d", user.getId());
      Counter.builder("events.errors")
          .description("Event operation errors")
          .tag("operation", "create")
          .register(meterRegistry)
          .increment();
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity(new ErrorResponse("Failed to create event: " + e.getMessage()))
          .build();
    } finally {
      sample.stop(
          Timer.builder("events.creation.time")
              .description("Time to create an event")
              .register(meterRegistry));
    }
  }

  @Override
  @Authenticated
  @GET
  @Path("/events/{eventId}")
  @Produces(MediaType.APPLICATION_JSON)
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
                .entity(new ErrorResponse("Event not found"))
                .build());
  }

  @Override
  @Authenticated
  @POST
  @Path("/events/join-by-code")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response joinEventByCode(EventJoinByCodeRequest joinEventByCodeRequest) {
    User user = authenticatedUser.get();
    String code = joinEventByCodeRequest.getInvitationCode();

    if (code == null || code.isBlank()) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity(new ErrorResponse("invitationCode is required to join an event"))
          .build();
    }

    EventInvitationCode invitationCode =
        eventInvitationCodeMapper.findByInvitationCode(code).orElse(null);
    if (invitationCode == null) {
      return Response.status(Response.Status.NOT_FOUND)
          .entity(new ErrorResponse("No active event matches the invitation code"))
          .build();
    }

    app.aoki.quarkuscrud.entity.Event event =
        eventMapper.findById(invitationCode.getEventId()).orElse(null);

    if (event == null
        || event.getStatus() == EventStatus.DELETED
        || event.getStatus() == EventStatus.EXPIRED) {
      return Response.status(Response.Status.NOT_FOUND)
          .entity(new ErrorResponse("No active event matches the invitation code"))
          .build();
    }

    // Check if user already joined
    if (eventAttendeeMapper.findByEventAndAttendee(event.getId(), user.getId()).isPresent()) {
      return Response.status(Response.Status.CONFLICT)
          .entity(new ErrorResponse("User already joined the event"))
          .build();
    }

    // Add user as attendee
    app.aoki.quarkuscrud.entity.EventAttendee attendee =
        new app.aoki.quarkuscrud.entity.EventAttendee();
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
              .entity(new ErrorResponse("User already joined the event"))
              .build();
        }
      }
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity(new ErrorResponse("Failed to join event: " + e.getMessage()))
          .build();
    }
  }

  @Override
  @Authenticated
  @GET
  @Path("/events/{eventId}/attendees")
  @Produces(MediaType.APPLICATION_JSON)
  public Response listEventAttendees(@PathParam("eventId") Long eventId) {
    // Verify event exists
    if (eventMapper.findById(eventId).isEmpty()) {
      return Response.status(Response.Status.NOT_FOUND)
          .entity(new ErrorResponse("Event not found"))
          .build();
    }

    List<app.aoki.quarkuscrud.entity.EventAttendee> attendees =
        eventAttendeeMapper.findByEventId(eventId);
    List<EventAttendee> responses =
        attendees.stream().map(this::toAttendeeResponse).collect(Collectors.toList());
    return Response.ok(responses).build();
  }

  @Override
  @Authenticated
  @GET
  @Path("/users/{userId}/events")
  @Produces(MediaType.APPLICATION_JSON)
  public Response listEventsByUser(@PathParam("userId") Long userId) {
    // Verify user exists
    if (userMapper.findById(userId).isEmpty()) {
      return Response.status(Response.Status.NOT_FOUND)
          .entity(new ErrorResponse("User not found"))
          .build();
    }

    List<app.aoki.quarkuscrud.entity.Event> events = eventMapper.findByInitiatorId(userId);
    List<Event> responses =
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
  @GET
  @Path("/events/{eventId}/live")
  @Produces({MediaType.SERVER_SENT_EVENTS, MediaType.APPLICATION_JSON})
  public Response streamEventLive(@PathParam("eventId") Long eventId) {
    // For now, return a simple response. SSE implementation would require more setup
    // This is a placeholder that returns JSON instead of SSE
    if (eventMapper.findById(eventId).isEmpty()) {
      return Response.status(Response.Status.NOT_FOUND)
          .entity(new ErrorResponse("Event not found"))
          .build();
    }

    // Return a simple status response for now
    // TODO: Implement proper Server-Sent Events streaming
    return Response.ok()
        .entity(new ErrorResponse("Event live streaming not yet implemented"))
        .build();
  }

  private Event toEventResponse(app.aoki.quarkuscrud.entity.Event event, String invitationCode) {
    Event response = new Event();
    response.setId(event.getId());
    response.setInitiatorId(event.getInitiatorId());
    response.setStatus(Event.StatusEnum.fromValue(event.getStatus().getValue()));
    if (invitationCode != null) {
      response.setInvitationCode(invitationCode);
    }
    if (event.getExpiresAt() != null) {
      response.setExpiresAt(event.getExpiresAt().atOffset(ZoneOffset.UTC));
    }
    response.setCreatedAt(event.getCreatedAt().atOffset(ZoneOffset.UTC));
    if (event.getUpdatedAt() != null) {
      response.setUpdatedAt(event.getUpdatedAt().atOffset(ZoneOffset.UTC));
    }

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

  private EventAttendee toAttendeeResponse(app.aoki.quarkuscrud.entity.EventAttendee attendee) {
    EventAttendee response = new EventAttendee();
    response.setId(attendee.getId());
    response.setEventId(attendee.getEventId());
    response.setAttendeeUserId(attendee.getAttendeeUserId());
    response.setCreatedAt(attendee.getCreatedAt().atOffset(ZoneOffset.UTC));
    if (attendee.getUpdatedAt() != null) {
      response.setUpdatedAt(attendee.getUpdatedAt().atOffset(ZoneOffset.UTC));
    }

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
    SecureRandom rnd = new SecureRandom(); // created at runtime
    for (int i = 0; i < INVITATION_CODE_LENGTH; i++) {
      code.append(INVITATION_CODE_CHARS.charAt(rnd.nextInt(INVITATION_CODE_CHARS.length())));
    }
    return code.toString();
  }

  private LocalDateTime toLocalDateTime(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof OffsetDateTime offsetDateTime) {
      return offsetDateTime.toLocalDateTime();
    }
    if (value instanceof String text && !text.isBlank()) {
      try {
        return OffsetDateTime.parse(text).toLocalDateTime();
      } catch (Exception e) {
        LOG.warnf("Failed to parse datetime string: %s - %s", text, e.getMessage());
        return null;
      }
    }
    return null;
  }
}
