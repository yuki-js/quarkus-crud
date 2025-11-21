package app.aoki.quarkuscrud.usecase;

import app.aoki.quarkuscrud.entity.Friendship;
import app.aoki.quarkuscrud.service.FriendshipService;
import app.aoki.quarkuscrud.service.UserService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Use case for friendship-related business flows.
 *
 * <p>This use case orchestrates complete business operations including validation, business logic,
 * and DTO mapping.
 */
@ApplicationScoped
public class FriendshipUseCase {

  @Inject FriendshipService friendshipService;
  @Inject UserService userService;

  /**
   * Lists received friendships for a user.
   *
   * @param userId the recipient user ID
   * @return list of friendship DTOs
   */
  public List<app.aoki.quarkuscrud.generated.model.Friendship> listReceivedFriendships(
      Long userId) {
    return friendshipService.findByRecipientId(userId).stream()
        .map(this::toFriendshipDto)
        .collect(Collectors.toList());
  }

  /**
   * Creates a friendship from sender to recipient.
   *
   * @param senderId the sender user ID
   * @param recipientId the recipient user ID
   * @return the created friendship as DTO
   * @throws IllegalArgumentException if recipient user not found
   * @throws IllegalStateException if friendship already exists
   */
  @Transactional
  public app.aoki.quarkuscrud.generated.model.Friendship createFriendship(
      Long senderId, Long recipientId) {
    if (userService.findById(recipientId).isEmpty()) {
      throw new IllegalArgumentException("User not found");
    }

    if (friendshipService.findBySenderAndRecipient(senderId, recipientId).isPresent()) {
      throw new IllegalStateException("Friendship already exists");
    }

    Friendship friendship = friendshipService.createFriendship(senderId, recipientId);
    return toFriendshipDto(friendship);
  }

  private app.aoki.quarkuscrud.generated.model.Friendship toFriendshipDto(Friendship friendship) {
    app.aoki.quarkuscrud.generated.model.Friendship response =
        new app.aoki.quarkuscrud.generated.model.Friendship();
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
