package app.aoki.quarkuscrud.resource;

import app.aoki.quarkuscrud.entity.Room;
import app.aoki.quarkuscrud.entity.User;
import app.aoki.quarkuscrud.filter.Authenticated;
import app.aoki.quarkuscrud.filter.AuthenticatedUser;
import app.aoki.quarkuscrud.generated.api.RoomsApi;
import app.aoki.quarkuscrud.generated.model.CreateRoomRequest;
import app.aoki.quarkuscrud.generated.model.RoomResponse;
import app.aoki.quarkuscrud.generated.model.UpdateRoomRequest;
import app.aoki.quarkuscrud.service.RoomService;
import app.aoki.quarkuscrud.support.ErrorResponse;
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
  @Inject AuthenticatedUser authenticatedUser;

  @Override
  @Authenticated
  public Response createRoom(CreateRoomRequest createRoomRequest) {
    User user = authenticatedUser.get();
    Room room =
        roomService.createRoom(
            createRoomRequest.getName(), createRoomRequest.getDescription(), user.getId());
    return Response.status(Response.Status.CREATED).entity(toRoomResponse(room)).build();
  }

  @Override
  @Authenticated
  public Response deleteRoom(Long id) {
    User user = authenticatedUser.get();

    Optional<Room> existingRoom = roomService.findById(id);
    if (existingRoom.isEmpty()) {
      return Response.status(Response.Status.NOT_FOUND)
          .entity(new ErrorResponse("Room not found"))
          .build();
    }

    Room room = existingRoom.get();
    if (!room.getUserId().equals(user.getId())) {
      return Response.status(Response.Status.FORBIDDEN)
          .entity(new ErrorResponse("You don't have permission to delete this room"))
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
  @Authenticated
  public Response getMyRooms() {
    User user = authenticatedUser.get();
    List<RoomResponse> rooms =
        roomService.findByUserId(user.getId()).stream()
            .map(this::toRoomResponse)
            .collect(Collectors.toList());
    return Response.ok(rooms).build();
  }

  @Override
  public Response getRoomById(Long id) {
    Optional<Room> room = roomService.findById(id);
    if (room.isEmpty()) {
      return Response.status(Response.Status.NOT_FOUND)
          .entity(new ErrorResponse("Room not found"))
          .build();
    }
    return Response.ok(toRoomResponse(room.get())).build();
  }

  @Override
  @Authenticated
  public Response updateRoom(Long id, UpdateRoomRequest updateRoomRequest) {
    User user = authenticatedUser.get();

    Optional<Room> existingRoom = roomService.findById(id);
    if (existingRoom.isEmpty()) {
      return Response.status(Response.Status.NOT_FOUND)
          .entity(new ErrorResponse("Room not found"))
          .build();
    }

    Room room = existingRoom.get();
    if (!room.getUserId().equals(user.getId())) {
      return Response.status(Response.Status.FORBIDDEN)
          .entity(new ErrorResponse("You don't have permission to update this room"))
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
