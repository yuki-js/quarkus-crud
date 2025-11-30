package app.aoki.quarkuscrud.service;

import app.aoki.quarkuscrud.entity.EventUserData;
import app.aoki.quarkuscrud.mapper.EventUserDataMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Service for managing event user data.
 *
 * <p>This service handles event-specific user data creation, retrieval, and updates. User data is
 * versioned, with each update creating a new revision.
 */
@ApplicationScoped
public class EventUserDataService {

  @Inject EventUserDataMapper eventUserDataMapper;

  /**
   * Finds the latest user data for an event and user.
   *
   * @param eventId the event ID
   * @param userId the user ID
   * @return an Optional containing the user data if found
   */
  public Optional<EventUserData> findLatestByEventIdAndUserId(Long eventId, Long userId) {
    return eventUserDataMapper.findLatestByEventIdAndUserId(eventId, userId);
  }

  /**
   * Creates a new user data revision for an event and user.
   *
   * @param eventId the event ID
   * @param userId the user ID
   * @param userData JSON user data
   * @param revisionMeta optional JSON metadata about the revision
   * @return the created user data
   */
  @Transactional
  public EventUserData createRevision(
      Long eventId, Long userId, String userData, String revisionMeta) {
    EventUserData newData = new EventUserData();
    newData.setEventId(eventId);
    newData.setUserId(userId);
    newData.setUserData(userData);
    newData.setRevisionMeta(revisionMeta);
    LocalDateTime now = LocalDateTime.now();
    newData.setCreatedAt(now);
    newData.setUpdatedAt(now);

    eventUserDataMapper.insert(newData);
    return newData;
  }
}
