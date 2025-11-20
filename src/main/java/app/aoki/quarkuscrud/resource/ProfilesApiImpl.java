package app.aoki.quarkuscrud.resource;

import app.aoki.quarkuscrud.entity.User;
import app.aoki.quarkuscrud.entity.UserProfile;
import app.aoki.quarkuscrud.filter.Authenticated;
import app.aoki.quarkuscrud.filter.AuthenticatedUser;
import app.aoki.quarkuscrud.generated.api.ProfilesApi;
import app.aoki.quarkuscrud.generated.model.GetUserProfile200Response;
import app.aoki.quarkuscrud.generated.model.UpdateMyProfileRequest;
import app.aoki.quarkuscrud.mapper.UserMapper;
import app.aoki.quarkuscrud.mapper.UserProfileMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
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
  public Response getMyProfile() {
    User user = authenticatedUser.get();
    return userProfileMapper
        .findLatestByUserId(user.getId())
        .map(profile -> Response.ok(toProfileResponse(profile)).build())
        .orElse(
            Response.status(Response.Status.NOT_FOUND)
                .entity(new app.aoki.quarkuscrud.support.ErrorResponse("Profile not found"))
                .build());
  }

  @Override
  @Authenticated
  public Response getUserProfile(@PathParam("userId") Long userId) {
    return userProfileMapper
        .findLatestByUserId(userId)
        .map(profile -> Response.ok(toProfileResponse(profile)).build())
        .orElse(
            Response.status(Response.Status.NOT_FOUND)
                .entity(new app.aoki.quarkuscrud.support.ErrorResponse("Profile not found"))
                .build());
  }

  @Override
  @Authenticated
  public Response updateMyProfile(UpdateMyProfileRequest updateMyProfileRequest) {
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
          .entity(
              new app.aoki.quarkuscrud.support.ErrorResponse(
                  "Failed to update profile: " + e.getMessage()))
          .build();
    }
  }

  private GetUserProfile200Response toProfileResponse(UserProfile profile) {
    GetUserProfile200Response response = new GetUserProfile200Response();
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
