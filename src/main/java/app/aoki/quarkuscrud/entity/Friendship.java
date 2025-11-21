package app.aoki.quarkuscrud.entity;

import io.quarkus.runtime.annotations.RegisterForReflection;
import java.time.LocalDateTime;

/**
 * Friendship entity representing unidirectional profile card exchange.
 *
 * <p>This represents a many-to-many relationship where users can send and receive profile cards.
 * The relationship is unidirectional (sender -> recipient).
 */
@RegisterForReflection
public class Friendship {
  private Long id;
  private Long senderId;
  private Long recipientId;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public Friendship() {}

  public Friendship(
      Long id, Long senderId, Long recipientId, LocalDateTime createdAt, LocalDateTime updatedAt) {
    this.id = id;
    this.senderId = senderId;
    this.recipientId = recipientId;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getSenderId() {
    return senderId;
  }

  public void setSenderId(Long senderId) {
    this.senderId = senderId;
  }

  public Long getRecipientId() {
    return recipientId;
  }

  public void setRecipientId(Long recipientId) {
    this.recipientId = recipientId;
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
