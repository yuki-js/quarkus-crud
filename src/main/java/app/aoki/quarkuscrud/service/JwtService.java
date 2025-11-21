package app.aoki.quarkuscrud.service;

import app.aoki.quarkuscrud.entity.AuthMethod;
import app.aoki.quarkuscrud.entity.AuthnProvider;
import app.aoki.quarkuscrud.entity.User;
import app.aoki.quarkuscrud.mapper.AuthnProviderMapper;
import io.smallrye.jwt.algorithm.SignatureAlgorithm;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.List;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Service for generating JWT tokens for internally-authenticated users.
 *
 * <p>This service handles JWT token generation for users authenticated by this service (currently
 * anonymous users). When users are authenticated by external providers (OIDC), this service is NOT
 * used - those tokens come directly from the external provider.
 */
@ApplicationScoped
public class JwtService {

  @ConfigProperty(name = "smallrye.jwt.new-token.issuer")
  String issuer;

  @ConfigProperty(name = "smallrye.jwt.new-token.lifespan")
  Long tokenLifespan;

  @Inject AuthnProviderMapper authnProviderMapper;

  /**
   * Generates a JWT token for a user with internal authentication.
   *
   * <p>The token structure uses the user's effective subject from their primary authentication
   * provider:
   *
   * <ul>
   *   <li>Subject (sub): Effective subject (authIdentifier for anonymous, externalSubject for
   *       others)
   *   <li>User Principal Name (upn): Same as subject for consistency
   *   <li>Groups: Authentication method value (e.g., "anonymous")
   *   <li>Expiration: Configurable lifespan
   * </ul>
   *
   * @param user the user to generate a token for
   * @return the signed JWT token
   */
  public String generateToken(User user) {
    // Get the user's primary authentication provider
    List<AuthnProvider> authnProviders = authnProviderMapper.findByUserId(user.getId());
    if (authnProviders.isEmpty()) {
      throw new IllegalStateException("User has no authentication providers");
    }

    // Use the first (primary) authentication provider
    AuthnProvider primaryAuthn = authnProviders.get(0);
    String subject = primaryAuthn.getEffectiveSubject();
    String group = primaryAuthn.getAuthMethod().getValue();

    return Jwt.issuer(issuer)
        .upn(subject) // User principal name - effective subject
        .subject(subject) // Subject - effective subject
        .groups(group) // Authentication method as group
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
    // Get the user's authentication providers
    List<AuthnProvider> authnProviders = authnProviderMapper.findByUserId(user.getId());
    if (authnProviders.isEmpty() || authnProviders.get(0).getAuthMethod() != AuthMethod.ANONYMOUS) {
      throw new IllegalArgumentException("User is not authenticated anonymously");
    }
    return generateToken(user);
  }
}
