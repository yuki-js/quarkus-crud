package app.aoki.entity;

import java.time.LocalDateTime;

/**
 * User entity representing all users in the system.
 *
 * <p>Users are equal regardless of authentication method. The authentication identifier is used for
 * anonymous authentication and as a reference for external authentication providers.
 */
public class User {
  private Long id;
  private String authIdentifier;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public User() {}

  public User(Long id, String authIdentifier, LocalDateTime createdAt, LocalDateTime updatedAt) {
    this.id = id;
    this.authIdentifier = authIdentifier;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getAuthIdentifier() {
    return authIdentifier;
  }

  public void setAuthIdentifier(String authIdentifier) {
    this.authIdentifier = authIdentifier;
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
