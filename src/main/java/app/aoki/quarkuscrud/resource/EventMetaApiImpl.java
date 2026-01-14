package app.aoki.quarkuscrud.resource;

import app.aoki.quarkuscrud.entity.Event;
import app.aoki.quarkuscrud.generated.model.MetaData;
import app.aoki.quarkuscrud.generated.model.MetaDataUpdateRequest;
import app.aoki.quarkuscrud.service.EventService;
import app.aoki.quarkuscrud.util.JwtUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import java.time.LocalDateTime;
import org.jboss.logging.Logger;

@RequestScoped
public class EventMetaApiImpl {

  private static final Logger LOG = Logger.getLogger(EventMetaApiImpl.class);

  @Inject EventService eventService;
  @Inject ObjectMapper objectMapper;

  public Response getEventMeta(Long eventId, SecurityContext securityContext) {
    LOG.infof("Getting metadata for event %d", eventId);

    Long requestingUserId = JwtUtil.extractUserId(securityContext);
    if (requestingUserId == null) {
      return Response.status(Response.Status.UNAUTHORIZED).entity("Authentication required").build();
    }

    Event event = eventService.getEventById(eventId);
    if (event == null) {
      return Response.status(Response.Status.NOT_FOUND).entity("Event not found").build();
    }

    // Check if user is initiator or attendee
    if (!event.getInitiatorId().equals(requestingUserId)
        && !eventService.isUserAttendee(eventId, requestingUserId)) {
      return Response.status(Response.Status.FORBIDDEN)
          .entity("You must be the initiator or an attendee to access event metadata")
          .build();
    }

    MetaData metaData = new MetaData();
    try {
      if (event.getUsermeta() != null && !event.getUsermeta().isBlank()) {
        metaData.setUsermeta(objectMapper.readValue(event.getUsermeta(), Object.class));
      } else {
        metaData.setUsermeta(null);
      }
    } catch (Exception e) {
      LOG.errorf("Failed to parse usermeta for event %d: %s", eventId, e.getMessage());
      metaData.setUsermeta(null);
    }

    return Response.ok(metaData).build();
  }

  public Response updateEventMeta(
      Long eventId, MetaDataUpdateRequest request, SecurityContext securityContext) {
    LOG.infof("Updating metadata for event %d", eventId);

    Long requestingUserId = JwtUtil.extractUserId(securityContext);
    if (requestingUserId == null) {
      return Response.status(Response.Status.UNAUTHORIZED).entity("Authentication required").build();
    }

    Event event = eventService.getEventById(eventId);
    if (event == null) {
      return Response.status(Response.Status.NOT_FOUND).entity("Event not found").build();
    }

    // Only initiator can update event metadata
    if (!event.getInitiatorId().equals(requestingUserId)) {
      return Response.status(Response.Status.FORBIDDEN)
          .entity("Only the event initiator can update event metadata")
          .build();
    }

    try {
      String usermetaJson =
          request.getUsermeta() != null
              ? objectMapper.writeValueAsString(request.getUsermeta())
              : null;
      event.setUsermeta(usermetaJson);
      event.setUpdatedAt(LocalDateTime.now());
      eventService.updateEvent(event);

      MetaData metaData = new MetaData();
      metaData.setUsermeta(request.getUsermeta());
      return Response.ok(metaData).build();
    } catch (Exception e) {
      LOG.errorf("Failed to update usermeta for event %d: %s", eventId, e.getMessage());
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity("Failed to update metadata")
          .build();
    }
  }
}
