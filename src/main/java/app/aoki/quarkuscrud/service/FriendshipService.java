package app.aoki.quarkuscrud.service;

import app.aoki.quarkuscrud.entity.Friendship;
import app.aoki.quarkuscrud.mapper.FriendshipMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing friendships between users.
 *
 * <p>This service handles friendship creation and retrieval. Friendships are directional
 * relationships from sender to recipient.
 */
@ApplicationScoped
public class FriendshipService {

  @Inject FriendshipMapper friendshipMapper;

  /**
   * Lists all friendships where the user is the recipient.
   *
   * @param recipientId the recipient user ID
   * @return list of friendships
   */
  public List<Friendship> findByRecipientId(Long recipientId) {
    return friendshipMapper.findByRecipientId(recipientId);
  }

  /**
   * Creates a new friendship from sender to recipient.
   *
   * @param senderId the sender user ID
   * @param recipientId the recipient user ID
   * @return the created friendship
   */
  @Transactional
  public Friendship createFriendship(Long senderId, Long recipientId) {
    Friendship friendship = new Friendship();
    friendship.setSenderId(senderId);
    friendship.setRecipientId(recipientId);
    LocalDateTime now = LocalDateTime.now();
    friendship.setCreatedAt(now);
    friendship.setUpdatedAt(now);

    friendshipMapper.insert(friendship);
    return friendship;
  }

  /**
   * Checks if a friendship exists from sender to recipient.
   *
   * @param senderId the sender user ID
   * @param recipientId the recipient user ID
   * @return an Optional containing the friendship if it exists
   */
  public Optional<Friendship> findBySenderAndRecipient(Long senderId, Long recipientId) {
    return friendshipMapper.findBySenderAndRecipient(senderId, recipientId);
  }
}
