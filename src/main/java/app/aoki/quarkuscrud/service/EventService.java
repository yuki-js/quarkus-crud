package app.aoki.quarkuscrud.service;

import app.aoki.quarkuscrud.entity.Event;
import app.aoki.quarkuscrud.entity.EventAttendee;
import app.aoki.quarkuscrud.entity.EventInvitationCode;
import app.aoki.quarkuscrud.entity.EventStatus;
import app.aoki.quarkuscrud.entity.EventUserData;
import app.aoki.quarkuscrud.mapper.EventAttendeeMapper;
import app.aoki.quarkuscrud.mapper.EventInvitationCodeMapper;
import app.aoki.quarkuscrud.mapper.EventMapper;
import app.aoki.quarkuscrud.mapper.EventUserDataMapper;
import io.quarkus.narayana.jta.QuarkusTransaction;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.security.SecureRandom;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.jboss.logging.Logger;

/**
 * Service for managing events.
 *
 * <p>This service handles event lifecycle operations including creation, retrieval, and attendee
 * management. It also manages invitation codes for events.
 */
@ApplicationScoped
public class EventService {

  private static final Logger LOG = Logger.getLogger(EventService.class);

  // Invitation code character set, so that Japanese users can easily use it. We
  // never change this
  private static final String INVITATION_CODE_CHARS = "あいうえおかきくけこさしすせそたちつてとなにぬねのはひふへほまみむめも";

  // Invitation code length so that it is short enough to be memorable. We never
  // change this. Collision probability is high, so we permit collision between
  // active and deleted/expired events and introduce robust collision handling
  // logic
  private static final int INVITATION_CODE_LENGTH = 3;
  private static final int EVENT_CREATION_MAX_ATTEMPTS = 256;

  @Inject EventMapper eventMapper;
  @Inject EventAttendeeMapper eventAttendeeMapper;
  @Inject EventInvitationCodeMapper eventInvitationCodeMapper;
  @Inject EventUserDataMapper eventUserDataMapper;

  /**
   * Creates a new event with an invitation code.
   *
   * @param initiatorId the ID of the user creating the event
   * @param meta JSON metadata for the event
   * @param expiresAt when the event expires
   * @return the created event
   */
  public Event createEvent(Long initiatorId, String meta, LocalDateTime expiresAt) {
    for (int attempt = 1; attempt <= EVENT_CREATION_MAX_ATTEMPTS; attempt++) {
      try {
        return QuarkusTransaction.requiringNew()
            .call(() -> createEventInTransaction(initiatorId, meta, expiresAt));
      } catch (Exception e) {
        if (shouldRetryTransaction(e) && attempt < EVENT_CREATION_MAX_ATTEMPTS) {
          LOG.debugf("Retrying event creation due to transient issue (attempt %d)", attempt);
          continue;
        }
        throw wrapAsRuntimeException(e);
      }
    }
    throw new IllegalStateException("Unable to create event after serialization retries");
  }

  private Event createEventInTransaction(Long initiatorId, String meta, LocalDateTime expiresAt) {
    eventMapper.ensureSerializableIsolationLevel();

    Event event = new Event();
    event.setInitiatorId(initiatorId);
    event.setStatus(EventStatus.CREATED);
    event.setMeta(meta);
    event.setExpiresAt(expiresAt);
    LocalDateTime now = LocalDateTime.now();
    event.setCreatedAt(now);
    event.setUpdatedAt(now);

    eventMapper.insert(event);

    EventInvitationCode code = new EventInvitationCode();
    code.setEventId(event.getId());
    code.setInvitationCode(generateInvitationCode());
    code.setCreatedAt(now);
    code.setUpdatedAt(now);

    if (eventInvitationCodeMapper.insertIfInvitationCodeAvailable(code) != 1) {
      throw new InvitationCodeCollisionException();
    }

    return event;
  }

  /**
   * Finds an event by ID.
   *
   * @param eventId the event ID
   * @return an Optional containing the event if found
   */
  public Optional<Event> findById(Long eventId) {
    return eventMapper.findById(eventId);
  }

  /**
   * Finds all events created by a specific user.
   *
   * @param userId the initiator user ID
   * @return list of events
   */
  public List<Event> findByInitiatorId(Long userId) {
    return eventMapper.findByInitiatorId(userId);
  }

  /**
   * Gets the invitation code for an event.
   *
   * @param eventId the event ID
   * @return an Optional containing the invitation code if found
   */
  public Optional<String> getInvitationCode(Long eventId) {
    return eventInvitationCodeMapper.findByEventId(eventId).stream()
        .findFirst()
        .map(EventInvitationCode::getInvitationCode);
  }

