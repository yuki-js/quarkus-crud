package app.aoki.quarkuscrud.entity;

import io.quarkus.runtime.annotations.RegisterForReflection;
import java.time.LocalDateTime;

/**
 * User profile revision entity.
 *
 * <p>Each record represents an immutable profile revision. The latest revision for a user is their
 * current profile. Revisions are accumulated over time.
 */
@RegisterForReflection
public class UserProfile {
  private Long id;
  private Long userId;
  private String profileData;
  private String revisionMeta;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  public UserProfile() {}

  public UserProfile(
      Long id,
      Long userId,
      String profileData,
      String revisionMeta,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {
    this.id = id;
    this.userId = userId;
    this.profileData = profileData;
    this.revisionMeta = revisionMeta;
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

  public String getProfileData() {
    return profileData;
  }

  public void setProfileData(String profileData) {
    this.profileData = profileData;
  }

  public String getRevisionMeta() {
    return revisionMeta;
  }

  public void setRevisionMeta(String revisionMeta) {
    this.revisionMeta = revisionMeta;
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
