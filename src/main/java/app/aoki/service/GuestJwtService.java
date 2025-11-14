package app.aoki.service;

import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Service for generating JWT tokens for guest users.
 *
 * <p>This service only handles guest user authentication. For real user authentication, the system
 * should delegate to external authentication providers (e.g., OAuth2/OIDC providers).
 */
@ApplicationScoped
public class GuestJwtService {

  @ConfigProperty(name = "smallrye.jwt.new-token.issuer")
  String issuer;

  @ConfigProperty(name = "smallrye.jwt.new-token.lifespan")
  Long tokenLifespan;

  /**
   * Generate a JWT token for a guest user.
   *
   * @param userId the guest user ID
   * @return the JWT token
   */
  public String generateGuestToken(Long userId) {
    return Jwt.issuer(issuer)
        .upn("guest_" + userId) // User principal name
        .subject(userId.toString()) // User ID as subject
        .groups("guest") // Guest role
        .expiresIn(tokenLifespan)
        .sign();
  }
}
