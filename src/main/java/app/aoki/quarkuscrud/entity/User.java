package app.aoki.quarkuscrud.entity;

import io.quarkus.runtime.annotations.RegisterForReflection;
import java.time.LocalDateTime;

/**
 * User entity representing basic user information.
 *
 * <p>This entity handles only the fundamental user information. Authentication details are stored
 * in the AuthnProvider entity.
 */
@RegisterForReflection
public class User {
  private Long id;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private UserLifecycleStatus lifecycleStatus;
  private Long currentProfileCardRevision;
  private String meta; // JSONB stored as String

  public User() {}

  public User(
      Long id,
      LocalDateTime createdAt,
      LocalDateTime updatedAt,
      UserLifecycleStatus lifecycleStatus,
      Long currentProfileCardRevision,
      String meta) {
    this.id = id;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
    this.lifecycleStatus = lifecycleStatus;
    this.currentProfileCardRevision = currentProfileCardRevision;
    this.meta = meta;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
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

  public UserLifecycleStatus getLifecycleStatus() {
    return lifecycleStatus;
  }

  public void setLifecycleStatus(UserLifecycleStatus lifecycleStatus) {
    this.lifecycleStatus = lifecycleStatus;
  }

  public Long getCurrentProfileCardRevision() {
    return currentProfileCardRevision;
  }

  public void setCurrentProfileCardRevision(Long currentProfileCardRevision) {
    this.currentProfileCardRevision = currentProfileCardRevision;
  }

  public String getMeta() {
    return meta;
  }

  public void setMeta(String meta) {
    this.meta = meta;
  }
}
