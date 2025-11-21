package app.aoki.quarkuscrud.resource;

import app.aoki.quarkuscrud.entity.User;
import app.aoki.quarkuscrud.filter.Authenticated;
import app.aoki.quarkuscrud.filter.AuthenticatedUser;
import app.aoki.quarkuscrud.generated.api.EventsApi;
import app.aoki.quarkuscrud.generated.model.Event;
import app.aoki.quarkuscrud.generated.model.EventAttendee;
import app.aoki.quarkuscrud.generated.model.EventCreateRequest;
import app.aoki.quarkuscrud.generated.model.EventJoinByCodeRequest;
import app.aoki.quarkuscrud.support.ErrorResponse;
import app.aoki.quarkuscrud.usecase.EventUseCase;
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
import org.postgresql.util.PSQLException;

@ApplicationScoped
@Path("/api")
public class EventsApiImpl implements EventsApi {

  @Inject EventUseCase eventUseCase;
  @Inject AuthenticatedUser authenticatedUser;

  @Override
  @Authenticated
  @POST
  @Path("/events")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response createEvent(EventCreateRequest createEventRequest) {
    User user = authenticatedUser.get();

    try {
      Event event = eventUseCase.createEvent(user.getId(), createEventRequest);
      return Response.status(Response.Status.CREATED).entity(event).build();
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
    return eventUseCase
        .getEventById(eventId)
        .map(event -> Response.ok(event).build())
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

    try {
      EventAttendee attendee = eventUseCase.joinEventByCode(user.getId(), joinEventByCodeRequest);
      return Response.status(Response.Status.CREATED).entity(attendee).build();
    } catch (IllegalArgumentException e) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity(new ErrorResponse(e.getMessage()))
          .build();
    } catch (IllegalStateException e) {
      return Response.status(Response.Status.CONFLICT)
          .entity(new ErrorResponse(e.getMessage()))
          .build();
    } catch (Exception e) {
      if (e.getCause() instanceof PSQLException psqlException
          && "23505".equals(psqlException.getSQLState())) {
        return Response.status(Response.Status.CONFLICT)
            .entity(new ErrorResponse("User already joined the event"))
            .build();
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
  @Produces({MediaType.SERVER_SENT_EVENTS, MediaType.APPLICATION_JSON})
  public Response streamEventLive(@PathParam("eventId") Long eventId) {
    if (!eventUseCase.eventExists(eventId)) {
      return Response.status(Response.Status.NOT_FOUND)
          .entity(new ErrorResponse("Event not found"))
          .build();
    }

    // TODO: Implement proper Server-Sent Events streaming
    return Response.ok()
        .entity(new ErrorResponse("Event live streaming not yet implemented"))
        .build();
  }
}
