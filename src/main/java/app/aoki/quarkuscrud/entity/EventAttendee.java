package app.aoki.quarkuscrud.entity;

import io.quarkus.runtime.annotations.RegisterForReflection;
import java.time.LocalDateTime;

/**
 * Event attendee entity.
 *
 * <p>Represents a participant in a quiz event.
 */
@RegisterForReflection
public class EventAttendee {
  private Long id;
  private Long eventId;
  private Long attendeeUserId;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private String meta; // JSONB stored as String

  public EventAttendee() {}

  public EventAttendee(
      Long id,
      Long eventId,
      Long attendeeUserId,
      LocalDateTime createdAt,
      LocalDateTime updatedAt,
      String meta) {
    this.id = id;
    this.eventId = eventId;
    this.attendeeUserId = attendeeUserId;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
    this.meta = meta;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getEventId() {
    return eventId;
  }

  public void setEventId(Long eventId) {
    this.eventId = eventId;
  }

  public Long getAttendeeUserId() {
    return attendeeUserId;
  }

  public void setAttendeeUserId(Long attendeeUserId) {
    this.attendeeUserId = attendeeUserId;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(LocalDateTime createdAt) {
    this.createdAt = createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }

  public String getMeta() {
    return meta;
  }

  public void setMeta(String meta) {
    this.meta = meta;
  }
}
