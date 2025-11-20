package app.aoki.quarkuscrud.resource;

import app.aoki.quarkuscrud.entity.User;
import app.aoki.quarkuscrud.entity.UserProfile;
import app.aoki.quarkuscrud.filter.Authenticated;
import app.aoki.quarkuscrud.filter.AuthenticatedUser;
import app.aoki.quarkuscrud.generated.api.ProfilesApi;
import app.aoki.quarkuscrud.generated.model.UserProfileUpdateRequest;
import app.aoki.quarkuscrud.mapper.UserMapper;
import app.aoki.quarkuscrud.mapper.UserProfileMapper;
import app.aoki.quarkuscrud.support.ErrorResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
@Path("/api")
public class ProfilesApiImpl implements ProfilesApi {

  @Inject UserProfileMapper userProfileMapper;
  @Inject UserMapper userMapper;
  @Inject AuthenticatedUser authenticatedUser;
  @Inject ObjectMapper objectMapper;

  @Override
  @Authenticated
  @GET
  @Path("/me/profile")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getMyProfile() {
    User user = authenticatedUser.get();
    return userProfileMapper
        .findLatestByUserId(user.getId())
        .map(profile -> Response.ok(toProfileResponse(profile)).build())
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
    return userProfileMapper
        .findLatestByUserId(userId)
        .map(profile -> Response.ok(toProfileResponse(profile)).build())
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
      // Create new profile revision
      UserProfile newProfile = new UserProfile();
      newProfile.setUserId(user.getId());
      newProfile.setProfileData(
          objectMapper.writeValueAsString(updateMyProfileRequest.getProfileData()));
      newProfile.setRevisionMeta(null);
      LocalDateTime now = LocalDateTime.now();
      newProfile.setCreatedAt(now);
      newProfile.setUpdatedAt(now);

      userProfileMapper.insert(newProfile);

      // Update user's current profile revision
      user.setCurrentProfileRevision(newProfile.getId());
      user.setUpdatedAt(now);
      userMapper.update(user);

      return Response.ok(toProfileResponse(newProfile)).build();
    } catch (Exception e) {
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity(new ErrorResponse("Failed to update profile: " + e.getMessage()))
          .build();
    }
  }

  private app.aoki.quarkuscrud.generated.model.UserProfile toProfileResponse(UserProfile profile) {
    app.aoki.quarkuscrud.generated.model.UserProfile response =
        new app.aoki.quarkuscrud.generated.model.UserProfile();
    response.setId(profile.getId());
    response.setUserId(profile.getUserId());
    response.setCreatedAt(profile.getCreatedAt().atOffset(ZoneOffset.UTC));
    response.setUpdatedAt(profile.getUpdatedAt().atOffset(ZoneOffset.UTC));

    // Parse JSON profile data
    try {
      Map<String, Object> profileData =
          objectMapper.readValue(profile.getProfileData(), new TypeReference<>() {});
      response.setProfileData(profileData);
    } catch (Exception e) {
      response.setProfileData(new HashMap<>());
    }

    // Parse JSON revision meta
    if (profile.getRevisionMeta() != null) {
      try {
        Map<String, Object> revisionMeta =
            objectMapper.readValue(profile.getRevisionMeta(), new TypeReference<>() {});
        response.setRevisionMeta(revisionMeta);
      } catch (Exception e) {
        response.setRevisionMeta(new HashMap<>());
      }
    } else {
      response.setRevisionMeta(new HashMap<>());
    }

    return response;
  }
}
