package app.aoki.quarkuscrud.service;

import app.aoki.quarkuscrud.entity.Event;
import app.aoki.quarkuscrud.entity.EventAttendee;
import app.aoki.quarkuscrud.entity.EventInvitationCode;
import app.aoki.quarkuscrud.entity.EventStatus;
import app.aoki.quarkuscrud.mapper.EventAttendeeMapper;
import app.aoki.quarkuscrud.mapper.EventInvitationCodeMapper;
import app.aoki.quarkuscrud.mapper.EventMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.security.SecureRandom;
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

  @Inject EventMapper eventMapper;
  @Inject EventAttendeeMapper eventAttendeeMapper;
  @Inject EventInvitationCodeMapper eventInvitationCodeMapper;

  /**
   * Creates a new event with an invitation code.
   *
   * @param initiatorId the ID of the user creating the event
   * @param meta JSON metadata for the event
   * @param expiresAt when the event expires
   * @return the created event
   */
  @Transactional
  public Event createEvent(Long initiatorId, String meta, LocalDateTime expiresAt) {
    Event event = new Event();
    event.setInitiatorId(initiatorId);
    event.setStatus(EventStatus.CREATED);
    event.setMeta(meta);
    event.setExpiresAt(expiresAt);
    LocalDateTime now = LocalDateTime.now();
    event.setCreatedAt(now);
    event.setUpdatedAt(now);

    eventMapper.insert(event);

    // Generate and insert invitation code
    String invitationCode = generateInvitationCode();
    EventInvitationCode code = new EventInvitationCode();
    code.setEventId(event.getId());
    code.setInvitationCode(invitationCode);
    code.setCreatedAt(now);
    code.setUpdatedAt(now);
    eventInvitationCodeMapper.insert(code);

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

  /**
   * Generates a random invitation code.
   *
   * <p>Note: This generates a random code but does not check for uniqueness. In a production
   * environment, you should implement retry logic or database constraints to ensure uniqueness.
   *
   * @return a random invitation code
   */
  private String generateInvitationCode() {
    StringBuilder code = new StringBuilder(INVITATION_CODE_LENGTH);
    SecureRandom rnd = new SecureRandom();
    for (int i = 0; i < INVITATION_CODE_LENGTH; i++) {
      code.append(INVITATION_CODE_CHARS.charAt(rnd.nextInt(INVITATION_CODE_CHARS.length())));
    }
    return code.toString();
  }
}
