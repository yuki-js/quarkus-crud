package app.aoki.service;

import app.aoki.entity.Room;
import app.aoki.mapper.RoomMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class RoomService {

    @Inject
    RoomMapper roomMapper;

    @Inject
    RoomEventBroadcaster eventBroadcaster;

    @Transactional
    public Room createRoom(String name, String description, Long userId) {
        Room room = new Room();
        room.setName(name);
        room.setDescription(description);
        room.setUserId(userId);
        room.setCreatedAt(LocalDateTime.now());
        room.setUpdatedAt(LocalDateTime.now());
        roomMapper.insert(room);
        eventBroadcaster.broadcastRoomCreated(room);
        return room;
    }

    public Optional<Room> findById(Long id) {
        return roomMapper.findById(id);
    }

    public List<Room> findByUserId(Long userId) {
        return roomMapper.findByUserId(userId);
    }

    public List<Room> findAll() {
        return roomMapper.findAll();
    }

    @Transactional
    public void updateRoom(Room room) {
        room.setUpdatedAt(LocalDateTime.now());
        roomMapper.update(room);
        eventBroadcaster.broadcastRoomUpdated(room);
    }

    @Transactional
    public void deleteRoom(Long id) {
        Optional<Room> room = findById(id);
        roomMapper.deleteById(id);
        room.ifPresent(eventBroadcaster::broadcastRoomDeleted);
    }
}
