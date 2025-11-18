package app.aoki.quarkuscrud.service;

import app.aoki.quarkuscrud.entity.AuthenticationProvider;
import app.aoki.quarkuscrud.entity.User;
import app.aoki.quarkuscrud.mapper.UserMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing users across all authentication providers.
 *
 * <p>This service handles user lifecycle operations for users authenticated through different
 * providers (anonymous, external OIDC, etc.). All users are treated equally with provider-specific
 * logic abstracted.
 */
@ApplicationScoped
public class UserService {

  @Inject UserMapper userMapper;

  /**
   * Creates a new user with anonymous authentication.
   *
   * <p>Generates a unique authentication identifier (UUID) for the user. This identifier serves as
   * both the internal reference and the JWT subject for anonymous users.
   *
   * @return the created user
   */
  @Transactional
  public User createAnonymousUser() {
    User user = new User();
    user.setAuthIdentifier(UUID.randomUUID().toString());
    user.setAuthProvider(AuthenticationProvider.ANONYMOUS);
    user.setExternalSubject(null); // Anonymous users don't have external subjects
    user.setCreatedAt(LocalDateTime.now());
    user.setUpdatedAt(LocalDateTime.now());
    userMapper.insert(user);
    return user;
  }

  /**
   * Creates or retrieves a user from an external authentication provider.
   *
   * <p>If a user with the given provider and external subject already exists, returns that user.
   * Otherwise, creates a new user. This enables seamless integration with external OIDC providers.
   *
   * @param provider the authentication provider
   * @param externalSubject the subject from the external provider
   * @return the user (existing or newly created)
   */
  @Transactional
  public User getOrCreateExternalUser(AuthenticationProvider provider, String externalSubject) {
    if (provider == AuthenticationProvider.ANONYMOUS) {
      throw new IllegalArgumentException("Use createAnonymousUser() for anonymous authentication");
    }

    // Check if user already exists
    Optional<User> existingUser =
        userMapper.findByProviderAndExternalSubject(provider, externalSubject);
    if (existingUser.isPresent()) {
      return existingUser.get();
    }

    // Create new user for external provider
    User user = new User();
    user.setAuthIdentifier(
        UUID.randomUUID().toString()); // Internal reference even for external users
    user.setAuthProvider(provider);
    user.setExternalSubject(externalSubject);
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
   * Finds a user by their internal authentication identifier.
   *
   * <p>The authentication identifier is an internal UUID used for all users regardless of provider.
   *
   * @param authIdentifier the authentication identifier
   * @return an Optional containing the user if found
   */
  public Optional<User> findByAuthIdentifier(String authIdentifier) {
    return userMapper.findByAuthIdentifier(authIdentifier);
  }

  /**
   * Finds a user by their external provider and subject.
   *
   * <p>Used to look up users authenticated via external providers (e.g., OIDC).
   *
   * @param provider the authentication provider
   * @param externalSubject the external subject identifier
   * @return an Optional containing the user if found
   */
  public Optional<User> findByProviderAndExternalSubject(
      AuthenticationProvider provider, String externalSubject) {
    return userMapper.findByProviderAndExternalSubject(provider, externalSubject);
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
