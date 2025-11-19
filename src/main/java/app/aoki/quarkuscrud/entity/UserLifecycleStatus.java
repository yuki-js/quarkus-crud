package app.aoki.quarkuscrud.entity;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * User account lifecycle status.
 *
 * <p>Represents the various stages of a user account's lifecycle from creation to deletion.
 */
@RegisterForReflection
public enum UserLifecycleStatus {
  /**
   * Account has been created.
   *
   * <p>Initial state when a user account is first created.
   */
  CREATED("created"),

  /**
   * Account provisioning completed.
   *
   * <p>All necessary records and external entities have been created, account is ready for login.
   */
  PROVISIONED("provisioned"),

  /**
   * Account is active.
   *
   * <p>User has completed initial login and account is fully activated.
   */
  ACTIVE("active"),

  /**
   * Account is paused.
   *
   * <p>Account has been temporarily suspended. Reason should be stored in meta field.
   */
  PAUSED("paused"),

  /**
   * Account is deleted.
   *
   * <p>Account has been marked as deleted.
   */
  DELETED("deleted");

  private final String value;

  UserLifecycleStatus(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  /**
   * Get UserLifecycleStatus from string value.
   *
   * @param value the string value
   * @return the corresponding UserLifecycleStatus
   * @throws IllegalArgumentException if value doesn't match any status
   */
  public static UserLifecycleStatus fromValue(String value) {
    for (UserLifecycleStatus status : values()) {
      if (status.value.equals(value)) {
        return status;
      }
    }
    throw new IllegalArgumentException("Unknown user lifecycle status: " + value);
  }
}
