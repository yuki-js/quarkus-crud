package app.aoki.quarkuscrud.resource;

import app.aoki.quarkuscrud.entity.Friendship;
import app.aoki.quarkuscrud.generated.model.MetaData;
import app.aoki.quarkuscrud.generated.model.MetaDataUpdateRequest;
import app.aoki.quarkuscrud.service.FriendshipService;
import app.aoki.quarkuscrud.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import java.time.LocalDateTime;
import org.jboss.logging.Logger;

@RequestScoped
public class FriendshipMetaApiImpl {

  private static final Logger LOG = Logger.getLogger(FriendshipMetaApiImpl.class);

  @Inject FriendshipService friendshipService;
  @Inject ObjectMapper objectMapper;

  public Response getFriendshipMeta(Long otherUserId, SecurityContext securityContext) {
    LOG.infof("Getting metadata for friendship with user %d", otherUserId);

    Long requestingUserId = JwtUtil.extractUserId(securityContext);
    if (requestingUserId == null) {
      return Response.status(Response.Status.UNAUTHORIZED).entity("Authentication required").build();
    }

    Friendship friendship = friendshipService.getFriendship(requestingUserId, otherUserId);
    if (friendship == null) {
      return Response.status(Response.Status.NOT_FOUND).entity("Friendship not found").build();
    }

    MetaData metaData = new MetaData();
    try {
      if (friendship.getUsermeta() != null && !friendship.getUsermeta().isBlank()) {
        metaData.setUsermeta(objectMapper.readValue(friendship.getUsermeta(), Object.class));
      } else {
        metaData.setUsermeta(null);
      }
    } catch (Exception e) {
      LOG.errorf(
          "Failed to parse usermeta for friendship between %d and %d: %s",
          requestingUserId, otherUserId, e.getMessage());
      metaData.setUsermeta(null);
    }

    return Response.ok(metaData).build();
  }

  public Response updateFriendshipMeta(
      Long otherUserId, MetaDataUpdateRequest request, SecurityContext securityContext) {
    LOG.infof("Updating metadata for friendship with user %d", otherUserId);

    Long requestingUserId = JwtUtil.extractUserId(securityContext);
    if (requestingUserId == null) {
      return Response.status(Response.Status.UNAUTHORIZED).entity("Authentication required").build();
    }

    Friendship friendship = friendshipService.getFriendship(requestingUserId, otherUserId);
    if (friendship == null) {
      return Response.status(Response.Status.NOT_FOUND).entity("Friendship not found").build();
    }

    try {
      String usermetaJson =
          request.getUsermeta() != null
              ? objectMapper.writeValueAsString(request.getUsermeta())
              : null;
      friendship.setUsermeta(usermetaJson);
      friendship.setUpdatedAt(LocalDateTime.now());
      friendshipService.updateFriendship(friendship);

      MetaData metaData = new MetaData();
      metaData.setUsermeta(request.getUsermeta());
      return Response.ok(metaData).build();
    } catch (Exception e) {
      LOG.errorf(
          "Failed to update usermeta for friendship between %d and %d: %s",
          requestingUserId, otherUserId, e.getMessage());
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity("Failed to update metadata")
          .build();
    }
  }
}
