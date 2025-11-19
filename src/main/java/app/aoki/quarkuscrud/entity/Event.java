package app.aoki.quarkuscrud.entity;

import io.quarkus.runtime.annotations.RegisterForReflection;
import java.time.LocalDateTime;

/**
 * Event entity (formerly "Room").
 *
 * <p>Represents a quiz event with participants and invitation codes.
 */
@RegisterForReflection
public class Event {
  private Long id;
  private Long initiatorUserId;
  private EventStatus status;
  private String meta; // JSONB stored as String
  private LocalDateTime expiresAt;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public Event() {}

  public Event(
      Long id,
      Long initiatorUserId,
      EventStatus status,
      String meta,
      LocalDateTime expiresAt,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {
    this.id = id;
    this.initiatorUserId = initiatorUserId;
    this.status = status;
    this.meta = meta;
    this.expiresAt = expiresAt;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getInitiatorUserId() {
    return initiatorUserId;
  }

  public void setInitiatorUserId(Long initiatorUserId) {
    this.initiatorUserId = initiatorUserId;
  }

  public EventStatus getStatus() {
    return status;
  }

  public void setStatus(EventStatus status) {
    this.status = status;
  }

  public String getMeta() {
    return meta;
  }

  public void setMeta(String meta) {
    this.meta = meta;
  }

  public LocalDateTime getExpiresAt() {
    return expiresAt;
  }

  public void setExpiresAt(LocalDateTime expiresAt) {
    this.expiresAt = expiresAt;
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
