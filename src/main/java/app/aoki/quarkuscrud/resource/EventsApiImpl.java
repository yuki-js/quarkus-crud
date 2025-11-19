package app.aoki.quarkuscrud.resource;

import app.aoki.quarkuscrud.entity.Event;
import app.aoki.quarkuscrud.entity.EventAttendee;
import app.aoki.quarkuscrud.entity.EventStatus;
import app.aoki.quarkuscrud.entity.User;
import app.aoki.quarkuscrud.filter.Authenticated;
import app.aoki.quarkuscrud.filter.AuthenticatedUser;
import app.aoki.quarkuscrud.generated.api.EventsApi;
import app.aoki.quarkuscrud.generated.model.CreateEvent201Response;
import app.aoki.quarkuscrud.generated.model.CreateEventRequest;
import app.aoki.quarkuscrud.generated.model.CreateGuestUser500Response;
import app.aoki.quarkuscrud.generated.model.JoinEventByCode201Response;
import app.aoki.quarkuscrud.generated.model.JoinEventByCodeRequest;
import app.aoki.quarkuscrud.generated.model.StreamEventLive200Response;
import app.aoki.quarkuscrud.service.EventService;
import app.aoki.quarkuscrud.service.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@ApplicationScoped
@Path("/api")
public class EventsApiImpl implements EventsApi {

  @Inject EventService eventService;
  @Inject UserService userService;
  @Inject AuthenticatedUser authenticatedUser;
  @Inject ObjectMapper objectMapper;

  @Override
  @Authenticated
  public Response createEvent(CreateEventRequest request) {
    User user = authenticatedUser.get();

    try {
      String metaJson = null;
      if (request.getMeta() != null && !request.getMeta().isEmpty()) {
        metaJson = objectMapper.writeValueAsString(request.getMeta());
      }

      LocalDateTime expiresAt =
          request.getExpiresAt() != null
              ? request.getExpiresAt().toLocalDateTime()
              : LocalDateTime.now().plusHours(24); // Default 24 hours

      Event event = eventService.createEvent(user.getId(), expiresAt, metaJson);

      // Update status to active
      eventService.updateStatus(event.getId(), EventStatus.ACTIVE);
      event.setStatus(EventStatus.ACTIVE);

      return Response.status(201).entity(toEventResponse(event)).build();
    } catch (JsonProcessingException e) {
      CreateGuestUser500Response error = new CreateGuestUser500Response();
      error.setError("Invalid event metadata");
      return Response.status(400).entity(error).build();
    }
  }

  @Override
  @Authenticated
  public Response getEventById(Long eventId) {
    Optional<Event> eventOpt = eventService.findById(eventId);
    if (eventOpt.isEmpty()) {
      CreateGuestUser500Response error = new CreateGuestUser500Response();
      error.setError("Event not found");
      return Response.status(404).entity(error).build();
    }

    return Response.ok(toEventResponse(eventOpt.get())).build();
  }

  @Override
  @Authenticated
  public Response joinEventByCode(JoinEventByCodeRequest request) {
    User user = authenticatedUser.get();
    String code = request.getInvitationCode();

    Optional<Event> eventOpt = eventService.findByInvitationCode(code);
    if (eventOpt.isEmpty()) {
      CreateGuestUser500Response error = new CreateGuestUser500Response();
      error.setError("No active event found for this invitation code");
      return Response.status(404).entity(error).build();
    }

    Event event = eventOpt.get();

    // Check if already attending
    if (eventService.isAttending(event.getId(), user.getId())) {
      CreateGuestUser500Response error = new CreateGuestUser500Response();
      error.setError("User already attending this event");
      return Response.status(409).entity(error).build();
    }

    EventAttendee attendee = eventService.addAttendee(event.getId(), user.getId());

    return Response.status(201).entity(toAttendeeResponse(attendee)).build();
  }

