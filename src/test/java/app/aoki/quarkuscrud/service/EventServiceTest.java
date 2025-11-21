package app.aoki.quarkuscrud.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.aoki.quarkuscrud.entity.Event;
import app.aoki.quarkuscrud.entity.EventAttendee;
import app.aoki.quarkuscrud.entity.EventStatus;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Unit tests for EventService.
 *
 * <p>Tests event creation, retrieval, invitation codes, and attendee management.
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class EventServiceTest {

  @Inject EventService eventService;
  @Inject UserService userService;

  private static Long testUserId;
  private static Long testEventId;
  private static String testInvitationCode;

  @Test
  @Order(1)
  @Transactional
  public void testCreateEvent() {
    // Create a test user first
    testUserId = userService.createAnonymousUser().getId();

    // Create an event
    LocalDateTime expiresAt = LocalDateTime.now().plusHours(2);
    Event event = eventService.createEvent(testUserId, "{\"title\":\"Test Event\"}", expiresAt);

    assertNotNull(event);
    assertNotNull(event.getId());
    assertEquals(testUserId, event.getInitiatorId());
    assertEquals(EventStatus.CREATED, event.getStatus());
    assertNotNull(event.getMeta());
    assertEquals(expiresAt, event.getExpiresAt());
    assertNotNull(event.getCreatedAt());
    assertNotNull(event.getUpdatedAt());

    testEventId = event.getId();
  }

  @Test
  @Order(2)
  public void testGetInvitationCode() {
    Optional<String> invitationCode = eventService.getInvitationCode(testEventId);

    assertTrue(invitationCode.isPresent());
    assertEquals(8, invitationCode.get().length());
    testInvitationCode = invitationCode.get();
  }

  @Test
  @Order(3)
  public void testFindById() {
    Optional<Event> event = eventService.findById(testEventId);

    assertTrue(event.isPresent());
    assertEquals(testEventId, event.get().getId());
    assertEquals(testUserId, event.get().getInitiatorId());
  }

  @Test
  @Order(4)
  public void testFindByIdNotFound() {
    Optional<Event> event = eventService.findById(999999L);
    assertTrue(event.isEmpty());
  }

  @Test
  @Order(5)
  public void testFindActiveEventByInvitationCode() {
    Optional<Event> event = eventService.findActiveEventByInvitationCode(testInvitationCode);

    assertTrue(event.isPresent());
    assertEquals(testEventId, event.get().getId());
  }

  @Test
  @Order(6)
  public void testFindActiveEventByInvitationCodeNotFound() {
    Optional<Event> event = eventService.findActiveEventByInvitationCode("INVALID1");
    assertTrue(event.isEmpty());
  }

  @Test
  @Order(7)
  public void testFindByInitiatorId() {
    List<Event> events = eventService.findByInitiatorId(testUserId);

    assertFalse(events.isEmpty());
    assertTrue(events.stream().anyMatch(e -> e.getId().equals(testEventId)));
  }

  @Test
  @Order(8)
  @Transactional
  public void testAddAttendee() {
    // Create another user to be an attendee
    Long attendeeUserId = userService.createAnonymousUser().getId();

    EventAttendee attendee = eventService.addAttendee(testEventId, attendeeUserId, null);

    assertNotNull(attendee);
    assertNotNull(attendee.getId());
    assertEquals(testEventId, attendee.getEventId());
    assertEquals(attendeeUserId, attendee.getAttendeeUserId());
    assertNotNull(attendee.getCreatedAt());
    assertNotNull(attendee.getUpdatedAt());
  }

  @Test
  @Order(9)
  public void testIsUserAttendee() {
    // Create another user
    Long attendeeUserId = userService.createAnonymousUser().getId();

    // Initially, user is not an attendee
    assertFalse(eventService.isUserAttendee(testEventId, attendeeUserId));
  }

  @Test
  @Order(10)
  public void testListAttendees() {
    List<EventAttendee> attendees = eventService.listAttendees(testEventId);

    assertFalse(attendees.isEmpty());
    assertTrue(attendees.stream().anyMatch(a -> a.getEventId().equals(testEventId)));
  }

  @Test
  @Order(11)
  public void testToLocalDateTimeVariants() {
    // Test with OffsetDateTime
    OffsetDateTime odt = OffsetDateTime.now(ZoneOffset.UTC);
    LocalDateTime ldtFromOdt = eventService.toLocalDateTime(odt);
    assertNotNull(ldtFromOdt);
    assertEquals(odt.toLocalDateTime(), ldtFromOdt);

    // Test with String
    String dateTimeString = odt.toString();
    LocalDateTime ldtFromString = eventService.toLocalDateTime(dateTimeString);
    assertNotNull(ldtFromString);

    // Test with null
    LocalDateTime ldtFromNull = eventService.toLocalDateTime(null);
    assertNull(ldtFromNull);

    // Test with invalid string
    LocalDateTime ldtFromInvalid = eventService.toLocalDateTime("invalid-date");
    assertNull(ldtFromInvalid);
  }

  @Test
  @Order(12)
  @Transactional
  public void testCreateEventWithNullMeta() {
    Long userId = userService.createAnonymousUser().getId();
    Event event = eventService.createEvent(userId, null, null);

    assertNotNull(event);
    assertNotNull(event.getId());
  }

  @Test
  @Order(13)
  @Transactional
  public void testAddAttendeeWithMeta() {
    Long userId = userService.createAnonymousUser().getId();
    Event event = eventService.createEvent(userId, null, null);
    Long attendeeUserId = userService.createAnonymousUser().getId();

    String meta = "{\"role\":\"moderator\"}";
    EventAttendee attendee = eventService.addAttendee(event.getId(), attendeeUserId, meta);

    assertNotNull(attendee);
    assertEquals(meta, attendee.getMeta());
  }
}
