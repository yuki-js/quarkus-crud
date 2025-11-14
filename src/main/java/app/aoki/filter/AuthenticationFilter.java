package app.aoki.filter;

import app.aoki.entity.User;
import app.aoki.service.UserService;
import app.aoki.support.ErrorResponse;
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
 * <p>This filter validates JWT tokens issued for guest users. For real user authentication, the
 * system can be extended to accept tokens from external authentication providers.
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

  @Inject UserService userService;

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
    boolean isAuthEndpoint = path.startsWith("/api/auth/");

    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      String errorMessage = isAuthEndpoint ? "No JWT token found" : "Authentication required";
      requestContext.abortWith(
          Response.status(Response.Status.UNAUTHORIZED)
              .entity(new ErrorResponse(errorMessage))
              .build());
      return;
    }

    // JWT validation is automatically done by Quarkus SmallRye JWT
    // If we reach here and jwt is available, it means the token is valid
    if (jwt == null || jwt.getSubject() == null) {
      String errorMessage = isAuthEndpoint ? "Invalid JWT token" : "Authentication required";
      requestContext.abortWith(
          Response.status(Response.Status.UNAUTHORIZED)
              .entity(new ErrorResponse(errorMessage))
              .build());
      return;
    }

    // Get user from JWT subject (UUID)
    String userUuid = jwt.getSubject();
    if (userUuid == null || userUuid.isEmpty()) {
      requestContext.abortWith(
          Response.status(Response.Status.UNAUTHORIZED)
              .entity(new ErrorResponse("Invalid token format"))
              .build());
      return;
    }

    // Look up user by UUID (stored in guestToken field for guest users)
    Optional<User> user = userService.findByGuestToken(userUuid);

    if (user.isEmpty()) {
      String errorMessage = isAuthEndpoint ? "Invalid JWT token" : "Authentication required";
      requestContext.abortWith(
          Response.status(Response.Status.UNAUTHORIZED)
              .entity(new ErrorResponse(errorMessage))
              .build());
      return;
    }

    // Store the authenticated user in the ThreadLocal
    AuthenticatedUser.setUser(user.get());
  }
}
