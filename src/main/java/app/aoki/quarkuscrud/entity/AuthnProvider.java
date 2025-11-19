package app.aoki.quarkuscrud.entity;

import io.quarkus.runtime.annotations.RegisterForReflection;
import java.time.LocalDateTime;

/**
 * Authentication provider entity.
 *
 * <p>Stores authentication provider information for users. A user can have multiple authentication
 * providers.
 */
@RegisterForReflection
public class AuthnProvider {
  private Long id;
  private Long userId;
  private AuthenticationMethod authMethod;
  private String authIdentifier;
  private String externalSubject;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public AuthnProvider() {}

  public AuthnProvider(
      Long id,
      Long userId,
      AuthenticationMethod authMethod,
      String authIdentifier,
      String externalSubject,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {
    this.id = id;
    this.userId = userId;
    this.authMethod = authMethod;
    this.authIdentifier = authIdentifier;
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

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  public AuthenticationMethod getAuthMethod() {
    return authMethod;
  }

  public void setAuthMethod(AuthenticationMethod authMethod) {
    this.authMethod = authMethod;
  }

  public String getAuthIdentifier() {
    return authIdentifier;
  }

  public void setAuthIdentifier(String authIdentifier) {
    this.authIdentifier = authIdentifier;
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
   * @return the subject to use in JWT tokens
   */
  public String getEffectiveSubject() {
    return authMethod == AuthenticationMethod.ANONYMOUS ? authIdentifier : externalSubject;
  }
}
