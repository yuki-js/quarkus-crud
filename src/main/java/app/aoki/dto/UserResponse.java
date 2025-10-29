package app.aoki.dto;

import app.aoki.entity.User;

import java.time.LocalDateTime;

public class UserResponse {
    private Long id;
    private String guestToken;
    private LocalDateTime createdAt;

    public UserResponse() {
    }

    public UserResponse(Long id, String guestToken, LocalDateTime createdAt) {
        this.id = id;
        this.guestToken = guestToken;
        this.createdAt = createdAt;
    }

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getGuestToken(), user.getCreatedAt());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getGuestToken() {
        return guestToken;
    }

    public void setGuestToken(String guestToken) {
        this.guestToken = guestToken;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
