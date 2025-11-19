package app.aoki.quarkuscrud.service;

import app.aoki.quarkuscrud.entity.User;
import app.aoki.quarkuscrud.entity.UserProfile;
import app.aoki.quarkuscrud.mapper.UserMapper;
import app.aoki.quarkuscrud.mapper.UserProfileMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Service for managing user profiles.
 *
 * <p>Handles profile creation, updates, and retrieval. Profiles are versioned - each update creates
 * a new revision.
 */
@ApplicationScoped
public class ProfileService {

  @Inject UserProfileMapper profileMapper;
  @Inject UserMapper userMapper;

  /**
   * Get the current profile for a user.
   *
   * @param userId the user ID
   * @return the current profile, or empty if no profile exists
   */
  public Optional<UserProfile> getCurrentProfile(Long userId) {
    return userMapper.findById(userId).flatMap(user -> {
      if (user.getCurrentProfileRevision() == null) {
        return Optional.empty();
      }
      return profileMapper.findById(user.getCurrentProfileRevision());
    });
  }

  /**
   * Create or update a user's profile.
   *
   * <p>Creates a new profile revision and updates the user's current profile reference.
   *
   * @param userId the user ID
   * @param profileData the profile data as JSON string
   * @return the created profile revision
   */
  @Transactional
  public UserProfile createOrUpdateProfile(Long userId, String profileData) {
    // Create new profile revision
    UserProfile profile = new UserProfile();
    profile.setUserId(userId);
    profile.setProfileData(profileData);
    profile.setRevisionMeta(null);
    profile.setCreatedAt(LocalDateTime.now());
    profile.setUpdatedAt(LocalDateTime.now());
    profileMapper.insert(profile);

    // Update user's current profile reference
    Optional<User> userOpt = userMapper.findById(userId);
    if (userOpt.isPresent()) {
      User user = userOpt.get();
      user.setCurrentProfileRevision(profile.getId());
      user.setUpdatedAt(LocalDateTime.now());
      userMapper.update(user);
    }

    return profile;
  }

  /**
   * Find a profile by its revision ID.
   *
   * @param profileId the profile revision ID
   * @return the profile, or empty if not found
   */
  public Optional<UserProfile> findById(Long profileId) {
    return profileMapper.findById(profileId);
  }

  /**
   * Get all profile revisions for a user.
   *
   * @param userId the user ID
   * @return list of profile revisions, ordered by creation date descending
   */
  public java.util.List<UserProfile> getProfileHistory(Long userId) {
    return profileMapper.findByUserId(userId);
  }
}
