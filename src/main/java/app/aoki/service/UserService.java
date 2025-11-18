package app.aoki.service;

import app.aoki.entity.User;
import app.aoki.mapper.UserMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing users.
 *
 * <p>All users are treated equally regardless of authentication method. This service handles user
 * lifecycle operations including creation, retrieval, update, and deletion.
 */
@ApplicationScoped
public class UserService {

  @Inject UserMapper userMapper;

  /**
   * Creates a new user with anonymous authentication.
   *
   * <p>Generates a unique authentication identifier (UUID) for the user. This identifier is used
   * for JWT token generation and user lookup.
   *
   * @return the created user
   */
  @Transactional
  public User createUser() {
    User user = new User();
    user.setAuthIdentifier(UUID.randomUUID().toString());
    user.setCreatedAt(LocalDateTime.now());
    user.setUpdatedAt(LocalDateTime.now());
    userMapper.insert(user);
    return user;
  }

  /**
   * Finds a user by ID.
   *
   * @param id the user ID
   * @return an Optional containing the user if found
   */
  public Optional<User> findById(Long id) {
    return userMapper.findById(id);
  }

  /**
   * Finds a user by their authentication identifier.
   *
   * <p>The authentication identifier is a UUID used for both anonymous authentication and as a
   * reference for external authentication providers.
   *
   * @param authIdentifier the authentication identifier
   * @return an Optional containing the user if found
   */
  public Optional<User> findByAuthIdentifier(String authIdentifier) {
    return userMapper.findByAuthIdentifier(authIdentifier);
  }

  /**
   * Updates an existing user.
   *
   * @param user the user to update
   */
  @Transactional
  public void updateUser(User user) {
    user.setUpdatedAt(LocalDateTime.now());
    userMapper.update(user);
  }

  /**
   * Deletes a user by ID.
   *
   * <p>Note: All rooms owned by this user will be automatically deleted due to the ON DELETE
   * CASCADE constraint on the rooms.user_id foreign key.
   *
   * @param id the user ID
   */
  @Transactional
  public void deleteUser(Long id) {
    userMapper.deleteById(id);
  }
}
