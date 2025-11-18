package app.aoki.service;

import app.aoki.entity.AuthenticationProvider;
import app.aoki.entity.User;
import io.smallrye.jwt.algorithm.SignatureAlgorithm;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Service for generating JWT tokens for internally-authenticated users.
 *
 * <p>This service handles JWT token generation for users authenticated by this service (currently
 * anonymous users). When users are authenticated by external providers (OIDC), this service is NOT
 * used - those tokens come directly from the external provider.
 *
 * <p>This design ensures minimal code changes when integrating external authentication providers.
 */
@ApplicationScoped
public class JwtService {

  @ConfigProperty(name = "smallrye.jwt.new-token.issuer")
  String issuer;

  @ConfigProperty(name = "smallrye.jwt.new-token.lifespan")
  Long tokenLifespan;

  /**
   * Generates a JWT token for a user with internal authentication.
   *
   * <p>The token structure uses the user's effective subject (which depends on authentication
   * provider):
   *
   * <ul>
   *   <li>Subject (sub): User's effective subject (authIdentifier for anonymous, externalSubject
   *       for others)
   *   <li>User Principal Name (upn): Same as subject for consistency
   *   <li>Groups: Authentication provider value (e.g., "anonymous")
   *   <li>Expiration: Configurable lifespan
   * </ul>
   *
   * @param user the user to generate a token for
   * @return the signed JWT token
   */
  public String generateToken(User user) {
    String subject = user.getEffectiveSubject();
    String group = user.getAuthProvider().getValue();

    return Jwt.issuer(issuer)
        .upn(subject) // User principal name - effective subject
        .subject(subject) // Subject - effective subject
        .groups(group) // Authentication provider as group
        .expiresIn(tokenLifespan)
        .jws()
        .algorithm(SignatureAlgorithm.ES256) // ECDSA with SHA-256
        .sign();
  }

  /**
   * Generates a JWT token for an anonymous user.
   *
   * <p>Convenience method for anonymous authentication. Simply calls {@link #generateToken(User)}.
   *
   * @param user the anonymous user
   * @return the signed JWT token
   */
  public String generateAnonymousToken(User user) {
    if (user.getAuthProvider() != AuthenticationProvider.ANONYMOUS) {
      throw new IllegalArgumentException("User is not authenticated anonymously");
    }
    return generateToken(user);
  }
}
