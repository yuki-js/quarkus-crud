package app.aoki.quarkuscrud.entity;

import java.time.LocalDateTime;

/**
 * User entity representing all users in the system.
 *
 * <p>Users are equal regardless of authentication method. The system supports multiple
 * authentication providers (anonymous, external OIDC, etc.) with a unified user model.
 *
 * <p>For anonymous users: authIdentifier is the primary identifier, externalSubject is null
 *
 * <p>For external provider users: externalSubject contains the provider's subject, authIdentifier
 * is an internal reference
 */
public class User {
  private Long id;
  private String authIdentifier;
  private AuthenticationProvider authProvider;
  private String externalSubject;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public User() {}

  public User(
      Long id,
      String authIdentifier,
      AuthenticationProvider authProvider,
      String externalSubject,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {
    this.id = id;
    this.authIdentifier = authIdentifier;
    this.authProvider = authProvider;
    this.externalSubject = externalSubject;
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

  public AuthenticationProvider getAuthProvider() {
    return authProvider;
  }

  public void setAuthProvider(AuthenticationProvider authProvider) {
    this.authProvider = authProvider;
  }

  public String getExternalSubject() {
    return externalSubject;
  }

  public void setExternalSubject(String externalSubject) {
    this.externalSubject = externalSubject;
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

  /**
   * Get the effective subject for JWT claims.
   *
   * <p>For anonymous users, returns authIdentifier. For external provider users, returns
   * externalSubject.
   *
   * @return the subject to use in JWT tokens
   */
  public String getEffectiveSubject() {
    return authProvider == AuthenticationProvider.ANONYMOUS ? authIdentifier : externalSubject;
  }
}
