package app.aoki.quarkuscrud.service;

import app.aoki.quarkuscrud.entity.Event;
import io.quarkus.runtime.annotations.RegisterForReflection;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.operators.multi.processors.BroadcastProcessor;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Broadcasts event changes to SSE subscribers.
 *
 * <p>Similar to RoomEventBroadcaster but for Event entities. Provides real-time updates for event
 * creation, updates, and deletion.
 */
@ApplicationScoped
@RegisterForReflection
public class EventEventBroadcaster {

  private final BroadcastProcessor<EventEvent> processor = BroadcastProcessor.create();

  public Multi<EventEvent> getEventStream() {
    return processor;
  }

  public void broadcastEventCreated(Event event) {
    processor.onNext(new EventEvent(EventEventType.CREATED, event));
  }

  public void broadcastEventUpdated(Event event) {
    processor.onNext(new EventEvent(EventEventType.UPDATED, event));
  }

  public void broadcastEventDeleted(Event event) {
    processor.onNext(new EventEvent(EventEventType.DELETED, event));
  }

  @RegisterForReflection
  public static class EventEvent {
    private final EventEventType type;
    private final Event event;

    public EventEvent(EventEventType type, Event event) {
      this.type = type;
      this.event = event;
    }

    public EventEventType getType() {
      return type;
    }

    public Event getEvent() {
      return event;
    }
  }

  @RegisterForReflection
  public enum EventEventType {
    CREATED,
    UPDATED,
    DELETED
  }
}
