package app.aoki.quarkuscrud.entity;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/** Unit tests for AuthenticationMethod enum. */
public class AuthenticationProviderTest {

  @Test
  public void testAnonymousMethod() {
    assertEquals("anonymous", AuthenticationMethod.ANONYMOUS.getValue());
  }

  @Test
  public void testOidcMethod() {
    assertEquals("oidc", AuthenticationMethod.OIDC.getValue());
  }

  @Test
  public void testFromValueAnonymous() {
    AuthenticationMethod method = AuthenticationMethod.fromValue("anonymous");
    assertEquals(AuthenticationMethod.ANONYMOUS, method);
  }

  @Test
  public void testFromValueOidc() {
    AuthenticationMethod method = AuthenticationMethod.fromValue("oidc");
    assertEquals(AuthenticationMethod.OIDC, method);
  }

  @Test
  public void testFromValueInvalid() {
    assertThrows(IllegalArgumentException.class, () -> AuthenticationMethod.fromValue("invalid"));
  }

  @Test
  public void testFromValueNull() {
    assertThrows(IllegalArgumentException.class, () -> AuthenticationMethod.fromValue(null));
  }
}
