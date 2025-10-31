package app.aoki.resource;

import app.aoki.entity.Room;
import app.aoki.entity.User;
import app.aoki.generated.api.RoomsApi;
import app.aoki.generated.model.CreateRoomRequest;
import app.aoki.generated.model.ErrorResponse;
import app.aoki.generated.model.RoomResponse;
import app.aoki.generated.model.UpdateRoomRequest;
import app.aoki.service.RoomService;
import app.aoki.service.UserService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@ApplicationScoped
@Path("/api/rooms")
public class RoomsApiImpl implements RoomsApi {

  @Inject RoomService roomService;
  @Inject UserService userService;

  @Override
  public Response createRoom(CreateRoomRequest createRoomRequest, String guestToken) {
    if (guestToken == null || guestToken.isEmpty()) {
      return Response.status(Response.Status.UNAUTHORIZED)
          .entity(new ErrorResponse().error("Authentication required"))
          .build();
    }

    Optional<User> user = userService.findByGuestToken(guestToken);
    if (user.isEmpty()) {
      return Response.status(Response.Status.UNAUTHORIZED)
          .entity(new ErrorResponse().error("Invalid authentication"))
          .build();
    }

    Room room =
        roomService.createRoom(
            createRoomRequest.getName(), createRoomRequest.getDescription(), user.get().getId());
    return Response.status(Response.Status.CREATED).entity(toRoomResponse(room)).build();
  }

  @Override
  public Response deleteRoom(Long id, String guestToken) {
    if (guestToken == null || guestToken.isEmpty()) {
      return Response.status(Response.Status.UNAUTHORIZED)
          .entity(new ErrorResponse().error("Authentication required"))
          .build();
    }

    Optional<User> user = userService.findByGuestToken(guestToken);
    if (user.isEmpty()) {
      return Response.status(Response.Status.UNAUTHORIZED)
          .entity(new ErrorResponse().error("Invalid authentication"))
          .build();
    }

    Optional<Room> existingRoom = roomService.findById(id);
    if (existingRoom.isEmpty()) {
      return Response.status(Response.Status.NOT_FOUND)
          .entity(new ErrorResponse().error("Room not found"))
          .build();
    }

    Room room = existingRoom.get();
    if (!room.getUserId().equals(user.get().getId())) {
      return Response.status(Response.Status.FORBIDDEN)
          .entity(new ErrorResponse().error("You don't have permission to delete this room"))
          .build();
    }

    roomService.deleteRoom(id);
    return Response.noContent().build();
  }

  @Override
  public Response getAllRooms() {
    List<RoomResponse> rooms =
        roomService.findAll().stream().map(this::toRoomResponse).collect(Collectors.toList());
    return Response.ok(rooms).build();
  }

  @Override
  public Response getMyRooms(String guestToken) {
    if (guestToken == null || guestToken.isEmpty()) {
      return Response.status(Response.Status.UNAUTHORIZED)
          .entity(new ErrorResponse().error("Authentication required"))
          .build();
    }

    Optional<User> user = userService.findByGuestToken(guestToken);
    if (user.isEmpty()) {
      return Response.status(Response.Status.UNAUTHORIZED)
          .entity(new ErrorResponse().error("Invalid authentication"))
          .build();
    }

    List<RoomResponse> rooms =
        roomService.findByUserId(user.get().getId()).stream()
            .map(this::toRoomResponse)
            .collect(Collectors.toList());
    return Response.ok(rooms).build();
  }

  @Override
  public Response getRoomById(Long id) {
    Optional<Room> room = roomService.findById(id);
    if (room.isEmpty()) {
      return Response.status(Response.Status.NOT_FOUND)
          .entity(new ErrorResponse().error("Room not found"))
          .build();
    }
    return Response.ok(toRoomResponse(room.get())).build();
  }

  @Override
  public Response updateRoom(Long id, UpdateRoomRequest updateRoomRequest, String guestToken) {
    if (guestToken == null || guestToken.isEmpty()) {
      return Response.status(Response.Status.UNAUTHORIZED)
          .entity(new ErrorResponse().error("Authentication required"))
          .build();
    }

    Optional<User> user = userService.findByGuestToken(guestToken);
    if (user.isEmpty()) {
      return Response.status(Response.Status.UNAUTHORIZED)
          .entity(new ErrorResponse().error("Invalid authentication"))
          .build();
    }

    Optional<Room> existingRoom = roomService.findById(id);
    if (existingRoom.isEmpty()) {
      return Response.status(Response.Status.NOT_FOUND)
          .entity(new ErrorResponse().error("Room not found"))
          .build();
    }

    Room room = existingRoom.get();
    if (!room.getUserId().equals(user.get().getId())) {
      return Response.status(Response.Status.FORBIDDEN)
          .entity(new ErrorResponse().error("You don't have permission to update this room"))
          .build();
    }

    room.setName(updateRoomRequest.getName());
    room.setDescription(updateRoomRequest.getDescription());
    roomService.updateRoom(room);

    return Response.ok(toRoomResponse(room)).build();
  }

  private RoomResponse toRoomResponse(Room room) {
    return new RoomResponse()
        .id(room.getId())
        .name(room.getName())
        .description(room.getDescription())
        .userId(room.getUserId())
        .createdAt(room.getCreatedAt().atOffset(ZoneOffset.UTC));
  }
}
