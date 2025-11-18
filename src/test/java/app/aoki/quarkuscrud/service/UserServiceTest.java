package app.aoki.quarkuscrud.service;

import static org.junit.jupiter.api.Assertions.*;

import app.aoki.quarkuscrud.entity.AuthenticationProvider;
import app.aoki.quarkuscrud.entity.User;
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
    assertNotNull(user.getAuthIdentifier());
    assertEquals(AuthenticationProvider.ANONYMOUS, user.getAuthProvider());
    assertNull(user.getExternalSubject());
    assertNotNull(user.getCreatedAt());
    assertNotNull(user.getUpdatedAt());
  }

  @Test
  @Transactional
  public void testGetOrCreateExternalUserNewUser() {
    String externalSubject = "google-user-123";
    User user = userService.getOrCreateExternalUser(AuthenticationProvider.OIDC, externalSubject);

    assertNotNull(user.getId());
    assertNotNull(user.getAuthIdentifier()); // Internal reference still generated
    assertEquals(AuthenticationProvider.OIDC, user.getAuthProvider());
    assertEquals(externalSubject, user.getExternalSubject());
    assertNotNull(user.getCreatedAt());
    assertNotNull(user.getUpdatedAt());
  }

  @Test
  @Transactional
  public void testGetOrCreateExternalUserExistingUser() {
    String externalSubject = "google-user-456";

    // Create user first time
    User user1 = userService.getOrCreateExternalUser(AuthenticationProvider.OIDC, externalSubject);
    Long userId1 = user1.getId();

    // Try to create same user again
    User user2 = userService.getOrCreateExternalUser(AuthenticationProvider.OIDC, externalSubject);
    Long userId2 = user2.getId();

    // Should return the same user
    assertEquals(userId1, userId2);
    assertEquals(externalSubject, user2.getExternalSubject());
  }

  @Test
  @Transactional
  public void testGetOrCreateExternalUserThrowsForAnonymous() {
    assertThrows(
        IllegalArgumentException.class,
        () -> userService.getOrCreateExternalUser(AuthenticationProvider.ANONYMOUS, "some-id"));
  }

  @Test
  @Transactional
  public void testFindByProviderAndExternalSubject() {
    String externalSubject = "github-user-789";
    User createdUser =
        userService.getOrCreateExternalUser(AuthenticationProvider.OIDC, externalSubject);

    Optional<User> foundUser =
        userService.findByProviderAndExternalSubject(AuthenticationProvider.OIDC, externalSubject);

    assertTrue(foundUser.isPresent());
    assertEquals(createdUser.getId(), foundUser.get().getId());
    assertEquals(externalSubject, foundUser.get().getExternalSubject());
  }

  @Test
  @Transactional
  public void testFindByProviderAndExternalSubject_NotFound() {
    Optional<User> foundUser =
        userService.findByProviderAndExternalSubject(
            AuthenticationProvider.OIDC, "non-existent-user");

    assertFalse(foundUser.isPresent());
  }

  @Test
  @Transactional
  public void testUserEffectiveSubjectAnonymous() {
    User user = userService.createAnonymousUser();
    assertEquals(user.getAuthIdentifier(), user.getEffectiveSubject());
  }

  @Test
  @Transactional
  public void testUserEffectiveSubjectExternal() {
    String externalSubject = "oidc-user-999";
    User user = userService.getOrCreateExternalUser(AuthenticationProvider.OIDC, externalSubject);
    assertEquals(externalSubject, user.getEffectiveSubject());
  }

  @Test
  @Transactional
  public void testAnonymousAndExternalUsersAreSeparate() {
    // Create anonymous user
    User anonymousUser = userService.createAnonymousUser();

    // Create external user with different subject
    User externalUser =
        userService.getOrCreateExternalUser(AuthenticationProvider.OIDC, "external-123");

    // They should be different users
    assertNotEquals(anonymousUser.getId(), externalUser.getId());
    assertNotEquals(anonymousUser.getAuthProvider(), externalUser.getAuthProvider());
  }
}
