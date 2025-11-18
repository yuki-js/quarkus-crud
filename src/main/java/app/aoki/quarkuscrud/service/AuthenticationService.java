package app.aoki.quarkuscrud.service;

import app.aoki.quarkuscrud.entity.AuthenticationProvider;
import app.aoki.quarkuscrud.entity.User;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Optional;
import org.eclipse.microprofile.jwt.JsonWebToken;

/**
 * Service for handling authentication logic across multiple providers.
 *
 * <p>This service centralizes authentication operations, separating business logic from the filter
 * layer. It handles JWT token validation and user lookup for all authentication methods (anonymous,
 * external OIDC, etc.).
 *
 * <p>The service is designed to support multiple authentication providers with minimal code
 * changes. When integrating external OIDC, only provider-specific logic needs to be added.
 */
@ApplicationScoped
public class AuthenticationService {

  @Inject UserService userService;

  /**
   * Authenticates a user from a JWT token.
   *
   * <p>The authentication strategy depends on the JWT issuer and groups:
   *
   * <ul>
   *   <li>For anonymous users: looks up by authIdentifier (JWT subject)
   *   <li>For external OIDC users: looks up by provider and externalSubject (JWT subject)
   * </ul>
   *
   * <p>This method is designed to support future external authentication providers with minimal
   * changes.
   *
   * @param jwt the validated JWT token
   * @return the authenticated user, or empty if user not found
   */
  public Optional<User> authenticateFromJwt(JsonWebToken jwt) {
    if (jwt == null || jwt.getSubject() == null) {
      return Optional.empty();
    }

    String subject = jwt.getSubject();
    if (subject == null || subject.isEmpty()) {
      return Optional.empty();
    }

    // Determine authentication provider from JWT groups
    AuthenticationProvider provider = determineProviderFromJwt(jwt);

    switch (provider) {
      case ANONYMOUS:
        // For anonymous users, subject is the authIdentifier
        return userService.findByAuthIdentifier(subject);

      case OIDC:
        // For OIDC users, subject is the externalSubject from the provider
        // When implementing OIDC, this will look up or create the user
        return userService.findByProviderAndExternalSubject(provider, subject);

      default:
        return Optional.empty();
    }
  }

  /**
   * Determines the authentication provider from JWT claims.
   *
   * <p>Uses the groups claim to identify the provider. For locally-issued tokens, the group is set
   * explicitly. For external OIDC tokens, we can check the issuer claim or other provider-specific
   * claims.
   *
   * @param jwt the JWT token
   * @return the authentication provider
   */
  private AuthenticationProvider determineProviderFromJwt(JsonWebToken jwt) {
    // Check groups claim first
    if (jwt.getGroups() != null) {
      if (jwt.getGroups().contains("anonymous")) {
        return AuthenticationProvider.ANONYMOUS;
      }
      if (jwt.getGroups().contains("oidc")) {
        return AuthenticationProvider.OIDC;
      }
    }

    // For external OIDC providers, we would check the issuer claim here
    // String issuer = jwt.getIssuer();
    // if (issuer != null && issuer.startsWith("https://accounts.google.com")) {
    //   return AuthenticationProvider.OIDC;
    // }

    // Default to anonymous for backward compatibility
    return AuthenticationProvider.ANONYMOUS;
  }

  /**
   * Checks if a request has a valid Bearer token in the Authorization header.
   *
   * @param authHeader the Authorization header value
   * @return true if the header contains a Bearer token
   */
  public boolean hasBearerToken(String authHeader) {
    return authHeader != null && authHeader.startsWith("Bearer ");
  }

  /**
   * Authenticates or creates a user from an external OIDC token.
   *
   * <p>This method is designed for future OIDC integration. When an external OIDC token is
   * received, this method will:
   *
   * <ol>
   *   <li>Validate the token (signature, expiration, issuer)
   *   <li>Extract the subject and other claims
   *   <li>Look up or create the user in our database
   *   <li>Return the authenticated user
   * </ol>
   *
   * <p>Example integration:
   *
   * <pre>{@code
   * String externalSubject = jwt.getSubject();
   * User user = userService.getOrCreateExternalUser(
   *     AuthenticationProvider.OIDC,
   *     externalSubject
   * );
   * return Optional.of(user);
   * }</pre>
   *
   * @param jwt the validated external OIDC JWT token
   * @return the authenticated user
   */
  public Optional<User> authenticateFromExternalOidc(JsonWebToken jwt) {
    // TODO: Implement when integrating external OIDC provider
    // This is a placeholder showing how easy the integration will be
    String externalSubject = jwt.getSubject();
    User user = userService.getOrCreateExternalUser(AuthenticationProvider.OIDC, externalSubject);
    return Optional.of(user);
  }
}
