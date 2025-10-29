package app.aoki.resource;

import app.aoki.dto.RoomRequest;
import app.aoki.dto.RoomResponse;
import app.aoki.entity.Room;
import app.aoki.entity.User;
import app.aoki.service.RoomService;
import app.aoki.service.UserService;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Path("/api/rooms")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class RoomResource {

    private static final String GUEST_TOKEN_COOKIE = "guest_token";

    @Inject
    RoomService roomService;

    @Inject
    UserService userService;

    @POST
    public Response createRoom(RoomRequest request, @CookieParam(GUEST_TOKEN_COOKIE) String guestToken) {
        if (guestToken == null || guestToken.isEmpty()) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("{\"error\": \"Authentication required\"}")
                    .build();
        }

        Optional<User> user = userService.findByGuestToken(guestToken);
        if (user.isEmpty()) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("{\"error\": \"Invalid authentication\"}")
                    .build();
        }

        Room room = roomService.createRoom(request.getName(), request.getDescription(), user.get().getId());
        return Response.status(Response.Status.CREATED)
                .entity(RoomResponse.from(room))
                .build();
    }

    @GET
    public Response getAllRooms() {
        List<RoomResponse> rooms = roomService.findAll().stream()
                .map(RoomResponse::from)
                .collect(Collectors.toList());
        return Response.ok(rooms).build();
    }

    @GET
    @Path("/{id}")
    public Response getRoomById(@PathParam("id") Long id) {
        Optional<Room> room = roomService.findById(id);
        if (room.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\": \"Room not found\"}")
                    .build();
        }
        return Response.ok(RoomResponse.from(room.get())).build();
    }

    @GET
    @Path("/my")
    public Response getMyRooms(@CookieParam(GUEST_TOKEN_COOKIE) String guestToken) {
        if (guestToken == null || guestToken.isEmpty()) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("{\"error\": \"Authentication required\"}")
                    .build();
        }

        Optional<User> user = userService.findByGuestToken(guestToken);
        if (user.isEmpty()) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("{\"error\": \"Invalid authentication\"}")
                    .build();
        }

        List<RoomResponse> rooms = roomService.findByUserId(user.get().getId()).stream()
                .map(RoomResponse::from)
                .collect(Collectors.toList());
        return Response.ok(rooms).build();
    }

    @PUT
    @Path("/{id}")
    public Response updateRoom(@PathParam("id") Long id, RoomRequest request, 
                               @CookieParam(GUEST_TOKEN_COOKIE) String guestToken) {
        if (guestToken == null || guestToken.isEmpty()) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("{\"error\": \"Authentication required\"}")
                    .build();
        }

        Optional<User> user = userService.findByGuestToken(guestToken);
        if (user.isEmpty()) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("{\"error\": \"Invalid authentication\"}")
                    .build();
        }

        Optional<Room> existingRoom = roomService.findById(id);
        if (existingRoom.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\": \"Room not found\"}")
                    .build();
        }

        Room room = existingRoom.get();
        if (!room.getUserId().equals(user.get().getId())) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity("{\"error\": \"You don't have permission to update this room\"}")
                    .build();
        }

        room.setName(request.getName());
        room.setDescription(request.getDescription());
        roomService.updateRoom(room);

        return Response.ok(RoomResponse.from(room)).build();
    }

    @DELETE
    @Path("/{id}")
    public Response deleteRoom(@PathParam("id") Long id, @CookieParam(GUEST_TOKEN_COOKIE) String guestToken) {
        if (guestToken == null || guestToken.isEmpty()) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("{\"error\": \"Authentication required\"}")
                    .build();
        }

        Optional<User> user = userService.findByGuestToken(guestToken);
        if (user.isEmpty()) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("{\"error\": \"Invalid authentication\"}")
                    .build();
        }

        Optional<Room> existingRoom = roomService.findById(id);
        if (existingRoom.isEmpty()) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity("{\"error\": \"Room not found\"}")
                    .build();
        }

        Room room = existingRoom.get();
        if (!room.getUserId().equals(user.get().getId())) {
            return Response.status(Response.Status.FORBIDDEN)
                    .entity("{\"error\": \"You don't have permission to delete this room\"}")
                    .build();
        }

        roomService.deleteRoom(id);
        return Response.noContent().build();
    }
}
