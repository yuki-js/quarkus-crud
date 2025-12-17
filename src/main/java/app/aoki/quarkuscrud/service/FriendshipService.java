package app.aoki.quarkuscrud.service;

import app.aoki.quarkuscrud.entity.Friendship;
import app.aoki.quarkuscrud.mapper.FriendshipMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;

/**
 * Service for managing friendships between users.
 *
 * <p>This service handles friendship creation and retrieval. Friendships are mutual
 * (bidirectional) relationships between users.
 */
@ApplicationScoped
public class FriendshipService {

  @Inject FriendshipMapper friendshipMapper;

  /**
   * Creates a new mutual friendship between sender and recipient.
   * This creates two directional relationships: sender->recipient and recipient->sender.
   *
   * @param senderId the sender user ID
   * @param recipientId the recipient user ID
   * @return the created friendship from sender to recipient
   */
  @Transactional
  public Friendship createFriendship(Long senderId, Long recipientId) {
    LocalDateTime now = LocalDateTime.now();

    // Create friendship from sender to recipient
    Friendship friendship = new Friendship();
    friendship.setSenderId(senderId);
    friendship.setRecipientId(recipientId);
    friendship.setCreatedAt(now);
    friendship.setUpdatedAt(now);
    friendshipMapper.insert(friendship);

    // Create reverse friendship from recipient to sender
    Friendship reverseFriendship = new Friendship();
    reverseFriendship.setSenderId(recipientId);
    reverseFriendship.setRecipientId(senderId);
    reverseFriendship.setCreatedAt(now);
    reverseFriendship.setUpdatedAt(now);
    friendshipMapper.insert(reverseFriendship);

    return friendship;
  }
}
