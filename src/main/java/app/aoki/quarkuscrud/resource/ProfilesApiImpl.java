package app.aoki.quarkuscrud.resource;

import app.aoki.quarkuscrud.entity.User;
import app.aoki.quarkuscrud.generated.api.ProfilesApi;
import app.aoki.quarkuscrud.generated.model.UserProfile;
import app.aoki.quarkuscrud.generated.model.UserProfileUpdateRequest;
import app.aoki.quarkuscrud.support.Authenticated;
import app.aoki.quarkuscrud.support.AuthenticatedUser;
import app.aoki.quarkuscrud.support.ErrorResponse;
import app.aoki.quarkuscrud.usecase.ProfileUseCase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@ApplicationScoped
@Path("/api")
public class ProfilesApiImpl implements ProfilesApi {

  @Inject ProfileUseCase profileUseCase;
  @Inject AuthenticatedUser authenticatedUser;

  @Override
  @Authenticated
  @GET
  @Path("/me/profile")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getMyProfile() {
    User user = authenticatedUser.get();
    return profileUseCase
        .getLatestProfile(user.getId())
        .map(profile -> Response.ok(profile).build())
        .orElse(
            Response.status(Response.Status.NOT_FOUND)
                .entity(new ErrorResponse("Profile not found"))
                .build());
  }

  @Override
  @Authenticated
  @GET
  @Path("/users/{userId}/profile")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getUserProfile(@PathParam("userId") Long userId) {
    return profileUseCase
        .getLatestProfile(userId)
        .map(profile -> Response.ok(profile).build())
        .orElse(
            Response.status(Response.Status.NOT_FOUND)
                .entity(new ErrorResponse("Profile not found"))
                .build());
  }

  @Override
  @Authenticated
  @PUT
  @Path("/me/profile")
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public Response updateMyProfile(UserProfileUpdateRequest updateMyProfileRequest) {
    User user = authenticatedUser.get();

    try {
      UserProfile profile = profileUseCase.updateProfile(user.getId(), updateMyProfileRequest);
      return Response.ok(profile).build();
    } catch (Exception e) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity(new ErrorResponse("Failed to update profile: " + e.getMessage()))
          .build();
    }
  }
}
