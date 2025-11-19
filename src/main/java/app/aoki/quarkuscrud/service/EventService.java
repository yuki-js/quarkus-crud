package app.aoki.quarkuscrud.service;

import app.aoki.quarkuscrud.entity.Event;
import app.aoki.quarkuscrud.entity.EventStatus;
import app.aoki.quarkuscrud.mapper.EventMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class EventService {

  @Inject EventMapper eventMapper;

  @Inject EventEventBroadcaster eventBroadcaster;

  @Transactional
  public Event createEvent(String name, String description, Long initiatorUserId) {
    Event event = new Event();
    event.setInitiatorUserId(initiatorUserId);
    event.setStatus(EventStatus.CREATED);
    // Store name and description in meta JSONB
    event.setMeta(
        String.format(
            "{\"name\":\"%s\",\"description\":\"%s\"}", escapeJson(name), escapeJson(description)));
    event.setExpiresAt(LocalDateTime.now().plusDays(7)); // Default 7 days expiration
    event.setCreatedAt(LocalDateTime.now());
    event.setUpdatedAt(LocalDateTime.now());
    eventMapper.insert(event);
    eventBroadcaster.broadcastEventCreated(event);
    return event;
  }

  public Optional<Event> findById(Long id) {
    return eventMapper.findById(id);
  }

  public List<Event> findByInitiatorUserId(Long initiatorUserId) {
    return eventMapper.findByInitiatorUserId(initiatorUserId);
  }

  public List<Event> findAll() {
    return eventMapper.findAll();
  }

  @Transactional
  public void updateEvent(Event event) {
    event.setUpdatedAt(LocalDateTime.now());
    eventMapper.update(event);
    eventBroadcaster.broadcastEventUpdated(event);
  }

  @Transactional
  public void deleteEvent(Long id) {
    // Fetch event before deletion to broadcast the event with details
    Optional<Event> event = eventMapper.findById(id);
    if (event.isPresent()) {
      Event e = event.get();
      e.setStatus(EventStatus.DELETED);
      e.setUpdatedAt(LocalDateTime.now());
      eventMapper.update(e);
      eventBroadcaster.broadcastEventDeleted(e);
    }
  }

  private String escapeJson(String value) {
    if (value == null) {
      return "";
    }
    return value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t");
  }
}
