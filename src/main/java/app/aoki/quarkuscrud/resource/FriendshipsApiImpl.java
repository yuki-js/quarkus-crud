package app.aoki.quarkuscrud.resource;

import app.aoki.quarkuscrud.entity.Friendship;
import app.aoki.quarkuscrud.entity.User;
import app.aoki.quarkuscrud.filter.Authenticated;
import app.aoki.quarkuscrud.filter.AuthenticatedUser;
import app.aoki.quarkuscrud.generated.api.FriendshipsApi;
import app.aoki.quarkuscrud.generated.model.ReceiveFriendship201Response;
import app.aoki.quarkuscrud.generated.model.ReceiveFriendshipRequest;
import app.aoki.quarkuscrud.mapper.FriendshipMapper;
import app.aoki.quarkuscrud.mapper.UserMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;
import org.postgresql.util.PSQLException;

@ApplicationScoped
@Path("/api")
public class FriendshipsApiImpl implements FriendshipsApi {

  @Inject FriendshipMapper friendshipMapper;
  @Inject UserMapper userMapper;
  @Inject AuthenticatedUser authenticatedUser;

  @Override
  @Authenticated
  public Response listReceivedFriendships() {
    User user = authenticatedUser.get();
    List<Friendship> friendships = friendshipMapper.findByRecipientId(user.getId());
    List<ReceiveFriendship201Response> responses =
        friendships.stream().map(this::toFriendshipResponse).collect(Collectors.toList());
    return Response.ok(responses).build();
  }

  @Override
  @Authenticated
  public Response receiveFriendship(
      @PathParam("userId") Long userId, @Valid ReceiveFriendshipRequest receiveFriendshipRequest) {
    User currentUser = authenticatedUser.get();

    // Prevent users from receiving friendship from themselves
    if (userId.equals(currentUser.getId())) {
      return Response.status(Response.Status.BAD_REQUEST)
          .entity(new app.aoki.quarkuscrud.support.ErrorResponse("Cannot receive friendship from yourself"))
          .build();
    }

    // Verify the sender user exists
    if (userMapper.findById(userId).isEmpty()) {
      return Response.status(Response.Status.NOT_FOUND)
          .entity(new app.aoki.quarkuscrud.support.ErrorResponse("User not found"))
          .build();
    }

    // Check if friendship already exists
    if (friendshipMapper.findBySenderAndRecipient(userId, currentUser.getId()).isPresent()) {
      return Response.status(Response.Status.CONFLICT)
          .entity(new app.aoki.quarkuscrud.support.ErrorResponse("Friendship already exists"))
          .build();
    }

    // Create friendship
    Friendship friendship = new Friendship();
    friendship.setSenderId(userId);
    friendship.setRecipientId(currentUser.getId());
    LocalDateTime now = LocalDateTime.now();
    friendship.setCreatedAt(now);
    friendship.setUpdatedAt(now);

    try {
      friendshipMapper.insert(friendship);
      return Response.status(Response.Status.CREATED)
          .entity(toFriendshipResponse(friendship))
          .build();
    } catch (Exception e) {
      // Handle unique constraint violation
      if (e.getCause() instanceof PSQLException) {
        PSQLException psqlException = (PSQLException) e.getCause();
        if (psqlException.getSQLState() != null
            && psqlException.getSQLState().equals("23505")) { // Unique violation
          return Response.status(Response.Status.CONFLICT)
              .entity(new app.aoki.quarkuscrud.support.ErrorResponse("Friendship already exists"))
              .build();
        }
      }
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity(
              new app.aoki.quarkuscrud.support.ErrorResponse(
                  "Failed to create friendship: " + e.getMessage()))
          .build();
    }
  }

  private ReceiveFriendship201Response toFriendshipResponse(Friendship friendship) {
    ReceiveFriendship201Response response = new ReceiveFriendship201Response();
    response.setId(friendship.getId());
    response.setSenderUserId(friendship.getSenderId());
    response.setRecipientUserId(friendship.getRecipientId());
    response.setCreatedAt(friendship.getCreatedAt().atOffset(ZoneOffset.UTC));
    response.setUpdatedAt(friendship.getUpdatedAt().atOffset(ZoneOffset.UTC));
    // TODO: Add sender profile if needed
    return response;
  }
}
