package app.aoki.quarkuscrud.resource;

import app.aoki.quarkuscrud.generated.api.AuthenticationApi;
import app.aoki.quarkuscrud.generated.model.User;
import app.aoki.quarkuscrud.service.JwtService;
import app.aoki.quarkuscrud.service.UserService;
import app.aoki.quarkuscrud.support.Authenticated;
import app.aoki.quarkuscrud.support.AuthenticatedUser;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import org.jboss.logging.Logger;

@ApplicationScoped
@Path("/api")
public class AuthenticationApiImpl implements AuthenticationApi {

  private static final Logger LOG = Logger.getLogger(AuthenticationApiImpl.class);

  @Inject UserService userService;
  @Inject JwtService jwtService;
  @Inject AuthenticatedUser authenticatedUser;
  @Inject ObjectMapper objectMapper;

  @Override
  public Response createGuestUser() {
    // Create a new user with anonymous authentication
    app.aoki.quarkuscrud.entity.User user = userService.createAnonymousUser();
    String token = jwtService.generateAnonymousToken(user);

    return Response.ok(toUserResponse(user)).header("Authorization", "Bearer " + token).build();
  }

  @Override
  @Authenticated
  public Response getCurrentUser() {
    app.aoki.quarkuscrud.entity.User user = authenticatedUser.get();
    return Response.ok(toUserResponse(user)).build();
  }

  private User toUserResponse(app.aoki.quarkuscrud.entity.User user) {
    User response = new User();
    response.setId(user.getId());
    response.setCreatedAt(user.getCreatedAt().atOffset(ZoneOffset.UTC));
    response.setUpdatedAt(
        user.getUpdatedAt() != null ? user.getUpdatedAt().atOffset(ZoneOffset.UTC) : null);
    response.setCurrentProfileRevision(user.getCurrentProfileRevision());
    response.setMeta(parseMeta(user.getMeta()));
    if (user.getAccountLifecycle() != null) {
      response.setAccountLifecycle(
          User.AccountLifecycleEnum.fromValue(user.getAccountLifecycle().name().toLowerCase()));
    }
    return response;
  }

  private Map<String, Object> parseMeta(String metaJson) {
    if (metaJson == null || metaJson.isBlank()) {
      return new HashMap<>();
    }
    try {
      return objectMapper.readValue(metaJson, new TypeReference<>() {});
    } catch (Exception e) {
      LOG.errorf(e, "Failed to parse user metadata JSON: %s", metaJson);
      return new HashMap<>();
    }
  }
}
