package app.aoki.quarkuscrud.resource;

import app.aoki.quarkuscrud.entity.Event;
import app.aoki.quarkuscrud.entity.User;
import app.aoki.quarkuscrud.filter.Authenticated;
import app.aoki.quarkuscrud.filter.AuthenticatedUser;
import app.aoki.quarkuscrud.generated.api.EventsApi;
import app.aoki.quarkuscrud.generated.model.CreateEventRequest;
import app.aoki.quarkuscrud.generated.model.EventResponse;
import app.aoki.quarkuscrud.generated.model.UpdateEventRequest;
import app.aoki.quarkuscrud.service.EventService;
import app.aoki.quarkuscrud.support.ErrorResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@ApplicationScoped
@Path("/api/events")
public class EventsApiImpl implements EventsApi {

  @Inject EventService eventService;
  @Inject AuthenticatedUser authenticatedUser;
  @Inject ObjectMapper objectMapper;

  @Override
  @Authenticated
  public Response createEvent(CreateEventRequest createEventRequest) {
    User user = authenticatedUser.get();
    Event event =
        eventService.createEvent(
            createEventRequest.getName(), createEventRequest.getDescription(), user.getId());
    return Response.status(Response.Status.CREATED).entity(toEventResponse(event)).build();
  }

  @Override
  @Authenticated
  public Response deleteEvent(Long id) {
    User user = authenticatedUser.get();

    Optional<Event> existingEvent = eventService.findById(id);
    if (existingEvent.isEmpty()) {
      return Response.status(Response.Status.NOT_FOUND)
          .entity(new ErrorResponse("Event not found"))
          .build();
    }

    Event event = existingEvent.get();
    if (!event.getInitiatorUserId().equals(user.getId())) {
      return Response.status(Response.Status.FORBIDDEN)
          .entity(new ErrorResponse("You don't have permission to delete this event"))
          .build();
    }

    eventService.deleteEvent(id);
    return Response.noContent().build();
  }

  @Override
  public Response getAllEvents() {
    List<EventResponse> events =
        eventService.findAll().stream().map(this::toEventResponse).collect(Collectors.toList());
    return Response.ok(events).build();
  }

  @Override
  @Authenticated
  public Response getMyEvents() {
    User user = authenticatedUser.get();
    List<EventResponse> events =
        eventService.findByInitiatorUserId(user.getId()).stream()
            .map(this::toEventResponse)
            .collect(Collectors.toList());
    return Response.ok(events).build();
  }

  @Override
  public Response getEventById(Long id) {
    Optional<Event> event = eventService.findById(id);
    if (event.isEmpty()) {
      return Response.status(Response.Status.NOT_FOUND)
          .entity(new ErrorResponse("Event not found"))
          .build();
    }
    return Response.ok(toEventResponse(event.get())).build();
  }

  @Override
  @Authenticated
  public Response updateEvent(Long id, UpdateEventRequest updateEventRequest) {
    User user = authenticatedUser.get();

    Optional<Event> existingEvent = eventService.findById(id);
    if (existingEvent.isEmpty()) {
      return Response.status(Response.Status.NOT_FOUND)
          .entity(new ErrorResponse("Event not found"))
          .build();
    }

    Event event = existingEvent.get();
    if (!event.getInitiatorUserId().equals(user.getId())) {
      return Response.status(Response.Status.FORBIDDEN)
          .entity(new ErrorResponse("You don't have permission to update this event"))
          .build();
    }

    // Update name and description in meta JSONB
    event.setMeta(
        String.format(
            "{\"name\":\"%s\",\"description\":\"%s\"}",
            escapeJson(updateEventRequest.getName()),
            escapeJson(updateEventRequest.getDescription())));
    eventService.updateEvent(event);

    return Response.ok(toEventResponse(event)).build();
  }

  private EventResponse toEventResponse(Event event) {
    EventResponse response = new EventResponse();
    response.setId(event.getId());
    response.setInitiatorUserId(event.getInitiatorUserId());

    // Map EventStatus to StatusEnum
    EventResponse.StatusEnum statusEnum;
    switch (event.getStatus()) {
      case CREATED:
        statusEnum = EventResponse.StatusEnum.CREATED;
        break;
      case ACTIVE:
        statusEnum = EventResponse.StatusEnum.ACTIVE;
        break;
      case ENDED:
        statusEnum = EventResponse.StatusEnum.ENDED;
        break;
      case EXPIRED:
        statusEnum = EventResponse.StatusEnum.EXPIRED;
        break;
      case DELETED:
        statusEnum = EventResponse.StatusEnum.DELETED;
        break;
      default:
        statusEnum = EventResponse.StatusEnum.CREATED;
    }
    response.setStatus(statusEnum);

    response.setExpiresAt(event.getExpiresAt().atOffset(ZoneOffset.UTC));
    response.setCreatedAt(event.getCreatedAt().atOffset(ZoneOffset.UTC));
    response.setUpdatedAt(event.getUpdatedAt().atOffset(ZoneOffset.UTC));

    // Extract name and description from meta JSONB
    try {
      JsonNode metaNode = objectMapper.readTree(event.getMeta());
      if (metaNode.has("name")) {
        response.setName(metaNode.get("name").asText());
      }
      if (metaNode.has("description")) {
        response.setDescription(metaNode.get("description").asText(null));
      }
    } catch (Exception e) {
      // If parsing fails, leave name and description null
    }

    return response;
  }

  private String escapeJson(String value) {
    if (value == null) {
      return "";
    }
    return value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t");
  }
}
