package app.aoki.quarkuscrud.resource;

import app.aoki.quarkuscrud.entity.User;
import app.aoki.quarkuscrud.filter.Authenticated;
import app.aoki.quarkuscrud.filter.AuthenticatedUser;
import app.aoki.quarkuscrud.generated.api.FriendshipsApi;
import app.aoki.quarkuscrud.generated.model.Friendship;
import app.aoki.quarkuscrud.generated.model.ReceiveFriendshipRequest;
import app.aoki.quarkuscrud.service.FriendshipService;
import app.aoki.quarkuscrud.service.UserService;
import app.aoki.quarkuscrud.support.ErrorResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;
import org.postgresql.util.PSQLException;

@ApplicationScoped
@Path("/api")
public class FriendshipsApiImpl implements FriendshipsApi {

  @Inject FriendshipService friendshipService;
  @Inject UserService userService;
  @Inject AuthenticatedUser authenticatedUser;

  @Override
  @Authenticated
  public Response listReceivedFriendships() {
    User user = authenticatedUser.get();
    List<Friendship> payload =
        friendshipService.findByRecipientId(user.getId()).stream()
            .map(this::toFriendshipResponse)
            .collect(Collectors.toList());
    return Response.ok(payload).build();
  }

  @Override
  @Authenticated
  public Response receiveFriendship(Long userId, ReceiveFriendshipRequest request) {
    User sender = authenticatedUser.get();

    if (userService.findById(userId).isEmpty()) {
      return Response.status(Response.Status.NOT_FOUND)
          .entity(new ErrorResponse("User not found"))
          .build();
    }

    if (friendshipService.findBySenderAndRecipient(sender.getId(), userId).isPresent()) {
      return Response.status(Response.Status.CONFLICT)
          .entity(new ErrorResponse("Friendship already exists"))
          .build();
    }

    try {
      app.aoki.quarkuscrud.entity.Friendship friendship =
          friendshipService.createFriendship(sender.getId(), userId);
      return Response.status(Response.Status.CREATED)
          .entity(toFriendshipResponse(friendship))
          .build();
    } catch (Exception e) {
      if (e.getCause() instanceof PSQLException psqlException
          && "23505".equals(psqlException.getSQLState())) {
        return Response.status(Response.Status.CONFLICT)
            .entity(new ErrorResponse("Friendship already exists"))
            .build();
      }
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity(new ErrorResponse("Failed to create friendship: " + e.getMessage()))
          .build();
    }
  }

  private Friendship toFriendshipResponse(app.aoki.quarkuscrud.entity.Friendship friendship) {
    Friendship response = new Friendship();
    response.setId(friendship.getId());
    response.setSenderUserId(friendship.getSenderId());
    response.setRecipientUserId(friendship.getRecipientId());
    response.setCreatedAt(friendship.getCreatedAt().atOffset(ZoneOffset.UTC));
    if (friendship.getUpdatedAt() != null) {
      response.setUpdatedAt(friendship.getUpdatedAt().atOffset(ZoneOffset.UTC));
    }
    return response;
  }
}
