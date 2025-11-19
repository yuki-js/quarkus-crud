package app.aoki.quarkuscrud.service;

import app.aoki.quarkuscrud.entity.AuthenticationMethod;
import app.aoki.quarkuscrud.entity.User;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Optional;
import org.eclipse.microprofile.jwt.JsonWebToken;

/**
 * Service for handling authentication logic across multiple providers.
 *
 * <p>This service centralizes authentication operations, separating business logic from the filter
 * layer. It handles JWT token validation and user lookup for all authentication methods.
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
   *   <li>For external OIDC users: looks up by method and externalSubject (JWT subject)
   * </ul>
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

    // Determine authentication method from JWT groups
    AuthenticationMethod method = determineMethodFromJwt(jwt);

    switch (method) {
      case ANONYMOUS:
        // For anonymous users, subject is the authIdentifier
        return userService.findByAuthIdentifier(subject);

      case OIDC:
        // For OIDC users, subject is the externalSubject from the provider
        return userService.findByMethodAndExternalSubject(method, subject);

      default:
        return Optional.empty();
    }
  }

  /**
   * Determines the authentication method from JWT claims.
   *
   * <p>Uses the groups claim to identify the method.
   *
   * @param jwt the JWT token
   * @return the authentication method
   */
  private AuthenticationMethod determineMethodFromJwt(JsonWebToken jwt) {
    // Check groups claim first
    if (jwt.getGroups() != null) {
      if (jwt.getGroups().contains("anonymous")) {
        return AuthenticationMethod.ANONYMOUS;
      }
      if (jwt.getGroups().contains("oidc")) {
        return AuthenticationMethod.OIDC;
      }
    }

    // Default to anonymous for backward compatibility
    return AuthenticationMethod.ANONYMOUS;
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
   * @param jwt the validated external OIDC JWT token
   * @return the authenticated user
   */
  public Optional<User> authenticateFromExternalOidc(JsonWebToken jwt) {
    String externalSubject = jwt.getSubject();
    User user = userService.getOrCreateExternalUser(AuthenticationMethod.OIDC, externalSubject);
    return Optional.of(user);
  }
}
