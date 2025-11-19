package app.aoki.quarkuscrud.resource;

import app.aoki.quarkuscrud.entity.Friendship;
import app.aoki.quarkuscrud.entity.User;
import app.aoki.quarkuscrud.filter.Authenticated;
import app.aoki.quarkuscrud.filter.AuthenticatedUser;
import app.aoki.quarkuscrud.generated.api.FriendshipsApi;
import app.aoki.quarkuscrud.generated.model.CreateGuestUser500Response;
import app.aoki.quarkuscrud.generated.model.ReceiveFriendship201Response;
import app.aoki.quarkuscrud.generated.model.ReceiveFriendshipRequest;
import app.aoki.quarkuscrud.service.FriendshipService;
import app.aoki.quarkuscrud.service.UserService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class FriendshipsApiImpl implements FriendshipsApi {

  @Inject FriendshipService friendshipService;
  @Inject UserService userService;
  @Inject AuthenticatedUser authenticatedUser;

  @Override
  @Authenticated
  public Response listReceivedFriendships() {
    User user = authenticatedUser.get();
    List<Friendship> friendships = friendshipService.getReceivedFriendships(user.getId());

    List<ReceiveFriendship201Response> responses =
        friendships.stream().map(this::toFriendshipResponse).collect(Collectors.toList());

    return Response.ok(responses).build();
  }

  @Override
  @Authenticated
  public Response receiveFriendship(Long userId, ReceiveFriendshipRequest request) {
    User recipient = authenticatedUser.get();

    // Verify sender exists
    if (userService.findById(userId).isEmpty()) {
      CreateGuestUser500Response error = new CreateGuestUser500Response();
      error.setError("User not found");
      return Response.status(404).entity(error).build();
    }

    // Check if friendship already exists
    if (friendshipService.exists(userId, recipient.getId())) {
      CreateGuestUser500Response error = new CreateGuestUser500Response();
      error.setError("Friendship already exists");
      return Response.status(409).entity(error).build();
    }

    // Create friendship (userId is sender, recipient is the authenticated user)
    Friendship friendship = friendshipService.createFriendship(userId, recipient.getId());

    return Response.status(201).entity(toFriendshipResponse(friendship)).build();
  }

  private ReceiveFriendship201Response toFriendshipResponse(Friendship friendship) {
    ReceiveFriendship201Response response = new ReceiveFriendship201Response();
    response.setId(friendship.getId());
    response.setSenderUserId(friendship.getSenderId());
    response.setRecipientUserId(friendship.getRecipientId());
    response.setCreatedAt(friendship.getCreatedAt().atOffset(ZoneOffset.UTC));
    response.setUpdatedAt(friendship.getUpdatedAt().atOffset(ZoneOffset.UTC));
    return response;
  }
}
