package app.aoki.quarkuscrud.usecase;

import app.aoki.quarkuscrud.entity.Event;
import app.aoki.quarkuscrud.entity.EventAttendee;
import app.aoki.quarkuscrud.entity.EventUserData;
import app.aoki.quarkuscrud.generated.model.EventCreateRequest;
import app.aoki.quarkuscrud.generated.model.EventJoinByCodeRequest;
import app.aoki.quarkuscrud.generated.model.EventUserDataUpdateRequest;
import app.aoki.quarkuscrud.service.EventService;
import app.aoki.quarkuscrud.service.EventUserDataService;
import app.aoki.quarkuscrud.service.UserService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.jboss.logging.Logger;

/**
 * Use case for event-related business flows.
 *
 * <p>This use case orchestrates complete business operations including validation, business logic,
 * and DTO mapping.
 */
@ApplicationScoped
public class EventUseCase {

  private static final Logger LOG = Logger.getLogger(EventUseCase.class);

  @Inject EventService eventService;
  @Inject EventUserDataService eventUserDataService;
  @Inject UserService userService;
  @Inject ObjectMapper objectMapper;

  /**
   * Creates a new event with the given request data.
   *
   * @param userId the ID of the user creating the event
   * @param request the event creation request
   * @return the created event as DTO
   * @throws Exception if event creation fails
   */
  @Transactional
  public app.aoki.quarkuscrud.generated.model.Event createEvent(
      Long userId, EventCreateRequest request) throws Exception {
    String meta = objectMapper.writeValueAsString(request.getMeta());
    Event event =
        eventService.createEvent(
            userId, meta, eventService.toLocalDateTime(request.getExpiresAt()));
    String invitationCode = eventService.getInvitationCode(event.getId()).orElse(null);
    return toEventDto(event, invitationCode);
  }

  /**
   * Gets an event by ID.
   *
   * @param eventId the event ID
   * @return an Optional containing the event DTO if found
   */
  public Optional<app.aoki.quarkuscrud.generated.model.Event> getEventById(Long eventId) {
    return eventService
        .findById(eventId)
        .map(
            event -> {
              String invitationCode = eventService.getInvitationCode(eventId).orElse(null);
              return toEventDto(event, invitationCode);
            });
  }

  /**
   * Joins an event by invitation code.
   *
   * @param userId the ID of the user joining
   * @param request the join request with invitation code
   * @return the created attendee as DTO
   * @throws IllegalArgumentException if invitation code is invalid
   * @throws IllegalStateException if user already joined
   * @throws Exception if join fails
   */
  @Transactional
  public app.aoki.quarkuscrud.generated.model.EventAttendee joinEventByCode(
      Long userId, EventJoinByCodeRequest request) throws Exception {
    String code = request.getInvitationCode();

    if (code == null || code.isBlank()) {
      throw new IllegalArgumentException("invitationCode is required to join an event");
    }

    Event event =
        eventService
            .findActiveEventByInvitationCode(code)
            .orElseThrow(
                () -> new IllegalArgumentException("No active event matches the invitation code"));

    if (eventService.isUserAttendee(event.getId(), userId)) {
      throw new IllegalStateException("User already joined the event");
    }

    EventAttendee attendee = eventService.addAttendee(event.getId(), userId, null);
    return toAttendeeDto(attendee);
  }

  /**
   * Lists all attendees for an event.
   *
   * @param eventId the event ID
   * @return list of attendee DTOs
   * @throws IllegalArgumentException if event not found
   */
  public List<app.aoki.quarkuscrud.generated.model.EventAttendee> listEventAttendees(Long eventId) {
    if (eventService.findById(eventId).isEmpty()) {
      throw new IllegalArgumentException("Event not found");
    }

    return eventService.listAttendees(eventId).stream()
        .map(this::toAttendeeDto)
        .collect(Collectors.toList());
  }

  /**
   * Lists all events for a user.
   *
   * @param userId the user ID
   * @return list of event DTOs
   * @throws IllegalArgumentException if user not found
   */
  public List<app.aoki.quarkuscrud.generated.model.Event> listEventsByUser(Long userId) {
    if (userService.findById(userId).isEmpty()) {
      throw new IllegalArgumentException("User not found");
    }

    return eventService.findByInitiatorId(userId).stream()
        .map(
            event -> {
              String invitationCode = eventService.getInvitationCode(event.getId()).orElse(null);
              return toEventDto(event, invitationCode);
            })
        .collect(Collectors.toList());
  }

  /**
   * Checks if an event exists.
   *
   * @param eventId the event ID
   * @return true if event exists
   */
  public boolean eventExists(Long eventId) {
    return eventService.findById(eventId).isPresent();
  }

  /**
   * Checks if a user is an attendee of an event.
   *
   * @param eventId the event ID
   * @param userId the user ID
   * @return true if user is an attendee
   */
  public boolean isUserAttendee(Long eventId, Long userId) {
    return eventService.isUserAttendee(eventId, userId);
  }

