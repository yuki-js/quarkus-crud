package app.aoki.resource;

import app.aoki.service.RoomEventBroadcaster;
import app.aoki.service.RoomEventBroadcaster.RoomEvent;
import io.smallrye.mutiny.Multi;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.time.LocalDateTime;
import org.jboss.resteasy.reactive.RestStreamElementType;

/** Resource for live/real-time updates via Server-Sent Events (SSE). */
@Path("/api/live")
public class LiveResource {

  @Inject RoomEventBroadcaster eventBroadcaster;

  /** Response for room events sent via SSE. */
  public static record RoomEventResponse(
      String eventType,
      Long roomId,
      String name,
      String description,
      Long userId,
      LocalDateTime timestamp) {
    public static RoomEventResponse from(RoomEvent event) {
      return new RoomEventResponse(
          event.eventType(),
          event.room().getId(),
          event.room().getName(),
          event.room().getDescription(),
          event.room().getUserId(),
          LocalDateTime.now());
    }
  }

  /**
   * Server-Sent Events endpoint for real-time room updates. Clients can subscribe to this endpoint
   * to receive live notifications when rooms are created, updated, or deleted.
   *
   * <p>Example usage with curl: curl -N http://localhost:8080/api/live/rooms
   *
   * <p>Example usage with JavaScript: const eventSource = new EventSource('/api/live/rooms');
   * eventSource.onmessage = (event) => { const data = JSON.parse(event.data); console.log('Room
   * event:', data); };
   */
  @GET
  @Path("/rooms")
  @Produces(MediaType.SERVER_SENT_EVENTS)
  @RestStreamElementType(MediaType.APPLICATION_JSON)
  public Multi<RoomEventResponse> streamRoomEvents() {
    return eventBroadcaster.getEventStream().map(RoomEventResponse::from);
  }
}
