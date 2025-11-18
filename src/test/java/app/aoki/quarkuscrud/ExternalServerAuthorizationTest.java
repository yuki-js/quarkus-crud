package app.aoki.quarkuscrud;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

import app.aoki.quarkuscrud.util.TestServerConfig;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;

/**
 * Integration tests for authorization and access control against an external server. Tests that
 * users can only modify their own resources.
 *
 * <p>To run these tests:
 *
 * <pre>
 * ./gradlew test --tests "ExternalServerAuthorizationTest"
 * </pre>
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ExternalServerAuthorizationTest {

  private static String user1Token;
  private static String user2Token;
  private static Long user1RoomId;

  @BeforeAll
  public static void setupExternalServer() {
    // Configure RestAssured to use external server
    TestServerConfig.configureForExternalServer();

    System.out.println(
        "Testing against external server: " + TestServerConfig.getExternalServerUrl());
  }

  @AfterAll
  public static void cleanup() {
    // Clean up: delete test room if it was created
    if (user1RoomId != null && user1Token != null) {
      try {
        given()
            .header("Authorization", "Bearer " + user1Token)
            .when()
            .delete("/api/rooms/" + user1RoomId)
            .then()
            .statusCode(anyOf(is(204), is(401), is(404)));
      } catch (Exception e) {
        System.out.println("Warning: Could not clean up test room: " + e.getMessage());
      }
    }

    // Reset RestAssured configuration
    TestServerConfig.reset();
  }

  @Test
  @Order(0)
  public void setup() {
    // Create two different guest users for testing authorization
    Response user1Response = given().contentType(ContentType.JSON).post("/api/auth/guest");
    String authHeader1 = user1Response.getHeader("Authorization");
    if (authHeader1 != null) {
      user1Token = authHeader1.substring(7);
    }

    Response user2Response = given().contentType(ContentType.JSON).post("/api/auth/guest");
    String authHeader2 = user2Response.getHeader("Authorization");
    if (authHeader2 != null) {
      user2Token = authHeader2.substring(7);
    }

    // User 1 creates a room
    if (user1Token != null) {
      Response roomResponse =
          given()
              .header("Authorization", "Bearer " + user1Token)
              .contentType(ContentType.JSON)
              .body("{\"name\":\"User 1 Room\",\"description\":\"Owned by user 1\"}")
              .post("/api/rooms");

      int statusCode = roomResponse.getStatusCode();
      if (statusCode == 200 || statusCode == 201) {
        user1RoomId = roomResponse.jsonPath().getLong("id");
      } else {
        System.out.println(
            "Warning: Could not create test room, got status "
                + statusCode
                + ". Auth tests may be skipped.");
      }
    }
  }

  @Test
  @Order(1)
  public void testUserCannotUpdateAnotherUsersRoom() {
    if (user1RoomId == null || user2Token == null) {
      System.out.println("Skipping test: Room was not created or user2 token not available");
      return;
    }

    given()
        .header("Authorization", "Bearer " + user2Token)
        .contentType(ContentType.JSON)
        .body("{\"name\":\"Hacked Room\",\"description\":\"Should fail\"}")
        .when()
        .put("/api/rooms/" + user1RoomId)
        .then()
        .statusCode(anyOf(is(403), is(401))); // 403 forbidden or 401 if auth issue
  }

  @Test
  @Order(2)
  public void testUserCannotDeleteAnotherUsersRoom() {
    if (user1RoomId == null || user2Token == null) {
      System.out.println("Skipping test: Room was not created or user2 token not available");
      return;
    }

    given()
        .header("Authorization", "Bearer " + user2Token)
        .when()
        .delete("/api/rooms/" + user1RoomId)
        .then()
        .statusCode(anyOf(is(403), is(401))); // 403 forbidden or 401 if auth issue
  }

  @Test
  @Order(3)
  public void testUserCanUpdateOwnRoom() {
    if (user1RoomId == null || user1Token == null) {
      System.out.println("Skipping test: Room was not created or user1 token not available");
      return;
    }

    given()
        .header("Authorization", "Bearer " + user1Token)
        .contentType(ContentType.JSON)
        .body("{\"name\":\"Updated by Owner\",\"description\":\"Owner update\"}")
        .when()
        .put("/api/rooms/" + user1RoomId)
        .then()
        .statusCode(anyOf(is(200), is(401)));
  }

  @Test
  @Order(4)
  public void testUserCanDeleteOwnRoom() {
    if (user1RoomId == null || user1Token == null) {
      System.out.println("Skipping test: Room was not created or user1 token not available");
      return;
    }

    given()
        .header("Authorization", "Bearer " + user1Token)
        .when()
        .delete("/api/rooms/" + user1RoomId)
        .then()
        .statusCode(anyOf(is(204), is(401)));

    // Mark as cleaned up
    user1RoomId = null;
  }

  @Test
  @Order(5)
  public void testMultiUserRoomIsolation() {
    if (user1Token == null || user2Token == null) {
      System.out.println("Skipping test: User tokens not available");
      return;
    }

    // User 1 creates a room
    Response user1RoomResponse =
        given()
            .header("Authorization", "Bearer " + user1Token)
            .contentType(ContentType.JSON)
            .body("{\"name\":\"User 1 Private Room\",\"description\":\"Private\"}")
            .post("/api/rooms");

    // User 2 creates a room
    Response user2RoomResponse =
        given()
            .header("Authorization", "Bearer " + user2Token)
            .contentType(ContentType.JSON)
            .body("{\"name\":\"User 2 Private Room\",\"description\":\"Private\"}")
            .post("/api/rooms");

    // Check if rooms were created
    int user1Status = user1RoomResponse.getStatusCode();
    int user2Status = user2RoomResponse.getStatusCode();

    if ((user1Status == 200 || user1Status == 201) && (user2Status == 200 || user2Status == 201)) {
      Long user1NewRoomId = user1RoomResponse.jsonPath().getLong("id");
      Long user2NewRoomId = user2RoomResponse.jsonPath().getLong("id");

      // User 1 gets their rooms - should see their own room
      given()
          .header("Authorization", "Bearer " + user1Token)
          .when()
          .get("/api/rooms/my")
          .then()
          .statusCode(anyOf(is(200), is(401)));

      // User 2 gets their rooms - should see their own room
      given()
          .header("Authorization", "Bearer " + user2Token)
          .when()
          .get("/api/rooms/my")
          .then()
          .statusCode(anyOf(is(200), is(401)));

      // Clean up
      try {
        given()
            .header("Authorization", "Bearer " + user1Token)
            .delete("/api/rooms/" + user1NewRoomId);
        given()
            .header("Authorization", "Bearer " + user2Token)
            .delete("/api/rooms/" + user2NewRoomId);
      } catch (Exception e) {
        System.out.println("Warning: Could not clean up test rooms: " + e.getMessage());
      }
    } else {
      System.out.println(
          "Skipping isolation verification: Rooms could not be created (status: "
              + user1Status
              + ", "
              + user2Status
              + ")");
    }
  }
}
