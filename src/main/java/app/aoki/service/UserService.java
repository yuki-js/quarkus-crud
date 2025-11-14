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

  @Inject UserMapper userMapper;
  @Inject PasswordService passwordService;

  @Transactional
  public User createGuestUser() {
    User user = new User();
    user.setGuestToken(UUID.randomUUID().toString());
    user.setRoles("guest");
    user.setCreatedAt(LocalDateTime.now());
    user.setUpdatedAt(LocalDateTime.now());
    userMapper.insert(user);
    return user;
  }

  @Transactional
  public User registerUser(String username, String password) {
    // Check if username already exists
    Optional<User> existing = userMapper.findByUsername(username);
    if (existing.isPresent()) {
      throw new IllegalArgumentException("Username already exists");
    }

    User user = new User();
    user.setUsername(username);
    user.setPasswordHash(passwordService.hashPassword(password));
    user.setRoles("user");
    user.setCreatedAt(LocalDateTime.now());
    user.setUpdatedAt(LocalDateTime.now());
    userMapper.insert(user);
    return user;
  }

  public Optional<User> authenticateUser(String username, String password) {
    Optional<User> userOpt = userMapper.findByUsername(username);
    if (userOpt.isEmpty()) {
      return Optional.empty();
    }

    User user = userOpt.get();
    if (user.getPasswordHash() == null
        || !passwordService.verifyPassword(password, user.getPasswordHash())) {
      return Optional.empty();
    }

    return Optional.of(user);
  }

  public Optional<User> findById(Long id) {
    return userMapper.findById(id);
  }

  public Optional<User> findByGuestToken(String guestToken) {
    return userMapper.findByGuestToken(guestToken);
  }

  public Optional<User> findByUsername(String username) {
    return userMapper.findByUsername(username);
  }

  @Transactional
  public void updateUser(User user) {
    user.setUpdatedAt(LocalDateTime.now());
    userMapper.update(user);
  }

  /**
   * Deletes a user by ID. Note: All rooms owned by this user will be automatically deleted due to
   * the ON DELETE CASCADE constraint on the rooms.user_id foreign key.
   */
  @Transactional
  public void deleteUser(Long id) {
    userMapper.deleteById(id);
  }
}
