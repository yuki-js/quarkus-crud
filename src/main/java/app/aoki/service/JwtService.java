package app.aoki.service;

import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.HashSet;
import java.util.Set;
import org.eclipse.microprofile.config.inject.ConfigProperty;

@ApplicationScoped
public class JwtService {

  @ConfigProperty(name = "smallrye.jwt.new-token.issuer")
  String issuer;

  @ConfigProperty(name = "smallrye.jwt.new-token.lifespan")
  Long tokenLifespan;

  /**
   * Generate a JWT token for a user.
   *
   * @param userId the user ID
   * @param username the username (optional, can be null for guest users)
   * @param roles comma-separated roles string
   * @return the JWT token
   */
  public String generateToken(Long userId, String username, String roles) {
    Set<String> groups = new HashSet<>();
    if (roles != null && !roles.isEmpty()) {
      for (String role : roles.split(",")) {
        groups.add(role.trim());
      }
    }

    // Always include 'user' role as minimum
    if (groups.isEmpty()) {
      groups.add("user");
    }

    var builder =
        Jwt.issuer(issuer)
            .upn(username != null ? username : "guest_" + userId)
            .subject(userId.toString())
            .groups(groups)
            .expiresIn(tokenLifespan);

    return builder.sign();
  }

  /**
   * Generate a JWT token for a guest user.
   *
   * @param userId the user ID
   * @return the JWT token
   */
  public String generateGuestToken(Long userId) {
    return generateToken(userId, null, "guest");
  }
}