  @Override
  @Authenticated
  public Response listEventAttendees(Long eventId) {
    // Verify event exists
    Optional<Event> eventOpt = eventService.findById(eventId);
    if (eventOpt.isEmpty()) {
      CreateGuestUser500Response error = new CreateGuestUser500Response();
      error.setError("Event not found");
      return Response.status(404).entity(error).build();
    }

    List<EventAttendee> attendees = eventService.getAttendees(eventId);
    List<JoinEventByCode201Response> responses =
        attendees.stream().map(this::toAttendeeResponse).collect(Collectors.toList());

    return Response.ok(responses).build();
  }

  @Override
  @Authenticated
  public Response listEventsByUser(Long userId) {
    // Verify user exists
    if (userService.findById(userId).isEmpty()) {
      CreateGuestUser500Response error = new CreateGuestUser500Response();
      error.setError("User not found");
      return Response.status(404).entity(error).build();
    }

    List<Event> events = eventService.findByInitiatorId(userId);
    List<CreateEvent201Response> responses =
        events.stream().map(this::toEventResponse).collect(Collectors.toList());

    return Response.ok(responses).build();
  }

  @Override
  @Authenticated
  public Response streamEventLive(Long eventId) {
    // Verify event exists
    Optional<Event> eventOpt = eventService.findById(eventId);
    if (eventOpt.isEmpty()) {
      CreateGuestUser500Response error = new CreateGuestUser500Response();
      error.setError("Event not found");
      return Response.status(404).entity(error).build();
    }

    // For now, return a simple response. SSE implementation would require additional infrastructure
    StreamEventLive200Response response = new StreamEventLive200Response();
    response.setEventType(StreamEventLive200Response.EventTypeEnum.JOINED);
    response.setEventId(eventId);
    response.setAttendeeUserId(0L);
    response.setTimestamp(LocalDateTime.now().atOffset(ZoneOffset.UTC));

    return Response.ok(response).build();
  }

  private CreateEvent201Response toEventResponse(Event event) {
    CreateEvent201Response response = new CreateEvent201Response();
    response.setId(event.getId());
    response.setInitiatorId(event.getInitiatorId());
    response.setStatus(mapEventStatus(event.getStatus()));

    if (event.getMeta() != null) {
      try {
        @SuppressWarnings("unchecked")
        Map<String, Object> metaMap = objectMapper.readValue(event.getMeta(), Map.class);
        response.setMeta(metaMap);
      } catch (JsonProcessingException e) {
        // Leave meta null if invalid
      }
    }

    // Get invitation code
    eventService.getInvitationCode(event.getId()).ifPresent(response::setInvitationCode);

    if (event.getExpiresAt() != null) {
      response.setExpiresAt(event.getExpiresAt().atOffset(ZoneOffset.UTC));
    }
    response.setCreatedAt(event.getCreatedAt().atOffset(ZoneOffset.UTC));
    response.setUpdatedAt(event.getUpdatedAt().atOffset(ZoneOffset.UTC));

    return response;
  }

  private JoinEventByCode201Response toAttendeeResponse(EventAttendee attendee) {
    JoinEventByCode201Response response = new JoinEventByCode201Response();
    response.setId(attendee.getId());
    response.setEventId(attendee.getEventId());
    response.setAttendeeUserId(attendee.getAttendeeUserId());

    if (attendee.getMeta() != null) {
      try {
        @SuppressWarnings("unchecked")
        Map<String, Object> metaMap = objectMapper.readValue(attendee.getMeta(), Map.class);
        response.setMeta(metaMap);
      } catch (JsonProcessingException e) {
        // Leave meta null if invalid
      }
    }

    response.setCreatedAt(attendee.getCreatedAt().atOffset(ZoneOffset.UTC));
    response.setUpdatedAt(attendee.getUpdatedAt().atOffset(ZoneOffset.UTC));

    return response;
  }

  private CreateEvent201Response.StatusEnum mapEventStatus(EventStatus status) {
    if (status == null) {
      return null;
    }
    return CreateEvent201Response.StatusEnum.fromValue(status.name().toLowerCase());
  }
}
