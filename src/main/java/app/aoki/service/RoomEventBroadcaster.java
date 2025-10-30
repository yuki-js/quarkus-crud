package app.aoki.service;

import app.aoki.entity.Room;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Service for broadcasting room events to all connected clients via SSE.
 */
@ApplicationScoped
public class RoomEventBroadcaster {

    private final BroadcastProcessor<RoomEvent> processor = BroadcastProcessor.create();

    public static record RoomEvent(
            String eventType,
            Room room
    ) {}

    /**
     * Broadcast a room created event
     */
    public void broadcastRoomCreated(Room room) {
        processor.onNext(new RoomEvent("ROOM_CREATED", room));
    }

    /**
     * Broadcast a room updated event
     */
    public void broadcastRoomUpdated(Room room) {
        processor.onNext(new RoomEvent("ROOM_UPDATED", room));
    }

    /**
     * Broadcast a room deleted event
     */
    public void broadcastRoomDeleted(Room room) {
        processor.onNext(new RoomEvent("ROOM_DELETED", room));
    }

    /**
     * Get the stream of room events for SSE subscribers
     */
    public Multi<RoomEvent> getEventStream() {
        return processor;
    }
}