  /**
   * Gets the latest user data for an event and user.
   *
   * @param eventId the event ID
   * @param userId the user ID
   * @return an Optional containing the user data DTO if found
   */
  public Optional<app.aoki.quarkuscrud.generated.model.EventUserData> getEventUserData(
      Long eventId, Long userId) {
    return eventUserDataService
        .findLatestByEventIdAndUserId(eventId, userId)
        .map(this::toUserDataDto);
  }

  /**
   * Updates the user data for an event and user.
   *
   * @param eventId the event ID
   * @param userId the user ID
   * @param request the update request
   * @return the updated user data as DTO
   * @throws Exception if update fails
   */
  @Transactional
  public app.aoki.quarkuscrud.generated.model.EventUserData updateEventUserData(
      Long eventId, Long userId, EventUserDataUpdateRequest request) throws Exception {
    String userData = objectMapper.writeValueAsString(request.getUserData());
    String revisionMeta = null;
    if (request.getRevisionMeta() != null) {
      revisionMeta = objectMapper.writeValueAsString(request.getRevisionMeta());
    }
    EventUserData newData =
        eventUserDataService.createRevision(eventId, userId, userData, revisionMeta);
    return toUserDataDto(newData);
  }

  private app.aoki.quarkuscrud.generated.model.Event toEventDto(
      Event event, String invitationCode) {
    app.aoki.quarkuscrud.generated.model.Event response =
        new app.aoki.quarkuscrud.generated.model.Event();
    response.setId(event.getId());
    response.setInitiatorId(event.getInitiatorId());
    response.setStatus(
        app.aoki.quarkuscrud.generated.model.Event.StatusEnum.fromValue(
            event.getStatus().getValue()));
    if (invitationCode != null) {
      response.setInvitationCode(invitationCode);
    }
    if (event.getExpiresAt() != null) {
      response.setExpiresAt(event.getExpiresAt().atOffset(ZoneOffset.UTC));
    }
    response.setCreatedAt(event.getCreatedAt().atOffset(ZoneOffset.UTC));
    if (event.getUpdatedAt() != null) {
      response.setUpdatedAt(event.getUpdatedAt().atOffset(ZoneOffset.UTC));
    }

    if (event.getMeta() != null) {
      try {
        Map<String, Object> meta =
            objectMapper.readValue(event.getMeta(), new TypeReference<>() {});
        response.setMeta(meta);
      } catch (Exception e) {
        response.setMeta(new HashMap<>());
      }
    } else {
      response.setMeta(new HashMap<>());
    }

    return response;
  }

  private app.aoki.quarkuscrud.generated.model.EventAttendee toAttendeeDto(EventAttendee attendee) {
    app.aoki.quarkuscrud.generated.model.EventAttendee response =
        new app.aoki.quarkuscrud.generated.model.EventAttendee();
    response.setId(attendee.getId());
    response.setEventId(attendee.getEventId());
    response.setAttendeeUserId(attendee.getAttendeeUserId());
    response.setCreatedAt(attendee.getCreatedAt().atOffset(ZoneOffset.UTC));
    if (attendee.getUpdatedAt() != null) {
      response.setUpdatedAt(attendee.getUpdatedAt().atOffset(ZoneOffset.UTC));
    }

    if (attendee.getMeta() != null) {
      try {
        Map<String, Object> meta =
            objectMapper.readValue(attendee.getMeta(), new TypeReference<>() {});
        response.setMeta(meta);
      } catch (Exception e) {
        response.setMeta(new HashMap<>());
      }
    } else {
      response.setMeta(new HashMap<>());
    }

    return response;
  }

  private app.aoki.quarkuscrud.generated.model.EventUserData toUserDataDto(EventUserData data) {
    app.aoki.quarkuscrud.generated.model.EventUserData response =
        new app.aoki.quarkuscrud.generated.model.EventUserData();
    response.setId(data.getId());
    response.setEventId(data.getEventId());
    response.setUserId(data.getUserId());
    response.setCreatedAt(data.getCreatedAt().atOffset(ZoneOffset.UTC));
    if (data.getUpdatedAt() != null) {
      response.setUpdatedAt(data.getUpdatedAt().atOffset(ZoneOffset.UTC));
    }

    if (data.getUserData() != null) {
      try {
        Map<String, Object> userData =
            objectMapper.readValue(data.getUserData(), new TypeReference<>() {});
        response.setUserData(userData);
      } catch (Exception e) {
        LOG.warnf(
            e,
            "Failed to parse userData JSON for EventUserData id=%d, eventId=%d, userId=%d",
            data.getId(),
            data.getEventId(),
            data.getUserId());
        response.setUserData(new HashMap<>());
      }
    } else {
      response.setUserData(new HashMap<>());
    }

    if (data.getRevisionMeta() != null) {
      try {
        Map<String, Object> revisionMeta =
            objectMapper.readValue(data.getRevisionMeta(), new TypeReference<>() {});
        response.setRevisionMeta(revisionMeta);
      } catch (Exception e) {
        LOG.warnf(
            e,
            "Failed to parse revisionMeta JSON for EventUserData id=%d, eventId=%d, userId=%d",
            data.getId(),
            data.getEventId(),
            data.getUserId());
        response.setRevisionMeta(new HashMap<>());
      }
    } else {
      response.setRevisionMeta(new HashMap<>());
    }

    return response;
  }
}
