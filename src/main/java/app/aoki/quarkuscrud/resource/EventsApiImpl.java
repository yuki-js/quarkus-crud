package app.aoki.quarkuscrud.resource;

import app.aoki.quarkuscrud.entity.User;
import app.aoki.quarkuscrud.filter.Authenticated;
import app.aoki.quarkuscrud.filter.AuthenticatedUser;
import app.aoki.quarkuscrud.generated.api.EventsApi;
import app.aoki.quarkuscrud.generated.model.Event;
import app.aoki.quarkuscrud.generated.model.EventAttendee;
import app.aoki.quarkuscrud.generated.model.EventCreateRequest;
import app.aoki.quarkuscrud.generated.model.EventJoinByCodeRequest;
import app.aoki.quarkuscrud.service.EventService;
import app.aoki.quarkuscrud.service.UserService;
import app.aoki.quarkuscrud.support.ErrorResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.postgresql.util.PSQLException;

@ApplicationScoped
@Path("/api")
public class EventsApiImpl implements EventsApi {

  @Inject EventService eventService;
  @Inject UserService userService;
  @Inject AuthenticatedUser authenticatedUser;
  @Inject ObjectMapper objectMapper;

  @Override
  @Authenticated
  @POST
  @Path("/events")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response createEvent(EventCreateRequest createEventRequest) {
    User user = authenticatedUser.get();

    try {
      String meta = objectMapper.writeValueAsString(createEventRequest.getMeta());
      app.aoki.quarkuscrud.entity.Event event =
          eventService.createEvent(
              user.getId(), meta, eventService.toLocalDateTime(createEventRequest.getExpiresAt()));

      String invitationCode = eventService.getInvitationCode(event.getId()).orElse(null);
      return Response.status(Response.Status.CREATED)
          .entity(toEventResponse(event, invitationCode))
          .build();
    } catch (Exception e) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity(new ErrorResponse("Failed to create event: " + e.getMessage()))
          .build();
    }
  }

  @Override
  @Authenticated
  @GET
  @Path("/events/{eventId}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getEventById(@PathParam("eventId") Long eventId) {
    return eventService
        .findById(eventId)
        .map(
            event -> {
              String invitationCode = eventService.getInvitationCode(eventId).orElse(null);
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

    app.aoki.quarkuscrud.entity.Event event =
        eventService.findActiveEventByInvitationCode(code).orElse(null);
    if (event == null) {
      return Response.status(Response.Status.NOT_FOUND)
          .entity(new ErrorResponse("No active event matches the invitation code"))
          .build();
    }

    // Check if user already joined
    if (eventService.isUserAttendee(event.getId(), user.getId())) {
      return Response.status(Response.Status.CONFLICT)
          .entity(new ErrorResponse("User already joined the event"))
          .build();
    }

    // Add user as attendee
    try {
      app.aoki.quarkuscrud.entity.EventAttendee attendee =
          eventService.addAttendee(event.getId(), user.getId(), null);
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
    if (eventService.findById(eventId).isEmpty()) {
      return Response.status(Response.Status.NOT_FOUND)
          .entity(new ErrorResponse("Event not found"))
          .build();
    }

    List<app.aoki.quarkuscrud.entity.EventAttendee> attendees = eventService.listAttendees(eventId);
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
    if (userService.findById(userId).isEmpty()) {
      return Response.status(Response.Status.NOT_FOUND)
          .entity(new ErrorResponse("User not found"))
          .build();
    }

    List<app.aoki.quarkuscrud.entity.Event> events = eventService.findByInitiatorId(userId);
    List<Event> responses =
        events.stream()
            .map(
                event -> {
                  String invitationCode =
                      eventService.getInvitationCode(event.getId()).orElse(null);
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
    if (eventService.findById(eventId).isEmpty()) {
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
}
