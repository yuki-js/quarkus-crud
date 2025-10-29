package app.aoki.dto;

import app.aoki.entity.Room;

import java.time.LocalDateTime;

public class RoomResponse {
    private Long id;
    private String name;
    private String description;
    private Long userId;
    private LocalDateTime createdAt;

    public RoomResponse() {
    }

    public RoomResponse(Long id, String name, String description, Long userId, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.userId = userId;
        this.createdAt = createdAt;
    }

    public static RoomResponse from(Room room) {
        return new RoomResponse(room.getId(), room.getName(), room.getDescription(), 
                                room.getUserId(), room.getCreatedAt());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
