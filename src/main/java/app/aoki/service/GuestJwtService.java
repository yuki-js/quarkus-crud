package app.aoki.service;

import io.smallrye.jwt.algorithm.SignatureAlgorithm;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Service for generating JWT tokens for guest users.
 *
 * <p>This service only handles guest user authentication. For real user authentication, the system
 * should delegate to external authentication providers (e.g., OAuth2/OIDC providers).
 *
 * <p>Uses UUID as subject for both guest users and external provider users to ensure consistent
 * identity handling across all authentication types.
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
   * @param userUuid the guest user's UUID (from guestToken field)
   * @return the JWT token
   */
  public String generateGuestToken(String userUuid) {
    return Jwt.issuer(issuer)
        .upn("guest_" + userUuid) // User principal name
        .subject(userUuid) // UUID as subject (same format for all users)
        .groups("guest") // Guest role
        .expiresIn(tokenLifespan)
        .jws()
        .algorithm(SignatureAlgorithm.ES256) // ECDSA with SHA-256
        .sign();
  }
}
