package app.aoki.quarkuscrud.support;

import app.aoki.quarkuscrud.entity.User;
import app.aoki.quarkuscrud.service.AuthenticationService;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import java.util.Optional;
import org.eclipse.microprofile.jwt.JsonWebToken;

/**
 * JAX-RS filter for authenticating requests using JWT Bearer tokens. Automatically applied to
 * endpoints annotated with @Authenticated.
 *
 * <p>This filter delegates authentication logic to AuthenticationService, maintaining separation of
 * concerns between filter and service layers.
 */
@Provider
@Priority(Priorities.AUTHENTICATION)
public class AuthenticationFilter implements ContainerRequestFilter {

  // Paths that don't require authentication
  private static final String[] PUBLIC_PATHS = {"/api/auth/guest", "/healthz", "/q/"};

  private boolean isPublicEndpoint(ContainerRequestContext requestContext) {
    String path = requestContext.getUriInfo().getPath();
    String method = requestContext.getMethod();

    // Check if path starts with any public path
    for (String publicPath : PUBLIC_PATHS) {
      if (path.startsWith(publicPath)) {
        return true;
      }
    }

    // Special handling for /api/rooms endpoints
    // GET /api/rooms and GET /api/rooms/{id} are public
    // Everything else requires authentication
    if (path.startsWith("/api/rooms")) {
      if ("GET".equals(method) && !path.contains("/my")) {
        return true;
      }
    }

    return false;
  }

  @Inject AuthenticationService authenticationService;

  @Inject JsonWebToken jwt;

  @Override
  public void filter(ContainerRequestContext requestContext) {
    // Skip authentication for public endpoints
    if (isPublicEndpoint(requestContext)) {
      return;
    }

    // Get Authorization header
    String authHeader = requestContext.getHeaderString("Authorization");

    // Get path to customize error messages
    String path = requestContext.getUriInfo().getPath();
    boolean isAuthEndpoint = path.startsWith("/api/auth/") || path.equals("/api/me");

    if (!authenticationService.hasBearerToken(authHeader)) {
      requestContext.abortWith(
          Response.status(Response.Status.UNAUTHORIZED)
              .entity(new ErrorResponse("No JWT token found"))
              .build());
      return;
    }

    // JWT validation is automatically done by Quarkus SmallRye JWT
    // Delegate authentication logic to service layer
    Optional<User> user = authenticationService.authenticateFromJwt(jwt);

    if (user.isEmpty()) {
      requestContext.abortWith(
          Response.status(Response.Status.UNAUTHORIZED)
              .entity(new ErrorResponse("Invalid JWT token"))
              .build());
      return;
    }

    // Store the authenticated user in the ThreadLocal
    AuthenticatedUser.setUser(user.get());
  }
}
