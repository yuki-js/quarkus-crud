package app.aoki.quarkuscrud.entity;

import io.quarkus.runtime.annotations.RegisterForReflection;
import java.time.LocalDateTime;

/**
 * Friendship entity.
 *
 * <p>Represents a unidirectional connection between users who have exchanged profile cards. The
 * connection is from sender to recipient.
 */
@RegisterForReflection
public class Friendship {
  private Long id;
  private Long senderUserId;
  private Long recipientUserId;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public Friendship() {}

  public Friendship(
      Long id,
      Long senderUserId,
      Long recipientUserId,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {
    this.id = id;
    this.senderUserId = senderUserId;
    this.recipientUserId = recipientUserId;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getSenderUserId() {
    return senderUserId;
  }

  public void setSenderUserId(Long senderUserId) {
    this.senderUserId = senderUserId;
  }

  public Long getRecipientUserId() {
    return recipientUserId;
  }

  public void setRecipientUserId(Long recipientUserId) {
    this.recipientUserId = recipientUserId;
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
