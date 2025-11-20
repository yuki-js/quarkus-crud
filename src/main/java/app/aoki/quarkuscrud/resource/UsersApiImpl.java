package app.aoki.quarkuscrud.resource;

import app.aoki.quarkuscrud.entity.User;
import app.aoki.quarkuscrud.filter.Authenticated;
import app.aoki.quarkuscrud.generated.api.UsersApi;
import app.aoki.quarkuscrud.generated.model.GetUserById200Response;
import app.aoki.quarkuscrud.mapper.UserMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;
import java.time.ZoneOffset;

@ApplicationScoped
@Path("/api/users/{userId}")
public class UsersApiImpl implements UsersApi {

  @Inject UserMapper userMapper;

  @Override
  @Authenticated
  public Response getUserById(@PathParam("userId") Long userId) {
    return userMapper
        .findById(userId)
        .map(user -> Response.ok(toUserPublicResponse(user)).build())
        .orElse(
            Response.status(Response.Status.NOT_FOUND)
                .entity(new app.aoki.quarkuscrud.support.ErrorResponse("User not found"))
                .build());
  }

  private GetUserById200Response toUserPublicResponse(User user) {
    GetUserById200Response response = new GetUserById200Response();
    response.setId(user.getId());
    response.setCreatedAt(user.getCreatedAt().atOffset(ZoneOffset.UTC));
    if (user.getAccountLifecycle() != null) {
      response.setAccountLifecycle(
          GetUserById200Response.AccountLifecycleEnum.fromValue(
              user.getAccountLifecycle().getValue()));
    }
    // TODO: Add profile summary if needed
    return response;
  }
}
