package app.aoki;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.TestInstance;

/**
 * Integration tests for Event CRUD operations. Tests create, read, and list functionality for
 * events. Equivalent to RoomCrudIntegrationTest in the original codebase.
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)

public class EventsCrudIntegrationTest {

  private static String jwtToken;
  private static Long eventId;
  private static String invitationCode;

  @Test
  @Order(0)
  public void setup() {
    // Create a guest user for testing
    Response response = given().contentType(ContentType.JSON).post("/api/auth/guest");
    jwtToken = response.getHeader("Authorization").substring(7);
    assertNotNull(jwtToken, "JWT token should not be null");
  }

  @Test
  @Order(1)
  public void testCreateEventWithoutAuthentication() {
    given()
        .contentType(ContentType.JSON)
        .body("{\"meta\":{\"name\":\"Test Event\"},\"expiresAt\":\"2025-12-31T23:59:59Z\"}")
        .when()
        .post("/api/events")
        .then()
        .statusCode(401)
        .body("error", equalTo("Authentication required"));
  }

  @Test
  @Order(2)
  public void testCreateEventWithAuthentication() {
    Response response =
        given()
            .header("Authorization", "Bearer " + jwtToken)
            .contentType(ContentType.JSON)
            .body(
                "{\"meta\":{\"name\":\"Test Event\",\"description\":\"Test Description\"},\"expiresAt\":\"2025-12-31T23:59:59Z\"}")
            .when()
            .post("/api/events")
            .then()
            .statusCode(201)
            .body("id", notNullValue())
            .body("status", equalTo("active"))
            .body("invitationCode", notNullValue())
            .body("createdAt", notNullValue())
            .extract()
            .response();

    eventId = response.jsonPath().getLong("id");
    invitationCode = response.jsonPath().getString("invitationCode");
    assertNotNull(eventId, "Event ID should not be null");
    assertNotNull(invitationCode, "Invitation code should not be null");
  }

  @Test
  @Order(3)
  public void testGetEventById() {
    given()
        .header("Authorization", "Bearer " + jwtToken)
        .when()
        .get("/api/events/" + eventId)
        .then()
        .statusCode(200)
        .body("id", equalTo(eventId.intValue()))
        .body("status", equalTo("active"));
  }

  @Test
  @Order(4)
  public void testGetNonExistentEvent() {
    given()
        .header("Authorization", "Bearer " + jwtToken)
        .when()
        .get("/api/events/999999")
        .then()
        .statusCode(404)
        .body("error", equalTo("Event not found"));
  }

  @Test
  @Order(5)
  public void testListEventsByUser() {
    Long userId = given().header("Authorization", "Bearer " + jwtToken)
        .when()
        .get("/api/me")
        .then()
        .extract()
        .jsonPath()
        .getLong("id");

    given()
        .header("Authorization", "Bearer " + jwtToken)
        .when()
        .get("/api/users/" + userId + "/events")
        .then()
        .statusCode(200)
        .body("$", hasSize(greaterThanOrEqualTo(1)));
  }

  @Test
  @Order(6)
  public void testJoinEventByCode() {
    // Create another user
    Response user2Response = given().contentType(ContentType.JSON).post("/api/auth/guest");
    String user2Token = user2Response.getHeader("Authorization").substring(7);

    given()
        .header("Authorization", "Bearer " + user2Token)
        .contentType(ContentType.JSON)
        .body("{\"invitationCode\":\"" + invitationCode + "\"}")
        .when()
        .post("/api/events/join-by-code")
        .then()
        .statusCode(201)
        .body("eventId", equalTo(eventId.intValue()))
        .body("attendeeUserId", notNullValue());
  }

  @Test
  @Order(7)
  public void testJoinNonExistentEventByCode() {
    given()
        .header("Authorization", "Bearer " + jwtToken)
        .contentType(ContentType.JSON)
        .body("{\"invitationCode\":\"INVALID123\"}")
        .when()
        .post("/api/events/join-by-code")
        .then()
        .statusCode(404)
        .body("error", containsString("No active event"));
  }

  @Test
  @Order(8)
  public void testListEventAttendees() {
    given()
        .header("Authorization", "Bearer " + jwtToken)
        .when()
        .get("/api/events/" + eventId + "/attendees")
        .then()
        .statusCode(200)
        .body("$", hasSize(greaterThanOrEqualTo(1)));
  }

  @Test
  @Order(9)
  public void testCreateMultipleEvents() {
    // Create second event
    Response response2 =
        given()
            .header("Authorization", "Bearer " + jwtToken)
            .contentType(ContentType.JSON)
            .body("{\"meta\":{\"name\":\"Second Event\"},\"expiresAt\":\"2025-12-31T23:59:59Z\"}")
            .post("/api/events");

    Long eventId2 = response2.jsonPath().getLong("id");
    assertNotNull(eventId2);
    assertNotEquals(eventId, eventId2, "Event IDs should be unique");

    // Create third event
    Response response3 =
        given()
            .header("Authorization", "Bearer " + jwtToken)
            .contentType(ContentType.JSON)
            .body("{\"meta\":{\"name\":\"Third Event\"},\"expiresAt\":\"2025-12-31T23:59:59Z\"}")
            .post("/api/events");

    Long eventId3 = response3.jsonPath().getLong("id");
    assertNotNull(eventId3);

    // Verify all events are listed
    Long userId = given().header("Authorization", "Bearer " + jwtToken)
        .get("/api/me")
        .then()
        .extract()
        .jsonPath()
        .getLong("id");

    given()
        .header("Authorization", "Bearer " + jwtToken)
        .get("/api/users/" + userId + "/events")
        .then()
        .statusCode(200)
        .body("$", hasSize(greaterThanOrEqualTo(3)));
  }

  @Test
  @Order(10)
  public void testDuplicateJoinAttempt() {
    // Create a user
    Response userResponse = given().contentType(ContentType.JSON).post("/api/auth/guest");
    String userToken = userResponse.getHeader("Authorization").substring(7);

    // Join the event
    given()
        .header("Authorization", "Bearer " + userToken)
        .contentType(ContentType.JSON)
        .body("{\"invitationCode\":\"" + invitationCode + "\"}")
        .post("/api/events/join-by-code")
        .then()
        .statusCode(201);

    // Try to join again - should return 409 Conflict
    given()
        .header("Authorization", "Bearer " + userToken)
        .contentType(ContentType.JSON)
        .body("{\"invitationCode\":\"" + invitationCode + "\"}")
        .post("/api/events/join-by-code")
        .then()
        .statusCode(409)
        .body("error", containsString("already"));
  }
}
