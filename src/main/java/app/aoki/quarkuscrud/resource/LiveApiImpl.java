package app.aoki.quarkuscrud.resource;

import app.aoki.quarkuscrud.generated.model.RoomEventResponse;
import app.aoki.quarkuscrud.service.RoomEventBroadcaster;
import app.aoki.quarkuscrud.service.RoomEventBroadcaster.RoomEvent;
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
@Path("/api/live/rooms")
public class LiveApiImpl {

  @Inject RoomEventBroadcaster eventBroadcaster;

  /**
   * Server-Sent Events endpoint for real-time room updates. Clients can subscribe to this endpoint
   * to receive live notifications when rooms are created, updated, or deleted.
   */
  @GET
  @Produces(MediaType.SERVER_SENT_EVENTS)
  @RestStreamElementType(MediaType.APPLICATION_JSON)
  public Multi<RoomEventResponse> streamRoomEvents() {
    return eventBroadcaster.getEventStream().map(this::toRoomEventResponse);
  }

  private RoomEventResponse toRoomEventResponse(RoomEvent event) {
    RoomEventResponse.EventTypeEnum eventType;
    switch (event.eventType()) {
      case RoomEventBroadcaster.EVENT_ROOM_CREATED:
        eventType = RoomEventResponse.EventTypeEnum.CREATED;
        break;
      case RoomEventBroadcaster.EVENT_ROOM_UPDATED:
        eventType = RoomEventResponse.EventTypeEnum.UPDATED;
        break;
      case RoomEventBroadcaster.EVENT_ROOM_DELETED:
        eventType = RoomEventResponse.EventTypeEnum.DELETED;
        break;
      default:
        throw new IllegalArgumentException("Unknown event type: " + event.eventType());
    }

    return new RoomEventResponse()
        .eventType(eventType)
        .roomId(event.room().getId())
        .name(event.room().getName())
        .description(event.room().getDescription())
        .userId(event.room().getUserId())
        .timestamp(LocalDateTime.now().atOffset(ZoneOffset.UTC));
  }
}
