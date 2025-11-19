package app.aoki.quarkuscrud.service;

import app.aoki.quarkuscrud.entity.AccountLifecycle;
import app.aoki.quarkuscrud.entity.AuthMethod;
import app.aoki.quarkuscrud.entity.AuthnProvider;
import app.aoki.quarkuscrud.entity.User;
import app.aoki.quarkuscrud.mapper.AuthnProviderMapper;
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
 * <p>This service handles user lifecycle operations. Authentication information is managed
 * separately in the AuthnProvider table.
 */
@ApplicationScoped
public class UserService {

  @Inject UserMapper userMapper;
  @Inject AuthnProviderMapper authnProviderMapper;

  /**
   * Creates a new user with anonymous authentication.
   *
   * <p>Generates a unique authentication identifier (UUID) for the user. Creates both the User
   * entity and the associated AuthnProvider record.
   *
   * @return the created user
   */
  @Transactional
  public User createAnonymousUser() {
    // Create user entity
    User user = new User();
    user.setAccountLifecycle(AccountLifecycle.CREATED);
    user.setCurrentProfileRevision(null);
    user.setMeta(null);
    user.setCreatedAt(LocalDateTime.now());
    user.setUpdatedAt(LocalDateTime.now());
    userMapper.insert(user);

    // Create authentication provider
    AuthnProvider authnProvider = new AuthnProvider();
    authnProvider.setUserId(user.getId());
    authnProvider.setAuthMethod(AuthMethod.ANONYMOUS);
    authnProvider.setAuthIdentifier(UUID.randomUUID().toString());
    authnProvider.setExternalSubject(null);
    authnProvider.setCreatedAt(LocalDateTime.now());
    authnProvider.setUpdatedAt(LocalDateTime.now());
    authnProviderMapper.insert(authnProvider);

    return user;
  }

  /**
   * Creates or retrieves a user from an external authentication provider.
   *
   * <p>If a user with the given provider and external subject already exists, returns that user.
   * Otherwise, creates a new user.
   *
   * @param authMethod the authentication method
   * @param externalSubject the subject from the external provider
   * @return the user (existing or newly created)
   */
  @Transactional
  public User getOrCreateExternalUser(AuthMethod authMethod, String externalSubject) {
    if (authMethod == AuthMethod.ANONYMOUS) {
      throw new IllegalArgumentException("Use createAnonymousUser() for anonymous authentication");
    }

    // Check if authentication provider already exists
    Optional<AuthnProvider> existingAuthnProvider =
        authnProviderMapper.findByMethodAndExternalSubject(authMethod, externalSubject);
    if (existingAuthnProvider.isPresent()) {
      // Return the associated user
      return userMapper.findById(existingAuthnProvider.get().getUserId()).orElseThrow();
    }

    // Create new user
    User user = new User();
    user.setAccountLifecycle(AccountLifecycle.CREATED);
    user.setCurrentProfileRevision(null);
    user.setMeta(null);
    user.setCreatedAt(LocalDateTime.now());
    user.setUpdatedAt(LocalDateTime.now());
    userMapper.insert(user);

    // Create authentication provider
    AuthnProvider authnProvider = new AuthnProvider();
    authnProvider.setUserId(user.getId());
    authnProvider.setAuthMethod(authMethod);
    authnProvider.setAuthIdentifier(
        UUID.randomUUID().toString()); // Internal reference for tracking
    authnProvider.setExternalSubject(externalSubject);
    authnProvider.setCreatedAt(LocalDateTime.now());
    authnProvider.setUpdatedAt(LocalDateTime.now());
    authnProviderMapper.insert(authnProvider);

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
   * @param authIdentifier the authentication identifier
   * @return an Optional containing the user if found
   */
  public Optional<User> findByAuthIdentifier(String authIdentifier) {
    Optional<AuthnProvider> authnProvider =
        authnProviderMapper.findByAuthIdentifier(authIdentifier);
    if (authnProvider.isPresent()) {
      return userMapper.findById(authnProvider.get().getUserId());
    }
    return Optional.empty();
  }

  /**
   * Finds a user by their external provider and subject.
   *
   * @param authMethod the authentication method
   * @param externalSubject the external subject identifier
   * @return an Optional containing the user if found
   */
  public Optional<User> findByMethodAndExternalSubject(
      AuthMethod authMethod, String externalSubject) {
    Optional<AuthnProvider> authnProvider =
        authnProviderMapper.findByMethodAndExternalSubject(authMethod, externalSubject);
    if (authnProvider.isPresent()) {
      return userMapper.findById(authnProvider.get().getUserId());
    }
    return Optional.empty();
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
   * <p>Note: All related entities (authentication providers, profiles, etc.) will be automatically
   * deleted due to ON DELETE CASCADE constraints.
   *
   * @param id the user ID
   */
  @Transactional
  public void deleteUser(Long id) {
    userMapper.deleteById(id);
  }
}
