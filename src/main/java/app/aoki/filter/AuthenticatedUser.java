package app.aoki.filter;

import app.aoki.entity.User;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.ext.Provider;

/** Helper to access the authenticated user from the request context. */
@Provider
public class AuthenticatedUser {

  @Context private ContainerRequestContext requestContext;

  /**
   * Gets the authenticated user from the request context.
   *
   * @return the authenticated user
   * @throws IllegalStateException if no authenticated user is found (should not happen
   *     if @Authenticated is used correctly)
   */
  public User get() {
    User user = (User) requestContext.getProperty(AuthenticationFilter.USER_PROPERTY);
    if (user == null) {
      throw new IllegalStateException("No authenticated user found in request context");
    }
    return user;
  }
}
