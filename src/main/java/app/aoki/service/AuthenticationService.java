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
 * layer. It handles JWT token validation and user lookup.
 */
@ApplicationScoped
public class AuthenticationService {

  @Inject UserService userService;

  /**
   * Authenticate a user from a JWT token.
   *
   * @param jwt the validated JWT token
   * @return the authenticated user, or empty if user not found
   */
  public Optional<User> authenticateFromJwt(JsonWebToken jwt) {
    if (jwt == null || jwt.getSubject() == null) {
      return Optional.empty();
    }

    String userUuid = jwt.getSubject();
    if (userUuid == null || userUuid.isEmpty()) {
      return Optional.empty();
    }

    // Look up user by UUID (stored in guestToken field for guest users)
    return userService.findByGuestToken(userUuid);
  }

  /**
   * Check if a request has a valid Bearer token in the Authorization header.
   *
   * @param authHeader the Authorization header value
   * @return true if the header contains a Bearer token
   */
  public boolean hasBearerToken(String authHeader) {
    return authHeader != null && authHeader.startsWith("Bearer ");
  }
}
