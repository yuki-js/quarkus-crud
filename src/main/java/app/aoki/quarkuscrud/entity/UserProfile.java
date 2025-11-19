package app.aoki.quarkuscrud.entity;

import io.quarkus.runtime.annotations.RegisterForReflection;
import java.time.LocalDateTime;

/**
 * User profile entity.
 *
 * <p>Stores user profile card information. Each record is immutable (except revision_meta) and
 * represents a snapshot of the user's profile at a point in time.
 */
@RegisterForReflection
public class UserProfile {
  private Long id;
  private Long userId;
  private String profileData; // JSONB stored as String
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
  private String revisionMeta; // JSONB stored as String

  public UserProfile() {}

  public UserProfile(
      Long id,
      Long userId,
      String profileData,
      LocalDateTime createdAt,
      LocalDateTime updatedAt,
      String revisionMeta) {
    this.id = id;
    this.userId = userId;
    this.profileData = profileData;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
    this.revisionMeta = revisionMeta;
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

  public String getRevisionMeta() {
    return revisionMeta;
  }

  public void setRevisionMeta(String revisionMeta) {
    this.revisionMeta = revisionMeta;
  }
}
