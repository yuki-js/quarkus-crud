package app.aoki.quarkuscrud.usecase;

import app.aoki.quarkuscrud.entity.Friendship;
import app.aoki.quarkuscrud.mapper.FriendshipMapper;
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
  @Inject FriendshipMapper friendshipMapper;

  /**
   * Gets a friendship by its ID with authorization check.
   *
   * @param friendshipId the friendship ID
   * @param requestingUserId the ID of the user requesting the friendship
   * @return the friendship as DTO
   * @throws IllegalArgumentException if friendship not found
   * @throws SecurityException if the requesting user is not part of the friendship
   */
  public app.aoki.quarkuscrud.generated.model.Friendship getFriendship(
      Long friendshipId, Long requestingUserId) {
    Friendship friendship =
        friendshipService
            .getFriendshipById(friendshipId)
            .orElseThrow(() -> new IllegalArgumentException("Friendship not found"));

    // Authorization check: verify the requesting user is either sender or recipient
    if (!friendship.getSenderId().equals(requestingUserId)
        && !friendship.getRecipientId().equals(requestingUserId)) {
      throw new SecurityException("Access denied. User is not part of this friendship.");
    }

    return toFriendshipDto(friendship);
  }

  /**
   * Lists received friendships for a user.
   *
   * @param userId the recipient user ID
   * @return list of friendship DTOs
   */
  public List<app.aoki.quarkuscrud.generated.model.Friendship> listReceivedFriendships(
      Long userId) {
    return friendshipMapper.findByRecipientId(userId).stream()
        .map(this::toFriendshipDto)
        .collect(Collectors.toList());
  }

  /**
   * Creates a mutual friendship between sender and recipient. This will create bidirectional
   * relationships automatically.
   *
   * @param senderId the sender user ID
   * @param recipientId the recipient user ID
   * @return the created friendship as DTO
   * @throws IllegalArgumentException if recipient user not found
   * @throws IllegalStateException if friendship already exists in either direction
   */
  @Transactional
  public app.aoki.quarkuscrud.generated.model.Friendship createFriendship(
      Long senderId, Long recipientId) {
    if (userService.findById(recipientId).isEmpty()) {
      throw new IllegalArgumentException("User not found");
    }

    // Check if friendship already exists in either direction
    if (friendshipMapper.existsBetweenUsers(senderId, recipientId)) {
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
