package app.aoki.quarkuscrud.resource;

import app.aoki.quarkuscrud.entity.User;
import app.aoki.quarkuscrud.generated.api.EventsApi;
import app.aoki.quarkuscrud.generated.model.Event;
import app.aoki.quarkuscrud.generated.model.EventAttendee;
import app.aoki.quarkuscrud.generated.model.EventCreateRequest;
import app.aoki.quarkuscrud.generated.model.EventJoinByCodeRequest;
import app.aoki.quarkuscrud.support.Authenticated;
import app.aoki.quarkuscrud.support.AuthenticatedUser;
import app.aoki.quarkuscrud.support.ErrorResponse;
import app.aoki.quarkuscrud.usecase.EventUseCase;
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
import java.util.List;
import org.jboss.logging.Logger;

@ApplicationScoped
@Path("/api")
public class EventsApiImpl implements EventsApi {

  private static final Logger LOG = Logger.getLogger(EventsApiImpl.class);

  @Inject EventUseCase eventUseCase;
  @Inject AuthenticatedUser authenticatedUser;
  @Inject MeterRegistry meterRegistry;

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
      Event event = eventUseCase.createEvent(user.getId(), createEventRequest);
      meterRegistry.counter("events.created").increment();
      LOG.infof("Successfully created event");
      return Response.status(Response.Status.CREATED).entity(event).build();
    } catch (Exception e) {
      LOG.errorf(e, "Failed to create event for user ID: %d", user.getId());
      meterRegistry.counter("events.errors", "operation", "create").increment();
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity(new ErrorResponse("Failed to create event: " + e.getMessage()))
          .build();
    } finally {
      sample.stop(meterRegistry.timer("events.creation.time"));
    }
  }

  @Override
  @Authenticated
  @GET
  @Path("/events/{eventId}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getEventById(@PathParam("eventId") Long eventId) {
    LOG.debugf("Fetching event ID: %d", eventId);
    Timer.Sample sample = Timer.start(meterRegistry);

    try {
      return eventUseCase
          .getEventById(eventId)
          .map(
              event -> {
                meterRegistry.counter("events.read", "result", "found").increment();
                return Response.ok(event).build();
              })
          .orElseGet(
              () -> {
                LOG.warnf("Event not found with ID: %d", eventId);
                meterRegistry.counter("events.read", "result", "not_found").increment();
                return Response.status(Response.Status.NOT_FOUND)
                    .entity(new ErrorResponse("Event not found"))
                    .build();
              });
    } finally {
      sample.stop(meterRegistry.timer("events.read.time"));
    }
  }

  @Override
  @Authenticated
  @POST
  @Path("/events/join-by-code")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response joinEventByCode(EventJoinByCodeRequest joinEventByCodeRequest) {
    User user = authenticatedUser.get();
    LOG.infof("User %d attempting to join event by code", user.getId());

    try {
      EventAttendee attendee = eventUseCase.joinEventByCode(user.getId(), joinEventByCodeRequest);
      meterRegistry.counter("events.join", "result", "success").increment();
      LOG.infof("User %d successfully joined event", user.getId());
      return Response.status(Response.Status.CREATED).entity(attendee).build();
    } catch (IllegalArgumentException e) {
      LOG.warnf("Invalid join request from user %d: %s", user.getId(), e.getMessage());
      meterRegistry.counter("events.join", "result", "invalid").increment();
      return Response.status(Response.Status.BAD_REQUEST)
          .entity(new ErrorResponse(e.getMessage()))
          .build();
    } catch (IllegalStateException e) {
      LOG.warnf("Join conflict for user %d: %s", user.getId(), e.getMessage());
      meterRegistry.counter("events.join", "result", "conflict").increment();
      return Response.status(Response.Status.CONFLICT)
          .entity(new ErrorResponse(e.getMessage()))
          .build();
    } catch (Exception e) {
      LOG.errorf(e, "Failed to join event for user %d", user.getId());
      meterRegistry.counter("events.errors", "operation", "join").increment();
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
    try {
      List<EventAttendee> attendees = eventUseCase.listEventAttendees(eventId);
      return Response.ok(attendees).build();
    } catch (IllegalArgumentException e) {
      return Response.status(Response.Status.NOT_FOUND)
          .entity(new ErrorResponse(e.getMessage()))
          .build();
    }
  }

  @Override
  @Authenticated
  @GET
  @Path("/users/{userId}/events")
  @Produces(MediaType.APPLICATION_JSON)
  public Response listEventsByUser(@PathParam("userId") Long userId) {
    try {
      List<Event> events = eventUseCase.listEventsByUser(userId);
      return Response.ok(events).build();
    } catch (IllegalArgumentException e) {
      return Response.status(Response.Status.NOT_FOUND)
          .entity(new ErrorResponse(e.getMessage()))
          .build();
    }
  }

  @Override
  @Authenticated
  @GET
  @Path("/events/{eventId}/live")
  @Produces("text/event-stream")
  public Response streamEventLive(@PathParam("eventId") Long eventId) {
    if (!eventUseCase.eventExists(eventId)) {
      return Response.status(Response.Status.NOT_FOUND)
          .entity(new ErrorResponse("Event not found"))
          .build();
    }

    // SSE streaming implementation would go here
    // For now, return not implemented
    return Response.status(Response.Status.NOT_IMPLEMENTED)
        .entity(new ErrorResponse("Live streaming not yet implemented"))
        .build();
  }
}
