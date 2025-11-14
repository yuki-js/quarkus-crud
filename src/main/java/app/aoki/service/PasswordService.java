package app.aoki.service;

import jakarta.enterprise.context.ApplicationScoped;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

@ApplicationScoped
public class PasswordService {

  private static final int SALT_LENGTH = 16;
  private static final String HASH_ALGORITHM = "SHA-256";

  /**
   * Hash a password with a random salt.
   *
   * @param password the plain text password
   * @return the hashed password in format: salt:hash
   */
  public String hashPassword(String password) {
    try {
      // Generate random salt
      SecureRandom random = new SecureRandom();
      byte[] salt = new byte[SALT_LENGTH];
      random.nextBytes(salt);

      // Hash password with salt
      MessageDigest md = MessageDigest.getInstance(HASH_ALGORITHM);
      md.update(salt);
      byte[] hashedPassword = md.digest(password.getBytes(StandardCharsets.UTF_8));

      // Encode salt and hash as Base64
      String saltEncoded = Base64.getEncoder().encodeToString(salt);
      String hashEncoded = Base64.getEncoder().encodeToString(hashedPassword);

      return saltEncoded + ":" + hashEncoded;
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("Failed to hash password", e);
    }
  }

  /**
   * Verify a password against a stored hash.
   *
   * @param password the plain text password to verify
   * @param storedHash the stored hash in format: salt:hash
   * @return true if the password matches
   */
  public boolean verifyPassword(String password, String storedHash) {
    try {
      String[] parts = storedHash.split(":");
      if (parts.length != 2) {
        return false;
      }

      byte[] salt = Base64.getDecoder().decode(parts[0]);
      byte[] expectedHash = Base64.getDecoder().decode(parts[1]);

      // Hash the provided password with the same salt
      MessageDigest md = MessageDigest.getInstance(HASH_ALGORITHM);
      md.update(salt);
      byte[] actualHash = md.digest(password.getBytes(StandardCharsets.UTF_8));

      // Compare hashes
      return MessageDigest.isEqual(expectedHash, actualHash);
    } catch (Exception e) {
      return false;
    }
  }
}
