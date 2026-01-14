package app.aoki.quarkuscrud.resource;

import app.aoki.quarkuscrud.entity.User;
import app.aoki.quarkuscrud.generated.model.MetaData;
import app.aoki.quarkuscrud.generated.model.MetaDataUpdateRequest;
import app.aoki.quarkuscrud.service.UserService;
import app.aoki.quarkuscrud.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import java.time.LocalDateTime;
import org.jboss.logging.Logger;

@RequestScoped
public class UserMetaApiImpl {

  private static final Logger LOG = Logger.getLogger(UserMetaApiImpl.class);

  @Inject UserService userService;
  @Inject ObjectMapper objectMapper;

  public Response getUserMeta(Long userId, SecurityContext securityContext) {
    LOG.infof("Getting metadata for user %d", userId);

    Long requestingUserId = JwtUtil.extractUserId(securityContext);
    if (requestingUserId == null) {
      return Response.status(Response.Status.UNAUTHORIZED).entity("Authentication required").build();
    }

    if (!requestingUserId.equals(userId)) {
      return Response.status(Response.Status.FORBIDDEN)
          .entity("You can only access your own metadata")
          .build();
    }

    User user = userService.getUserById(userId);
    if (user == null) {
      return Response.status(Response.Status.NOT_FOUND).entity("User not found").build();
    }

    MetaData metaData = new MetaData();
    try {
      if (user.getUsermeta() != null && !user.getUsermeta().isBlank()) {
        metaData.setUsermeta(objectMapper.readValue(user.getUsermeta(), Object.class));
      } else {
        metaData.setUsermeta(null);
      }
    } catch (Exception e) {
      LOG.errorf("Failed to parse usermeta for user %d: %s", userId, e.getMessage());
      metaData.setUsermeta(null);
    }

    return Response.ok(metaData).build();
  }

  public Response updateUserMeta(
      Long userId, MetaDataUpdateRequest request, SecurityContext securityContext) {
    LOG.infof("Updating metadata for user %d", userId);

    Long requestingUserId = JwtUtil.extractUserId(securityContext);
    if (requestingUserId == null) {
      return Response.status(Response.Status.UNAUTHORIZED).entity("Authentication required").build();
    }

    if (!requestingUserId.equals(userId)) {
      return Response.status(Response.Status.FORBIDDEN)
          .entity("You can only update your own metadata")
          .build();
    }

    User user = userService.getUserById(userId);
    if (user == null) {
      return Response.status(Response.Status.NOT_FOUND).entity("User not found").build();
    }

    try {
      String usermetaJson =
          request.getUsermeta() != null
              ? objectMapper.writeValueAsString(request.getUsermeta())
              : null;
      user.setUsermeta(usermetaJson);
      user.setUpdatedAt(LocalDateTime.now());
      userService.updateUser(user);

      MetaData metaData = new MetaData();
      metaData.setUsermeta(request.getUsermeta());
      return Response.ok(metaData).build();
    } catch (Exception e) {
      LOG.errorf("Failed to update usermeta for user %d: %s", userId, e.getMessage());
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity("Failed to update metadata")
          .build();
    }
  }
}
