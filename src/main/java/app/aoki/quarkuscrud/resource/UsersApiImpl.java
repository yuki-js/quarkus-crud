package app.aoki.quarkuscrud.resource;

import app.aoki.quarkuscrud.entity.User;
import app.aoki.quarkuscrud.filter.Authenticated;
import app.aoki.quarkuscrud.generated.api.UsersApi;
import app.aoki.quarkuscrud.generated.model.CreateGuestUser500Response;
import app.aoki.quarkuscrud.generated.model.GetUserById200Response;
import app.aoki.quarkuscrud.service.UserService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import java.time.ZoneOffset;
import java.util.Optional;

@ApplicationScoped
@Path("/api")
public class UsersApiImpl implements UsersApi {

  @Inject UserService userService;

  @Override
  @Authenticated
  public Response getUserById(Long userId) {
    Optional<User> userOpt = userService.findById(userId);
    if (userOpt.isEmpty()) {
      CreateGuestUser500Response error = new CreateGuestUser500Response();
      error.setError("User not found");
      return Response.status(404).entity(error).build();
    }

    User user = userOpt.get();
    GetUserById200Response response = new GetUserById200Response();
    response.setId(user.getId());
    response.setCreatedAt(user.getCreatedAt().atOffset(ZoneOffset.UTC));
    return Response.ok(response).build();
  }
}
