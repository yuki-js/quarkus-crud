package app.aoki.service;

import app.aoki.entity.User;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Optional;
import org.eclipse.microprofile.jwt.JsonWebToken;

/**
 * Service for handling authentication logic.
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
   * <p>Extracts the user's authentication identifier from the JWT subject and looks up the
   * corresponding user. Works for all authentication methods (anonymous, external providers, etc.).
   *
   * @param jwt the validated JWT token
   * @return the authenticated user, or empty if user not found
   */
  public Optional<User> authenticateFromJwt(JsonWebToken jwt) {
    if (jwt == null || jwt.getSubject() == null) {
      return Optional.empty();
    }

    String authIdentifier = jwt.getSubject();
    if (authIdentifier == null || authIdentifier.isEmpty()) {
      return Optional.empty();
    }

    // Look up user by their authentication identifier
    return userService.findByAuthIdentifier(authIdentifier);
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
}
