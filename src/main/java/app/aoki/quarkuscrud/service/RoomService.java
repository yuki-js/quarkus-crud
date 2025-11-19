package app.aoki.quarkuscrud.service;

import app.aoki.quarkuscrud.entity.Event;
import app.aoki.quarkuscrud.entity.EventStatus;
import app.aoki.quarkuscrud.entity.Room;
import app.aoki.quarkuscrud.mapper.EventMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Room service that provides backward-compatible Room API while using Event entities internally.
 *
 * <p>This service maps between the Room concept (user-facing) and Event entities (database). Room
 * name and description are stored in Event.meta as JSON.
 */
@ApplicationScoped
public class RoomService {

  @Inject EventMapper eventMapper;

  @Inject RoomEventBroadcaster eventBroadcaster;

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Transactional
  public Room createRoom(String name, String description, Long userId) {
    Event event = new Event();
    event.setInitiatorId(userId);
    event.setStatus(EventStatus.CREATED);
    event.setMeta(createMetaJson(name, description));
    event.setExpiresAt(LocalDateTime.now().plusDays(30)); // Default 30 days expiration
    event.setCreatedAt(LocalDateTime.now());
    event.setUpdatedAt(LocalDateTime.now());
    eventMapper.insert(event);

    Room room = mapEventToRoom(event);
    eventBroadcaster.broadcastRoomCreated(room);
    return room;
  }

  public Optional<Room> findById(Long id) {
    return eventMapper.findById(id).map(this::mapEventToRoom);
  }

  public List<Room> findByUserId(Long userId) {
    return eventMapper.findByInitiatorId(userId).stream()
        .map(this::mapEventToRoom)
        .collect(Collectors.toList());
  }

  public List<Room> findAll() {
    return eventMapper.findAll().stream().map(this::mapEventToRoom).collect(Collectors.toList());
  }

  @Transactional
  public void updateRoom(Room room) {
    Optional<Event> eventOpt = eventMapper.findById(room.getId());
    if (eventOpt.isEmpty()) {
      throw new IllegalArgumentException("Event not found: " + room.getId());
    }

    Event event = eventOpt.get();
    event.setMeta(createMetaJson(room.getName(), room.getDescription()));
    event.setUpdatedAt(LocalDateTime.now());
    eventMapper.update(event);

    room.setUpdatedAt(event.getUpdatedAt());
    eventBroadcaster.broadcastRoomUpdated(room);
  }

  @Transactional
  public void deleteRoom(Long id) {
    // Fetch event before deletion to broadcast the event with room details
    Optional<Event> event = eventMapper.findById(id);
    if (event.isPresent()) {
      Room room = mapEventToRoom(event.get());
      eventMapper.deleteById(id);
      eventBroadcaster.broadcastRoomDeleted(room);
    } else {
      eventMapper.deleteById(id);
    }
  }

  /**
   * Maps an Event entity to a Room model.
   *
   * @param event the event entity
   * @return the room model
   */
  private Room mapEventToRoom(Event event) {
    Room room = new Room();
    room.setId(event.getId());
    room.setUserId(event.getInitiatorId());
    room.setCreatedAt(event.getCreatedAt());
    room.setUpdatedAt(event.getUpdatedAt());

    // Extract name and description from meta JSON
    try {
      if (event.getMeta() != null && !event.getMeta().isEmpty()) {
        JsonNode meta = objectMapper.readTree(event.getMeta());
        room.setName(meta.has("name") ? meta.get("name").asText() : "");
        room.setDescription(meta.has("description") ? meta.get("description").asText() : "");
      } else {
        room.setName("");
        room.setDescription("");
      }
    } catch (JsonProcessingException e) {
      // Fallback to empty strings if JSON parsing fails
      room.setName("");
      room.setDescription("");
    }

    return room;
  }

  /**
   * Creates a JSON string with name and description for Event.meta.
   *
   * @param name the room name
   * @param description the room description
   * @return JSON string
   */
  private String createMetaJson(String name, String description) {
    ObjectNode meta = objectMapper.createObjectNode();
    meta.put("name", name != null ? name : "");
    meta.put("description", description != null ? description : "");
    return meta.toString();
  }
}
