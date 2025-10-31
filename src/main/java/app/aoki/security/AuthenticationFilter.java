package app.aoki.security;

import app.aoki.entity.User;
import app.aoki.model.ErrorResponse;
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
@Authenticated
@Priority(Priorities.AUTHENTICATION)
public class AuthenticationFilter implements ContainerRequestFilter {

  private static final String GUEST_TOKEN_COOKIE = "guest_token";
  static final String USER_PROPERTY = "authenticated.user";

  @Inject UserService userService;

  @Override
  public void filter(ContainerRequestContext requestContext) {
    Cookie cookie = requestContext.getCookies().get(GUEST_TOKEN_COOKIE);

    if (cookie == null || cookie.getValue() == null || cookie.getValue().isEmpty()) {
      requestContext.abortWith(
          Response.status(Response.Status.UNAUTHORIZED)
              .entity(new ErrorResponse("Authentication required"))
              .build());
      return;
    }

    String guestToken = cookie.getValue();
    Optional<User> user = userService.findByGuestToken(guestToken);

    if (user.isEmpty()) {
      requestContext.abortWith(
          Response.status(Response.Status.UNAUTHORIZED)
              .entity(new ErrorResponse("Invalid authentication"))
              .build());
      return;
    }

    // Store the authenticated user in the request context for use in the endpoint
    requestContext.setProperty(USER_PROPERTY, user.get());
  }
}
