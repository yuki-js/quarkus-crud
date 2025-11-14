package app.aoki.entity;

import java.time.LocalDateTime;

public class User {
  private Long id;
  private String guestToken;
  private String username;
  private String passwordHash;
  private String roles;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public User() {}

  public User(
      Long id,
      String guestToken,
      String username,
      String passwordHash,
      String roles,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {
    this.id = id;
    this.guestToken = guestToken;
    this.username = username;
    this.passwordHash = passwordHash;
    this.roles = roles;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getGuestToken() {
    return guestToken;
  }

  public void setGuestToken(String guestToken) {
    this.guestToken = guestToken;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public void setPasswordHash(String passwordHash) {
    this.passwordHash = passwordHash;
  }

  public String getRoles() {
    return roles;
  }

  public void setRoles(String roles) {
    this.roles = roles;
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
