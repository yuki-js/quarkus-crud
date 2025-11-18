package app.aoki.service;

import io.smallrye.jwt.algorithm.SignatureAlgorithm;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Service for generating and managing JWT tokens.
 *
 * <p>This service handles JWT token generation for all users regardless of authentication method.
 * Tokens can be issued for anonymous authentication or as part of external authentication provider
 * integration.
 *
 * <p>Uses UUID as subject and UPN for consistent identity handling across all authentication types.
 */
@ApplicationScoped
public class JwtService {

  @ConfigProperty(name = "smallrye.jwt.new-token.issuer")
  String issuer;

  @ConfigProperty(name = "smallrye.jwt.new-token.lifespan")
  Long tokenLifespan;

  /**
   * Generates a JWT token for a user.
   *
   * <p>The token includes:
   *
   * <ul>
   *   <li>Subject (sub): User's authentication identifier (UUID)
   *   <li>User Principal Name (upn): Same as subject for consistency
   *   <li>Groups: Authentication method indicator ("anonymous" for anonymous auth)
   *   <li>Expiration: Configurable lifespan
   * </ul>
   *
   * @param authIdentifier the user's authentication identifier (UUID)
   * @param authenticationMethod the authentication method (e.g., "anonymous", "oauth")
   * @return the signed JWT token
   */
  public String generateToken(String authIdentifier, String authenticationMethod) {
    return Jwt.issuer(issuer)
        .upn(authIdentifier) // User principal name - plain UUID for consistency
        .subject(authIdentifier) // UUID as subject (same format for all users)
        .groups(authenticationMethod) // Authentication method as group
        .expiresIn(tokenLifespan)
        .jws()
        .algorithm(SignatureAlgorithm.ES256) // ECDSA with SHA-256
        .sign();
  }

  /**
   * Generates a JWT token for anonymous authentication.
   *
   * <p>Convenience method that calls {@link #generateToken(String, String)} with "anonymous" as the
   * authentication method.
   *
   * @param authIdentifier the user's authentication identifier (UUID)
   * @return the signed JWT token
   */
  public String generateAnonymousToken(String authIdentifier) {
    return generateToken(authIdentifier, "anonymous");
  }
}
