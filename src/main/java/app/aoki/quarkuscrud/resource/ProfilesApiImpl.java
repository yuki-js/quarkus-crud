package app.aoki.quarkuscrud.resource;

import app.aoki.quarkuscrud.entity.User;
import app.aoki.quarkuscrud.entity.UserProfile;
import app.aoki.quarkuscrud.filter.Authenticated;
import app.aoki.quarkuscrud.filter.AuthenticatedUser;
import app.aoki.quarkuscrud.generated.api.ProfilesApi;
import app.aoki.quarkuscrud.generated.model.CreateGuestUser500Response;
import app.aoki.quarkuscrud.generated.model.GetUserProfile200Response;
import app.aoki.quarkuscrud.generated.model.UpdateMyProfileRequest;
import app.aoki.quarkuscrud.service.ProfileService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;

@ApplicationScoped
public class ProfilesApiImpl implements ProfilesApi {

  @Inject ProfileService profileService;
  @Inject AuthenticatedUser authenticatedUser;
  @Inject ObjectMapper objectMapper;

  @Override
  @Authenticated
  public Response getMyProfile() {
    User user = authenticatedUser.get();
    Optional<UserProfile> profileOpt = profileService.getCurrentProfile(user.getId());

    if (profileOpt.isEmpty()) {
      CreateGuestUser500Response error = new CreateGuestUser500Response();
      error.setError("Profile not found");
      return Response.status(404).entity(error).build();
    }

    return Response.ok(toProfileResponse(profileOpt.get())).build();
  }

  @Override
  @Authenticated
  public Response getUserProfile(Long userId) {
    Optional<UserProfile> profileOpt = profileService.getCurrentProfile(userId);

    if (profileOpt.isEmpty()) {
      CreateGuestUser500Response error = new CreateGuestUser500Response();
      error.setError("Profile not found");
      return Response.status(404).entity(error).build();
    }

    return Response.ok(toProfileResponse(profileOpt.get())).build();
  }

  @Override
  @Authenticated
  public Response updateMyProfile(UpdateMyProfileRequest request) {
    User user = authenticatedUser.get();

    try {
      String profileDataJson = objectMapper.writeValueAsString(request.getProfileData());
      UserProfile profile = profileService.createOrUpdateProfile(user.getId(), profileDataJson);
      return Response.ok(toProfileResponse(profile)).build();
    } catch (JsonProcessingException e) {
      CreateGuestUser500Response error = new CreateGuestUser500Response();
      error.setError("Invalid profile data");
      return Response.status(400).entity(error).build();
    }
  }

  private GetUserProfile200Response toProfileResponse(UserProfile profile) {
    GetUserProfile200Response response = new GetUserProfile200Response();
    response.setId(profile.getId());
    response.setUserId(profile.getUserId());

    if (profile.getProfileData() != null) {
      try {
        @SuppressWarnings("unchecked")
        Map<String, Object> profileDataMap =
            objectMapper.readValue(profile.getProfileData(), Map.class);
        response.setProfileData(profileDataMap);
      } catch (JsonProcessingException e) {
        // Leave profileData null if invalid
      }
    }

    if (profile.getRevisionMeta() != null) {
      try {
        @SuppressWarnings("unchecked")
        Map<String, Object> revisionMetaMap =
            objectMapper.readValue(profile.getRevisionMeta(), Map.class);
        response.setRevisionMeta(revisionMetaMap);
      } catch (JsonProcessingException e) {
        // Leave revisionMeta null if invalid
      }
    }

    response.setCreatedAt(profile.getCreatedAt().atOffset(ZoneOffset.UTC));
    response.setUpdatedAt(profile.getUpdatedAt().atOffset(ZoneOffset.UTC));
    return response;
  }
}
