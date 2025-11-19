package app.aoki.quarkuscrud.entity;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * Authentication provider types supported by the system.
 *
 * <p>This enum defines the different authentication methods available. Each provider type may have
 * different authentication flows and user management strategies.
 */
@RegisterForReflection
public enum AuthenticationProvider {
  /**
   * Anonymous authentication - users created locally without credentials.
   *
   * <p>Used for guest/anonymous access. User identity is managed internally via UUID-based
   * auth_identifier.
   */
  ANONYMOUS("anonymous"),

  /**
   * External OpenID Connect authentication provider.
   *
   * <p>Users authenticated via external OIDC provider (e.g., Google, Auth0, Keycloak). User
   * identity comes from the external provider's subject claim.
   */
  OIDC("oidc");

  private final String value;

  AuthenticationProvider(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  /**
   * Get AuthenticationProvider from string value.
   *
   * @param value the string value
   * @return the corresponding AuthenticationProvider
   * @throws IllegalArgumentException if value doesn't match any provider
   */
  public static AuthenticationProvider fromValue(String value) {
    for (AuthenticationProvider provider : values()) {
      if (provider.value.equals(value)) {
        return provider;
      }
    }
    throw new IllegalArgumentException("Unknown authentication provider: " + value);
  }
}
