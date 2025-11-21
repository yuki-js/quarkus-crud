package app.aoki.quarkuscrud.resource;

import app.aoki.quarkuscrud.entity.User;
import app.aoki.quarkuscrud.generated.api.FriendshipsApi;
import app.aoki.quarkuscrud.generated.model.Friendship;
import app.aoki.quarkuscrud.generated.model.ReceiveFriendshipRequest;
import app.aoki.quarkuscrud.support.Authenticated;
import app.aoki.quarkuscrud.support.AuthenticatedUser;
import app.aoki.quarkuscrud.support.ErrorResponse;
import app.aoki.quarkuscrud.usecase.FriendshipUseCase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.postgresql.util.PSQLException;

@ApplicationScoped
@Path("/api")
public class FriendshipsApiImpl implements FriendshipsApi {

  @Inject FriendshipUseCase friendshipUseCase;
  @Inject AuthenticatedUser authenticatedUser;

  @Override
  @Authenticated
  public Response listReceivedFriendships() {
    User user = authenticatedUser.get();
    List<Friendship> friendships = friendshipUseCase.listReceivedFriendships(user.getId());
    return Response.ok(friendships).build();
  }

  @Override
  @Authenticated
  public Response receiveFriendship(Long userId, ReceiveFriendshipRequest request) {
    User sender = authenticatedUser.get();

    try {
      Friendship friendship = friendshipUseCase.createFriendship(sender.getId(), userId);
      return Response.status(Response.Status.CREATED).entity(friendship).build();
    } catch (IllegalArgumentException e) {
      return Response.status(Response.Status.NOT_FOUND)
          .entity(new ErrorResponse(e.getMessage()))
          .build();
    } catch (IllegalStateException e) {
      return Response.status(Response.Status.CONFLICT)
          .entity(new ErrorResponse(e.getMessage()))
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
}
