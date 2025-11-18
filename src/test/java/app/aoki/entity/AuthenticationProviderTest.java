package app.aoki.entity;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/** Unit tests for AuthenticationProvider enum. */
public class AuthenticationProviderTest {

  @Test
  public void testAnonymousProvider() {
    assertEquals("anonymous", AuthenticationProvider.ANONYMOUS.getValue());
  }

  @Test
  public void testOidcProvider() {
    assertEquals("oidc", AuthenticationProvider.OIDC.getValue());
  }

  @Test
  public void testFromValueAnonymous() {
    AuthenticationProvider provider = AuthenticationProvider.fromValue("anonymous");
    assertEquals(AuthenticationProvider.ANONYMOUS, provider);
  }

  @Test
  public void testFromValueOidc() {
    AuthenticationProvider provider = AuthenticationProvider.fromValue("oidc");
    assertEquals(AuthenticationProvider.OIDC, provider);
  }

  @Test
  public void testFromValueInvalid() {
    assertThrows(IllegalArgumentException.class, () -> AuthenticationProvider.fromValue("invalid"));
  }

  @Test
  public void testFromValueNull() {
    assertThrows(IllegalArgumentException.class, () -> AuthenticationProvider.fromValue(null));
  }
}
