package app.aoki.resource;

import app.aoki.entity.User;
import app.aoki.generated.api.AuthenticationApi;
import app.aoki.generated.model.ErrorResponse;
import app.aoki.generated.model.UserResponse;
import app.aoki.service.UserService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import java.time.ZoneOffset;
import java.util.Optional;

@ApplicationScoped
@Path("/api/auth")
public class AuthenticationApiImpl implements AuthenticationApi {

  private static final String GUEST_TOKEN_COOKIE = "guest_token";
  private static final int COOKIE_MAX_AGE = 60 * 60 * 24 * 365; // 1 year

  @Inject UserService userService;

  @Override
  public Response createGuestUser() {
    User user = userService.createGuestUser();

    NewCookie cookie =
        new NewCookie.Builder(GUEST_TOKEN_COOKIE)
            .value(user.getGuestToken())
            .path("/")
            .maxAge(COOKIE_MAX_AGE)
            .httpOnly(true)
            .build();

    return Response.ok(toUserResponse(user)).cookie(cookie).build();
  }

  @Override
  public Response getCurrentUser(String guestToken) {
    if (guestToken == null || guestToken.isEmpty()) {
      return Response.status(Response.Status.UNAUTHORIZED)
          .entity(new ErrorResponse().error("No guest token found"))
          .build();
    }

    Optional<User> user = userService.findByGuestToken(guestToken);
    if (user.isEmpty()) {
      return Response.status(Response.Status.UNAUTHORIZED)
          .entity(new ErrorResponse().error("Invalid guest token"))
          .build();
    }

    return Response.ok(toUserResponse(user.get())).build();
  }

  private UserResponse toUserResponse(User user) {
    return new UserResponse()
        .id(user.getId())
        .createdAt(user.getCreatedAt().atOffset(ZoneOffset.UTC));
  }
}
