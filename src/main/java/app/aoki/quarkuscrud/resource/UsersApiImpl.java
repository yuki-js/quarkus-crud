package app.aoki.quarkuscrud.resource;

import app.aoki.quarkuscrud.entity.User;
import app.aoki.quarkuscrud.generated.api.UsersApi;
import app.aoki.quarkuscrud.generated.model.UserPublic;
import app.aoki.quarkuscrud.service.UserService;
import app.aoki.quarkuscrud.support.Authenticated;
import app.aoki.quarkuscrud.support.ErrorResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.time.ZoneOffset;

@ApplicationScoped
@Path("/api")
public class UsersApiImpl implements UsersApi {

  @Inject UserService userService;

  @Override
  @Authenticated
  @GET
  @Path("/users/{userId}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getUserById(@PathParam("userId") Long userId) {
    return userService
        .findById(userId)
        .map(user -> Response.ok(toUserPublicResponse(user)).build())
        .orElse(
            Response.status(Response.Status.NOT_FOUND)
                .entity(new ErrorResponse("User not found"))
                .build());
  }

  private UserPublic toUserPublicResponse(User user) {
    UserPublic response = new UserPublic();
    response.setId(user.getId());
    response.setCreatedAt(user.getCreatedAt().atOffset(ZoneOffset.UTC));
    if (user.getAccountLifecycle() != null) {
      response.setAccountLifecycle(
          UserPublic.AccountLifecycleEnum.fromValue(user.getAccountLifecycle().getValue()));
    }
    // TODO: Add profile summary if needed
    return response;
  }
}
