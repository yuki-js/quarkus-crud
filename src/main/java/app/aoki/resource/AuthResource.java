package app.aoki.resource;

import app.aoki.entity.User;
import app.aoki.service.UserService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.*;
import java.time.LocalDateTime;
import java.util.Optional;

@Path("/api/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthResource {

  private static final String GUEST_TOKEN_COOKIE = "guest_token";
  private static final int COOKIE_MAX_AGE = 60 * 60 * 24 * 365; // 1 year

  @Inject UserService userService;

  /**
   * Response for authenticated user information. Note: guestToken is NOT included for security -
   * it's only in the cookie.
   */
  public static record UserResponse(Long id, LocalDateTime createdAt) {
    public static UserResponse from(User user) {
      return new UserResponse(user.getId(), user.getCreatedAt());
    }
  }

  @POST
  @Path("/guest")
  public Response createGuestUser() {
    User user = userService.createGuestUser();

    NewCookie cookie =
        new NewCookie.Builder(GUEST_TOKEN_COOKIE)
            .value(user.getGuestToken())
            .path("/")
            .maxAge(COOKIE_MAX_AGE)
            .httpOnly(true)
            .build();

    return Response.ok(UserResponse.from(user)).cookie(cookie).build();
  }

  @GET
  @Path("/me")
  public Response getCurrentUser(@CookieParam(GUEST_TOKEN_COOKIE) String guestToken) {
    if (guestToken == null || guestToken.isEmpty()) {
      return Response.status(Response.Status.UNAUTHORIZED)
          .entity("{\"error\": \"No guest token found\"}")
          .build();
    }

    Optional<User> user = userService.findByGuestToken(guestToken);
    if (user.isEmpty()) {
      return Response.status(Response.Status.UNAUTHORIZED)
          .entity("{\"error\": \"Invalid guest token\"}")
          .build();
    }

    return Response.ok(UserResponse.from(user.get())).build();
  }
}
