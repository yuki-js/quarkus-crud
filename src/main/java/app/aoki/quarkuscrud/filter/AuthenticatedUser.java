package app.aoki.quarkuscrud.filter;

import app.aoki.quarkuscrud.entity.User;
import jakarta.enterprise.context.RequestScoped;

/** Helper to access the authenticated user from the request context. */
@RequestScoped
public class AuthenticatedUser {

  private static final ThreadLocal<User> USER_THREAD_LOCAL = new ThreadLocal<>();

  // Package-private setter for AuthenticationFilter to set the user
  static void setUser(User user) {
    USER_THREAD_LOCAL.set(user);
  }

  // Package-private method to clear the user (for cleanup)
  static void clear() {
    USER_THREAD_LOCAL.remove();
  }

  /**
   * Gets the authenticated user from the request context.
   *
   * @return the authenticated user
   * @throws IllegalStateException if no authenticated user is found (should not happen
   *     if @Authenticated is used correctly)
   */
  public User get() {
    User user = USER_THREAD_LOCAL.get();
    if (user == null) {
      throw new IllegalStateException("No authenticated user found in request context");
    }
    return user;
  }
}
