package app.aoki.quarkuscrud.entity;

import io.quarkus.runtime.annotations.RegisterForReflection;
import java.time.LocalDateTime;

/**
 * Event attendee entity.
 *
 * <p>Represents a user's participation in an event. Each user can attend an event only once.
 */
@RegisterForReflection
public class EventAttendee {
  private Long id;
  private Long eventId;
  private Long attendeeUserId;
  private String meta;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public EventAttendee() {}

  public EventAttendee(
      Long id,
      Long eventId,
      Long attendeeUserId,
      String meta,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {
    this.id = id;
    this.eventId = eventId;
    this.attendeeUserId = attendeeUserId;
    this.meta = meta;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
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

  public String getMeta() {
    return meta;
  }

  public void setMeta(String meta) {
    this.meta = meta;
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
}