  /**
   * Finds an active event by invitation code.
   *
   * @param invitationCode the invitation code
   * @return an Optional containing the event if found and active
   */
  public Optional<Event> findActiveEventByInvitationCode(String invitationCode) {
    Optional<EventInvitationCode> code =
        eventInvitationCodeMapper.findByInvitationCode(invitationCode);
    if (code.isEmpty()) {
      return Optional.empty();
    }

    Optional<Event> event = eventMapper.findById(code.get().getEventId());
    if (event.isEmpty()) {
      return Optional.empty();
    }

    Event e = event.get();
    if (e.getStatus() == EventStatus.DELETED || e.getStatus() == EventStatus.EXPIRED) {
      return Optional.empty();
    }

    if (e.getExpiresAt() != null && !e.getExpiresAt().isAfter(LocalDateTime.now())) {
      return Optional.empty();
    }

    return event;
  }

  /**
   * Adds an attendee to an event.
   *
   * @param eventId the event ID
   * @param userId the user ID
   * @param meta optional JSON metadata
   * @return the created attendee record
   */
  @Transactional
  public EventAttendee addAttendee(Long eventId, Long userId, String meta) {
    EventAttendee attendee = new EventAttendee();
    attendee.setEventId(eventId);
    attendee.setAttendeeUserId(userId);
    attendee.setMeta(meta);
    LocalDateTime now = LocalDateTime.now();
    attendee.setCreatedAt(now);
    attendee.setUpdatedAt(now);

    eventAttendeeMapper.insert(attendee);
    return attendee;
  }

  /**
   * Checks if a user is already an attendee of an event.
   *
   * @param eventId the event ID
   * @param userId the user ID
   * @return true if the user is already an attendee
   */
  public boolean isUserAttendee(Long eventId, Long userId) {
    return eventAttendeeMapper.findByEventAndAttendee(eventId, userId).isPresent();
  }

  /**
   * Lists all attendees for an event.
   *
   * @param eventId the event ID
   * @return list of attendees
   */
  public List<EventAttendee> listAttendees(Long eventId) {
    return eventAttendeeMapper.findByEventId(eventId);
  }

  /**
   * Converts an OffsetDateTime to LocalDateTime for database storage.
   *
   * @param value the value to convert
   * @return LocalDateTime or null
   */
  public LocalDateTime toLocalDateTime(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof OffsetDateTime offsetDateTime) {
      return offsetDateTime.toLocalDateTime();
    }
    if (value instanceof String text && !text.isBlank()) {
      try {
        return OffsetDateTime.parse(text).toLocalDateTime();
      } catch (Exception e) {
        LOG.warnf("Failed to parse datetime string: %s - %s", text, e.getMessage());
        return null;
      }
    }
    return null;
  }

  // ============================================================================
  // Event User Data Operations
  // ============================================================================

  /**
   * Finds the latest user data for an event and user.
   *
   * @param eventId the event ID
   * @param userId the user ID
   * @return an Optional containing the user data if found
   */
  public Optional<EventUserData> findLatestUserData(Long eventId, Long userId) {
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
  public EventUserData createUserDataRevision(
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

  /**
   * Generates a random invitation code.
   *
   * <p>Uniqueness is enforced by {@link #persistUniqueInvitationCode(Long, LocalDateTime)} which
   * retries database writes when collisions occur.
   */
  private String generateInvitationCode() {
    StringBuilder code = new StringBuilder(INVITATION_CODE_LENGTH);
    SecureRandom rnd = new SecureRandom();
    for (int i = 0; i < INVITATION_CODE_LENGTH; i++) {
      code.append(INVITATION_CODE_CHARS.charAt(rnd.nextInt(INVITATION_CODE_CHARS.length())));
    }
    return code.toString();
  }

  private boolean shouldRetryTransaction(Exception exception) {
    return isSerializationFailure(exception) || isInvitationCodeCollision(exception);
  }

  private boolean isSerializationFailure(Throwable throwable) {
    Throwable current = throwable;
    while (current != null) {
      if (current instanceof SQLException sqlException) {
        String sqlState = sqlException.getSQLState();
        if ("40001".equals(sqlState)) {
          return true;
        }
      }
      current = current.getCause();
    }
    return false;
  }

  private boolean isInvitationCodeCollision(Throwable throwable) {
    Throwable current = throwable;
    while (current != null) {
      if (current instanceof InvitationCodeCollisionException) {
        return true;
      }
      current = current.getCause();
    }
    return false;
  }

  private RuntimeException wrapAsRuntimeException(Exception exception) {
    if (exception instanceof RuntimeException runtimeException) {
      return runtimeException;
    }
    return new RuntimeException("Failed to create event", exception);
  }

  private static final class InvitationCodeCollisionException extends RuntimeException {
    private static final long serialVersionUID = 1L;
  }
}
