package app.aoki.quarkuscrud.resource;

import app.aoki.quarkuscrud.entity.AccountLifecycle;
import app.aoki.quarkuscrud.entity.User;
import app.aoki.quarkuscrud.filter.Authenticated;
import app.aoki.quarkuscrud.filter.AuthenticatedUser;
import app.aoki.quarkuscrud.generated.api.AuthenticationApi;
import app.aoki.quarkuscrud.generated.model.CreateGuestUser200Response;
import app.aoki.quarkuscrud.service.JwtService;
import app.aoki.quarkuscrud.service.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import java.time.ZoneOffset;
import java.util.Map;

@ApplicationScoped
public class AuthenticationApiImpl implements AuthenticationApi {

  @Inject UserService userService;
  @Inject JwtService jwtService;
  @Inject AuthenticatedUser authenticatedUser;
  @Inject ObjectMapper objectMapper;

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
    response.setAccountLifecycle(mapAccountLifecycle(user.getAccountLifecycle()));
    response.setCurrentProfileRevision(user.getCurrentProfileRevision());
    if (user.getMeta() != null) {
      try {
        @SuppressWarnings("unchecked")
        Map<String, Object> metaMap =
            objectMapper.readValue(user.getMeta(), Map.class);
        response.setMeta(metaMap);
      } catch (JsonProcessingException e) {
        // If meta is not valid JSON, leave it null
      }
    }
    response.setCreatedAt(user.getCreatedAt().atOffset(ZoneOffset.UTC));
    response.setUpdatedAt(user.getUpdatedAt().atOffset(ZoneOffset.UTC));
    return response;
  }

  private CreateGuestUser200Response.AccountLifecycleEnum mapAccountLifecycle(
      AccountLifecycle lifecycle) {
    if (lifecycle == null) {
      return null;
    }
    return CreateGuestUser200Response.AccountLifecycleEnum.fromValue(lifecycle.name().toLowerCase());
  }
}
