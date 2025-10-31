package app.aoki.filter;

import app.aoki.entity.User;
import app.aoki.exception.ErrorResponse;
import app.aoki.service.UserService;
import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Cookie;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import java.util.Optional;

/**
 * JAX-RS filter for authenticating requests using guest token from cookies. Automatically applied
 * to endpoints annotated with @Authenticated.
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

  private static final String GUEST_TOKEN_COOKIE = "guest_token";

  @Inject UserService userService;

  @Override
  public void filter(ContainerRequestContext requestContext) {
    // Skip authentication for public endpoints
    if (isPublicEndpoint(requestContext)) {
      return;
    }

    Cookie cookie = requestContext.getCookies().get(GUEST_TOKEN_COOKIE);

    // Get path to customize error messages
    String path = requestContext.getUriInfo().getPath();
    boolean isAuthEndpoint = path.startsWith("/api/auth/");

    if (cookie == null || cookie.getValue() == null || cookie.getValue().isEmpty()) {
      String errorMessage = isAuthEndpoint ? "No guest token found" : "Authentication required";
      requestContext.abortWith(
          Response.status(Response.Status.UNAUTHORIZED)
              .entity(new ErrorResponse(errorMessage))
              .build());
      return;
    }

    String guestToken = cookie.getValue();
    Optional<User> user = userService.findByGuestToken(guestToken);

    if (user.isEmpty()) {
      String errorMessage = isAuthEndpoint ? "Invalid guest token" : "Authentication required";
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
