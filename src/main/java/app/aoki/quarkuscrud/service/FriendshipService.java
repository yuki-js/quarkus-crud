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

  /**
   * Finds a friendship by sender and recipient IDs.
   *
   * @param senderId the sender user ID
   * @param recipientId the recipient user ID
   * @return optional friendship if found
   */
  public Optional<Friendship> findBySenderAndRecipient(Long senderId, Long recipientId) {
    return friendshipMapper.findBySenderAndRecipient(senderId, recipientId);
  }

  /**
   * Finds all friendships where the user is the recipient.
   *
   * @param recipientId the recipient user ID
   * @return list of friendships
   */
  public List<Friendship> findByRecipientId(Long recipientId) {
    return friendshipMapper.findByRecipientId(recipientId);
  }
}
