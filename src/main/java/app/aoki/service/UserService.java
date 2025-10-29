package app.aoki.service;

import app.aoki.entity.User;
import app.aoki.mapper.UserMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class UserService {

    @Inject
    UserMapper userMapper;

    @Transactional
    public User createGuestUser() {
        User user = new User();
        user.setGuestToken(UUID.randomUUID().toString());
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.insert(user);
        return user;
    }

    public Optional<User> findById(Long id) {
        return userMapper.findById(id);
    }

    public Optional<User> findByGuestToken(String guestToken) {
        return userMapper.findByGuestToken(guestToken);
    }

    @Transactional
    public void updateUser(User user) {
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.update(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        userMapper.deleteById(id);
    }
}
