package app.aoki.quarkuscrud.entity;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * Authentication method types.
 *
 * <p>Defines the different authentication methods available in the system.
 */
@RegisterForReflection
public enum AuthenticationMethod {
  /**
   * Anonymous authentication.
   *
   * <p>Users created locally without credentials using UUID-based auth_identifier.
   */
  ANONYMOUS("anonymous"),

  /**
   * External OpenID Connect authentication.
   *
   * <p>Users authenticated via external OIDC provider.
   */
  OIDC("oidc");

  private final String value;

  AuthenticationMethod(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  /**
   * Get AuthenticationMethod from string value.
   *
   * @param value the string value
   * @return the corresponding AuthenticationMethod
   * @throws IllegalArgumentException if value doesn't match any method
   */
  public static AuthenticationMethod fromValue(String value) {
    for (AuthenticationMethod method : values()) {
      if (method.value.equals(value)) {
        return method;
      }
    }
    throw new IllegalArgumentException("Unknown authentication method: " + value);
  }
}
