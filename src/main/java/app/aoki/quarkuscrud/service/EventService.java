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
import java.util.List;
import java.util.Optional;

/**
 * Service for managing quiz events.
 *
 * <p>Handles event creation, status management, invitation codes, and attendee operations.
 */
@ApplicationScoped
public class EventService {

  private static final SecureRandom RANDOM = new SecureRandom();
  private static final String CODE_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
  private static final int CODE_LENGTH = 6;

  @Inject EventMapper eventMapper;
  @Inject EventAttendeeMapper eventAttendeeMapper;
  @Inject EventInvitationCodeMapper eventInvitationCodeMapper;

  /**
   * Create a new event.
   *
   * @param initiatorId the ID of the user creating the event
   * @param expiresAt when the event expires
   * @param meta optional metadata as JSON string
   * @return the created event
   */
  @Transactional
  public Event createEvent(Long initiatorId, LocalDateTime expiresAt, String meta) {
    Event event = new Event();
    event.setInitiatorId(initiatorId);
    event.setStatus(EventStatus.CREATED);
    event.setExpiresAt(expiresAt);
    event.setMeta(meta);
    event.setCreatedAt(LocalDateTime.now());
    event.setUpdatedAt(LocalDateTime.now());
    eventMapper.insert(event);

    // Generate and store invitation code
    String code = generateInvitationCode();
    EventInvitationCode invitationCode = new EventInvitationCode();
    invitationCode.setEventId(event.getId());
    invitationCode.setInvitationCode(code);
    invitationCode.setCreatedAt(LocalDateTime.now());
    invitationCode.setUpdatedAt(LocalDateTime.now());
    eventInvitationCodeMapper.insert(invitationCode);

    return event;
  }

  /**
   * Find an event by ID.
   *
   * @param eventId the event ID
   * @return the event, or empty if not found
   */
  public Optional<Event> findById(Long eventId) {
    return eventMapper.findById(eventId);
  }

  /**
   * Find events by initiator user ID.
   *
   * @param userId the initiator user ID
   * @return list of events
   */
  public List<Event> findByInitiatorId(Long userId) {
    return eventMapper.findByInitiatorId(userId);
  }

  /**
   * Find an event by invitation code.
   *
   * @param code the invitation code
   * @return the event, or empty if not found or expired
   */
  public Optional<Event> findByInvitationCode(String code) {
    return eventInvitationCodeMapper
        .findByInvitationCode(code)
        .flatMap(
            invCode -> {
              Optional<Event> event = eventMapper.findById(invCode.getEventId());
              // Only return active events
              return event.filter(
                  e -> e.getStatus() == EventStatus.ACTIVE || e.getStatus() == EventStatus.CREATED);
            });
  }

  /**
   * Add an attendee to an event.
   *
   * @param eventId the event ID
   * @param userId the user ID
   * @return the created attendee record
   */
  @Transactional
  public EventAttendee addAttendee(Long eventId, Long userId) {
    // Check if already attending
    Optional<EventAttendee> existing = eventAttendeeMapper.findByEventAndAttendee(eventId, userId);
    if (existing.isPresent()) {
      return existing.get();
    }

    EventAttendee attendee = new EventAttendee();
    attendee.setEventId(eventId);
    attendee.setAttendeeUserId(userId);
    attendee.setMeta(null);
    attendee.setCreatedAt(LocalDateTime.now());
    attendee.setUpdatedAt(LocalDateTime.now());
    eventAttendeeMapper.insert(attendee);
    return attendee;
  }

  /**
   * Get all attendees for an event.
   *
   * @param eventId the event ID
   * @return list of attendees
   */
  public List<EventAttendee> getAttendees(Long eventId) {
    return eventAttendeeMapper.findByEventId(eventId);
  }

  /**
   * Check if a user is attending an event.
   *
   * @param eventId the event ID
   * @param userId the user ID
   * @return true if the user is attending
   */
  public boolean isAttending(Long eventId, Long userId) {
    return eventAttendeeMapper.findByEventAndAttendee(eventId, userId).isPresent();
  }

  /**
   * Update event status.
   *
   * @param eventId the event ID
   * @param status the new status
   */
  @Transactional
  public void updateStatus(Long eventId, EventStatus status) {
    Optional<Event> eventOpt = eventMapper.findById(eventId);
    if (eventOpt.isPresent()) {
      Event event = eventOpt.get();
      event.setStatus(status);
      event.setUpdatedAt(LocalDateTime.now());
      eventMapper.update(event);
    }
  }

  /**
   * Get the invitation code for an event.
   *
   * @param eventId the event ID
   * @return the invitation code, or empty if not found
   */
  public Optional<String> getInvitationCode(Long eventId) {
    return eventInvitationCodeMapper
        .findByEventId(eventId)
        .map(EventInvitationCode::getInvitationCode);
  }

  private String generateInvitationCode() {
    StringBuilder code = new StringBuilder(CODE_LENGTH);
    for (int i = 0; i < CODE_LENGTH; i++) {
      code.append(CODE_ALPHABET.charAt(RANDOM.nextInt(CODE_ALPHABET.length())));
    }
    return code.toString();
  }
}
