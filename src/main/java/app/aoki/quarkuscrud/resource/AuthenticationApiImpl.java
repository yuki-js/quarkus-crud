package app.aoki.quarkuscrud.resource;

import app.aoki.quarkuscrud.entity.User;
import app.aoki.quarkuscrud.filter.Authenticated;
import app.aoki.quarkuscrud.filter.AuthenticatedUser;
import app.aoki.quarkuscrud.generated.api.AuthenticationApi;
import app.aoki.quarkuscrud.generated.model.CreateGuestUser200Response;
import app.aoki.quarkuscrud.service.JwtService;
import app.aoki.quarkuscrud.service.UserService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import java.time.ZoneOffset;

@ApplicationScoped
@Path("/api")
public class AuthenticationApiImpl implements AuthenticationApi {

  @Inject UserService userService;
  @Inject JwtService jwtService;
  @Inject AuthenticatedUser authenticatedUser;

  @Override
  public Response createGuestUser() {
    // Create a new user with anonymous authentication
    User user = userService.createAnonymousUser();
    String token = jwtService.generateAnonymousToken(user);

    return Response.ok(toUserResponse(user)).header("Authorization", "Bearer " + token).build();
  }

  @Override
  @Authenticated
  public Response getCurrentUser() {
    User user = authenticatedUser.get();
    return Response.ok(toUserResponse(user)).build();
  }

  private CreateGuestUser200Response toUserResponse(User user) {
    CreateGuestUser200Response response = new CreateGuestUser200Response();
    response.setId(user.getId());
    response.setCreatedAt(user.getCreatedAt().atOffset(ZoneOffset.UTC));
    if (user.getAccountLifecycle() != null) {
      response.setAccountLifecycle(
          CreateGuestUser200Response.AccountLifecycleEnum.fromValue(
              user.getAccountLifecycle().name().toLowerCase()));
    }
    return response;
  }
}
