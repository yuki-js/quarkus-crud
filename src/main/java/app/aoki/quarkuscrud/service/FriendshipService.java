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
 * Service for managing friendships (profile card exchanges).
 *
 * <p>Friendships are unidirectional - a user can send their profile card to another user.
 */
@ApplicationScoped
public class FriendshipService {

  @Inject FriendshipMapper friendshipMapper;

  /**
   * Create a friendship (send profile card from sender to recipient).
   *
   * @param senderId the user sending their profile
   * @param recipientId the user receiving the profile
   * @return the created friendship
   */
  @Transactional
  public Friendship createFriendship(Long senderId, Long recipientId) {
    // Check if friendship already exists
    Optional<Friendship> existing = friendshipMapper.findBySenderAndRecipient(senderId, recipientId);
    if (existing.isPresent()) {
      return existing.get();
    }

    Friendship friendship = new Friendship();
    friendship.setSenderId(senderId);
    friendship.setRecipientId(recipientId);
    friendship.setCreatedAt(LocalDateTime.now());
    friendship.setUpdatedAt(LocalDateTime.now());
    friendshipMapper.insert(friendship);
    return friendship;
  }

  /**
   * Get all friendships where the user is the recipient (received profile cards).
   *
   * @param userId the recipient user ID
   * @return list of friendships
   */
  public List<Friendship> getReceivedFriendships(Long userId) {
    return friendshipMapper.findByRecipientId(userId);
  }

  /**
   * Get all friendships where the user is the sender (sent profile cards).
   *
   * @param userId the sender user ID
   * @return list of friendships
   */
  public List<Friendship> getSentFriendships(Long userId) {
    return friendshipMapper.findBySenderId(userId);
  }

  /**
   * Check if a friendship exists between sender and recipient.
   *
   * @param senderId the sender user ID
   * @param recipientId the recipient user ID
   * @return true if friendship exists
   */
  public boolean exists(Long senderId, Long recipientId) {
    return friendshipMapper.findBySenderAndRecipient(senderId, recipientId).isPresent();
  }

  /**
   * Delete a friendship.
   *
   * @param friendshipId the friendship ID
   */
  @Transactional
  public void deleteFriendship(Long friendshipId) {
    friendshipMapper.deleteById(friendshipId);
  }
}
