package app.aoki.quarkuscrud.resource;

import app.aoki.quarkuscrud.generated.model.EventEventResponse;
import app.aoki.quarkuscrud.service.EventEventBroadcaster;
import app.aoki.quarkuscrud.service.EventEventBroadcaster.EventEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.smallrye.mutiny.Multi;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.jboss.resteasy.reactive.RestStreamElementType;

/**
 * Implementation for live/real-time updates via Server-Sent Events (SSE). Note: This does not
 * implement the generated LiveApi interface because SSE requires returning Multi<T> instead of
 * Response, which is a Quarkus/Mutiny-specific feature not supported by standard JAX-RS code
 * generation.
 */
@ApplicationScoped
@Path("/api/live/events")
public class LiveApiImpl {

  @Inject EventEventBroadcaster eventBroadcaster;
  @Inject ObjectMapper objectMapper;

  /**
   * Server-Sent Events endpoint for real-time event updates. Clients can subscribe to this endpoint
   * to receive live notifications when events are created, updated, or deleted.
   */
  @GET
  @Produces(MediaType.SERVER_SENT_EVENTS)
  @RestStreamElementType(MediaType.APPLICATION_JSON)
  public Multi<EventEventResponse> streamEventEvents() {
    return eventBroadcaster.getEventStream().map(this::toEventEventResponse);
  }

  private EventEventResponse toEventEventResponse(EventEvent event) {
    EventEventResponse.EventTypeEnum eventType;
    switch (event.getType()) {
      case CREATED:
        eventType = EventEventResponse.EventTypeEnum.CREATED;
        break;
      case UPDATED:
        eventType = EventEventResponse.EventTypeEnum.UPDATED;
        break;
      case DELETED:
        eventType = EventEventResponse.EventTypeEnum.DELETED;
        break;
      default:
        throw new IllegalArgumentException("Unknown event type: " + event.getType());
    }

    EventEventResponse response = new EventEventResponse();
    response.setEventType(eventType);
    response.setEventId(event.getEvent().getId());
    response.setInitiatorUserId(event.getEvent().getInitiatorUserId());
    response.setTimestamp(LocalDateTime.now().atOffset(ZoneOffset.UTC));

    // Extract name and description from meta JSONB
    try {
      JsonNode metaNode = objectMapper.readTree(event.getEvent().getMeta());
      if (metaNode.has("name")) {
        response.setName(metaNode.get("name").asText());
      }
      if (metaNode.has("description")) {
        response.setDescription(metaNode.get("description").asText(""));
      }
    } catch (Exception e) {
      // If parsing fails, leave name and description as defaults
    }

    return response;
  }
}
