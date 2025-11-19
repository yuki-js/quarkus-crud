package app.aoki.quarkuscrud.service;

import static org.junit.jupiter.api.Assertions.*;

import app.aoki.quarkuscrud.entity.AuthenticationMethod;
import app.aoki.quarkuscrud.entity.AuthnProvider;
import app.aoki.quarkuscrud.entity.User;
import app.aoki.quarkuscrud.entity.UserLifecycleStatus;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** Unit tests for UserService focusing on multi-provider authentication support. */
@QuarkusTest
public class UserServiceTest {

  @Inject UserService userService;

  @Test
  @Transactional
  public void testCreateAnonymousUser() {
    User user = userService.createAnonymousUser();

    assertNotNull(user.getId());
    assertEquals(UserLifecycleStatus.CREATED, user.getLifecycleStatus());
    assertNotNull(user.getCreatedAt());
    assertNotNull(user.getUpdatedAt());

    // Check authentication provider
    Optional<AuthnProvider> authnProvider = userService.getPrimaryAuthnProvider(user.getId());
    assertTrue(authnProvider.isPresent());
    assertEquals(AuthenticationMethod.ANONYMOUS, authnProvider.get().getAuthMethod());
    assertNotNull(authnProvider.get().getAuthIdentifier());
    assertNull(authnProvider.get().getExternalSubject());
  }

  @Test
  @Transactional
  public void testGetOrCreateExternalUserNewUser() {
    String externalSubject = "google-user-123";
    User user = userService.getOrCreateExternalUser(AuthenticationMethod.OIDC, externalSubject);

    assertNotNull(user.getId());
    assertEquals(UserLifecycleStatus.CREATED, user.getLifecycleStatus());
    assertNotNull(user.getCreatedAt());
    assertNotNull(user.getUpdatedAt());

    // Check authentication provider
    Optional<AuthnProvider> authnProvider = userService.getPrimaryAuthnProvider(user.getId());
    assertTrue(authnProvider.isPresent());
    assertEquals(AuthenticationMethod.OIDC, authnProvider.get().getAuthMethod());
    assertEquals(externalSubject, authnProvider.get().getExternalSubject());
    assertNotNull(authnProvider.get().getAuthIdentifier()); // Internal reference still generated
  }

  @Test
  @Transactional
  public void testGetOrCreateExternalUserExistingUser() {
    String externalSubject = "google-user-456";

    // Create user first time
    User user1 = userService.getOrCreateExternalUser(AuthenticationMethod.OIDC, externalSubject);
    Long userId1 = user1.getId();

    // Try to create same user again
    User user2 = userService.getOrCreateExternalUser(AuthenticationMethod.OIDC, externalSubject);
    Long userId2 = user2.getId();

    // Should return the same user
    assertEquals(userId1, userId2);

    Optional<AuthnProvider> authnProvider = userService.getPrimaryAuthnProvider(userId2);
    assertTrue(authnProvider.isPresent());
    assertEquals(externalSubject, authnProvider.get().getExternalSubject());
  }

  @Test
  @Transactional
  public void testGetOrCreateExternalUserThrowsForAnonymous() {
    assertThrows(
        IllegalArgumentException.class,
        () -> userService.getOrCreateExternalUser(AuthenticationMethod.ANONYMOUS, "some-id"));
  }

  @Test
  @Transactional
  public void testFindByMethodAndExternalSubject() {
    String externalSubject = "github-user-789";
    User createdUser =
        userService.getOrCreateExternalUser(AuthenticationMethod.OIDC, externalSubject);

    Optional<User> foundUser =
        userService.findByMethodAndExternalSubject(AuthenticationMethod.OIDC, externalSubject);

    assertTrue(foundUser.isPresent());
    assertEquals(createdUser.getId(), foundUser.get().getId());

    Optional<AuthnProvider> authnProvider =
        userService.getPrimaryAuthnProvider(foundUser.get().getId());
    assertTrue(authnProvider.isPresent());
    assertEquals(externalSubject, authnProvider.get().getExternalSubject());
  }

  @Test
  @Transactional
  public void testFindByMethodAndExternalSubjectNotFound() {
    Optional<User> foundUser =
        userService.findByMethodAndExternalSubject(AuthenticationMethod.OIDC, "non-existent-user");

    assertFalse(foundUser.isPresent());
  }

  @Test
  @Transactional
  public void testAuthnProviderEffectiveSubjectAnonymous() {
    User user = userService.createAnonymousUser();
    Optional<AuthnProvider> authnProvider = userService.getPrimaryAuthnProvider(user.getId());
    assertTrue(authnProvider.isPresent());
    assertEquals(
        authnProvider.get().getAuthIdentifier(), authnProvider.get().getEffectiveSubject());
  }

  @Test
  @Transactional
  public void testAuthnProviderEffectiveSubjectExternal() {
    String externalSubject = "oidc-user-999";
    User user = userService.getOrCreateExternalUser(AuthenticationMethod.OIDC, externalSubject);
    Optional<AuthnProvider> authnProvider = userService.getPrimaryAuthnProvider(user.getId());
    assertTrue(authnProvider.isPresent());
    assertEquals(externalSubject, authnProvider.get().getEffectiveSubject());
  }

  @Test
  @Transactional
  public void testAnonymousAndExternalUsersAreSeparate() {
    // Create anonymous user
    User anonymousUser = userService.createAnonymousUser();

    // Create external user with different subject
    User externalUser =
        userService.getOrCreateExternalUser(AuthenticationMethod.OIDC, "external-123");

    // They should be different users
    assertNotEquals(anonymousUser.getId(), externalUser.getId());

    Optional<AuthnProvider> anonymousAuthn =
        userService.getPrimaryAuthnProvider(anonymousUser.getId());
    Optional<AuthnProvider> externalAuthn =
        userService.getPrimaryAuthnProvider(externalUser.getId());

    assertTrue(anonymousAuthn.isPresent());
    assertTrue(externalAuthn.isPresent());
    assertNotEquals(anonymousAuthn.get().getAuthMethod(), externalAuthn.get().getAuthMethod());
  }
}
