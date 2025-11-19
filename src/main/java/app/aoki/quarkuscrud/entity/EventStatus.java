package app.aoki.quarkuscrud.entity;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * Event status enumeration.
 *
 * <p>Represents the various states of a quiz event.
 */
@RegisterForReflection
public enum EventStatus {
  /**
   * Event has been created.
   *
   * <p>Initial state when an event is first created.
   */
  CREATED("created"),

  /**
   * Event is active.
   *
   * <p>Event is currently active and accepting participants.
   */
  ACTIVE("active"),

  /**
   * Event has ended.
   *
   * <p>Event has been completed normally.
   */
  ENDED("ended"),

  /**
   * Event has expired.
   *
   * <p>Event has passed its expiration time.
   */
  EXPIRED("expired"),

  /**
   * Event has been deleted.
   *
   * <p>Event has been marked as deleted.
   */
  DELETED("deleted");

  private final String value;

  EventStatus(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  /**
   * Get EventStatus from string value.
   *
   * @param value the string value
   * @return the corresponding EventStatus
   * @throws IllegalArgumentException if value doesn't match any status
   */
  public static EventStatus fromValue(String value) {
    for (EventStatus status : values()) {
      if (status.value.equals(value)) {
        return status;
      }
    }
    throw new IllegalArgumentException("Unknown event status: " + value);
  }
}
