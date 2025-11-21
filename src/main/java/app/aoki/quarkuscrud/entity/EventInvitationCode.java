package app.aoki.quarkuscrud.entity;

import io.quarkus.runtime.annotations.RegisterForReflection;
import java.time.LocalDateTime;

/**
 * Event invitation code entity.
 *
 * <p>Separated from Event table for performance. Invitation codes must be unique among
 * non-expired/non-deleted events. Code generation requires exclusive locking.
 */
@RegisterForReflection
public class EventInvitationCode {
  private Long id;
  private Long eventId;
  private String invitationCode;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public EventInvitationCode() {}

  public EventInvitationCode(
      Long id,
      Long eventId,
      String invitationCode,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {
    this.id = id;
    this.eventId = eventId;
    this.invitationCode = invitationCode;
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

  public String getInvitationCode() {
    return invitationCode;
  }

  public void setInvitationCode(String invitationCode) {
    this.invitationCode = invitationCode;
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
