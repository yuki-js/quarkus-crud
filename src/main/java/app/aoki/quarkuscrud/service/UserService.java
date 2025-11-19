package app.aoki.quarkuscrud.service;

import app.aoki.quarkuscrud.entity.AuthenticationMethod;
import app.aoki.quarkuscrud.entity.AuthnProvider;
import app.aoki.quarkuscrud.entity.User;
import app.aoki.quarkuscrud.entity.UserLifecycleStatus;
import app.aoki.quarkuscrud.mapper.AuthnProviderMapper;
import app.aoki.quarkuscrud.mapper.UserMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing users and their authentication providers.
 *
 * <p>This service handles user lifecycle operations including creation, retrieval, and updates.
 * User authentication details are managed separately in the AuthnProvider entity.
 */
@ApplicationScoped
public class UserService {

  @Inject UserMapper userMapper;
  @Inject AuthnProviderMapper authnProviderMapper;

  /**
   * Creates a new user with anonymous authentication.
   *
   * <p>Creates both a User entity and an associated AuthnProvider entity with anonymous
   * authentication method.
   *
   * @return the created user
   */
  @Transactional
  public User createAnonymousUser() {
    // Create user entity
    User user = new User();
    user.setCreatedAt(LocalDateTime.now());
    user.setUpdatedAt(LocalDateTime.now());
    user.setLifecycleStatus(UserLifecycleStatus.CREATED);
    user.setCurrentProfileCardRevision(null);
    user.setMeta("{}");
    userMapper.insert(user);

    // Create authentication provider for anonymous auth
    AuthnProvider authnProvider = new AuthnProvider();
    authnProvider.setUserId(user.getId());
    authnProvider.setAuthMethod(AuthenticationMethod.ANONYMOUS);
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
   * Otherwise, creates a new user and associated AuthnProvider entity.
   *
   * @param method the authentication method
   * @param externalSubject the subject from the external provider
   * @return the user (existing or newly created)
   */
  @Transactional
  public User getOrCreateExternalUser(AuthenticationMethod method, String externalSubject) {
    if (method == AuthenticationMethod.ANONYMOUS) {
      throw new IllegalArgumentException("Use createAnonymousUser() for anonymous authentication");
    }

    // Check if authentication provider already exists
    Optional<AuthnProvider> existingAuthnProvider =
        authnProviderMapper.findByMethodAndExternalSubject(method, externalSubject);
    if (existingAuthnProvider.isPresent()) {
      return userMapper.findById(existingAuthnProvider.get().getUserId()).orElseThrow();
    }

    // Create new user
    User user = new User();
    user.setCreatedAt(LocalDateTime.now());
    user.setUpdatedAt(LocalDateTime.now());
    user.setLifecycleStatus(UserLifecycleStatus.CREATED);
    user.setCurrentProfileCardRevision(null);
    user.setMeta("{}");
    userMapper.insert(user);

    // Create authentication provider
    AuthnProvider authnProvider = new AuthnProvider();
    authnProvider.setUserId(user.getId());
    authnProvider.setAuthMethod(method);
    authnProvider.setAuthIdentifier(UUID.randomUUID().toString());
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
    if (authnProvider.isEmpty()) {
      return Optional.empty();
    }
    return userMapper.findById(authnProvider.get().getUserId());
  }

  /**
   * Finds a user by their external provider and subject.
   *
   * @param method the authentication method
   * @param externalSubject the external subject identifier
   * @return an Optional containing the user if found
   */
  public Optional<User> findByMethodAndExternalSubject(
      AuthenticationMethod method, String externalSubject) {
    Optional<AuthnProvider> authnProvider =
        authnProviderMapper.findByMethodAndExternalSubject(method, externalSubject);
    if (authnProvider.isEmpty()) {
      return Optional.empty();
    }
    return userMapper.findById(authnProvider.get().getUserId());
  }

  /**
   * Gets the primary authentication provider for a user.
   *
   * @param userId the user ID
   * @return the primary authentication provider
   */
  public Optional<AuthnProvider> getPrimaryAuthnProvider(Long userId) {
    var providers = authnProviderMapper.findByUserId(userId);
    return providers.isEmpty() ? Optional.empty() : Optional.of(providers.get(0));
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
}
