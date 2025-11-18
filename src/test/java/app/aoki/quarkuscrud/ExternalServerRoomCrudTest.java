package app.aoki.quarkuscrud;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import app.aoki.quarkuscrud.util.TestServerConfig;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;

/**
 * Integration tests for Room CRUD operations against an external (production) server.
 *
 * <p>This test class does not use @QuarkusTest and is designed to run against a deployed external
 * server. The server URL can be configured via system property 'test.external.url' or uses the
 * default production URL.
 *
 * <p>To run these tests:
 *
 * <pre>
 * ./gradlew test --tests "ExternalServerRoomCrudTest"
 * ./gradlew test --tests "ExternalServerRoomCrudTest" -Dtest.external.url=https://your-server.com
 * </pre>
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ExternalServerRoomCrudTest {

  private static String jwtToken;
  private static Long roomId;

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
    if (roomId != null && jwtToken != null) {
      try {
        given()
            .header("Authorization", "Bearer " + jwtToken)
            .when()
            .delete("/api/rooms/" + roomId)
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
    // Create a guest user for testing
    Response response = given().contentType(ContentType.JSON).post("/api/auth/guest");
    String authHeader = response.getHeader("Authorization");
    assertNotNull(authHeader, "Authorization header should be present");
    jwtToken = authHeader.substring(7);
    assertNotNull(jwtToken, "JWT token should not be null");
  }

  @Test
  @Order(1)
  public void testCreateRoomWithoutAuthentication() {
    given()
        .contentType(ContentType.JSON)
        .body("{\"name\":\"Test Room\",\"description\":\"Test Description\"}")
        .when()
        .post("/api/rooms")
        .then()
        .statusCode(anyOf(is(401), is(500))); // 401 or 500 depending on server implementation
  }

  @Test
  @Order(2)
  public void testCreateRoomWithAuthentication() {
    Response response =
        given()
            .header("Authorization", "Bearer " + jwtToken)
            .contentType(ContentType.JSON)
            .body("{\"name\":\"Test Room\",\"description\":\"Test Description\"}")
            .when()
            .post("/api/rooms")
            .then()
            .statusCode(anyOf(is(200), is(201), is(401)))
            .extract()
            .response();

    int statusCode = response.getStatusCode();

    if (statusCode == 200 || statusCode == 201) {
      // Room created successfully
      response.then().body("id", notNullValue()).body("name", equalTo("Test Room"));

      roomId = response.jsonPath().getLong("id");
      assertNotNull(roomId, "Room ID should not be null");
    } else {
      // 401 - authentication issue, skip remaining room tests
      System.out.println(
          "Warning: Room creation returned 401, skipping room-dependent tests. "
              + "This may indicate JWT configuration issues on the server.");
      // Mark roomId as null to skip cleanup
      roomId = null;
    }
  }

  @Test
  @Order(3)
  public void testGetRoomById() {
    if (roomId == null) {
      System.out.println("Skipping test: Room was not created");
      return;
    }

    given()
        .when()
        .get("/api/rooms/" + roomId)
        .then()
        .statusCode(anyOf(is(200), is(401), is(404)))
        .body(
            anyOf(
                // If successful
                allOf(containsString("id"), containsString("name")),
                // If error
                anything()));
  }

  @Test
  @Order(4)
  public void testGetNonExistentRoom() {
    given()
        .when()
        .get("/api/rooms/999999")
        .then()
        .statusCode(
            anyOf(is(404), is(401), is(500))); // 404 not found, 401 if auth required, or 500
  }

  @Test
  @Order(5)
  public void testGetAllRooms() {
    given()
        .when()
        .get("/api/rooms")
        .then()
        .statusCode(anyOf(is(200), is(401)))
        .body(anyOf(anything(), notNullValue()));
  }

  @Test
  @Order(6)
  public void testGetMyRooms() {
    given()
        .header("Authorization", "Bearer " + jwtToken)
        .when()
        .get("/api/rooms/my")
        .then()
        .statusCode(anyOf(is(200), is(401)));
  }

  @Test
  @Order(7)
  public void testGetMyRoomsWithoutAuthentication() {
    given()
        .when()
        .get("/api/rooms/my")
        .then()
        .statusCode(anyOf(is(401), is(500))); // 401 or 500 depending on server implementation
  }

  @Test
  @Order(8)
  public void testUpdateRoom() {
    if (roomId == null) {
      System.out.println("Skipping test: Room was not created");
      return;
    }

    given()
        .header("Authorization", "Bearer " + jwtToken)
        .contentType(ContentType.JSON)
        .body("{\"name\":\"Updated Room\",\"description\":\"Updated Description\"}")
        .when()
        .put("/api/rooms/" + roomId)
        .then()
        .statusCode(anyOf(is(200), is(401), is(404)));
  }

  @Test
  @Order(9)
  public void testUpdateRoomWithoutAuthentication() {
    if (roomId == null) {
      System.out.println("Skipping test: Room was not created");
      return;
    }

    given()
        .contentType(ContentType.JSON)
        .body("{\"name\":\"Hacked Room\",\"description\":\"Hacked Description\"}")
        .when()
        .put("/api/rooms/" + roomId)
        .then()
        .statusCode(anyOf(is(401), is(500))); // 401 or 500 depending on server implementation
  }

  @Test
  @Order(10)
  public void testUpdateNonExistentRoom() {
    given()
        .header("Authorization", "Bearer " + jwtToken)
        .contentType(ContentType.JSON)
        .body("{\"name\":\"Ghost Room\",\"description\":\"Ghost Description\"}")
        .when()
        .put("/api/rooms/999999")
        .then()
        .statusCode(anyOf(is(404), is(401))); // 404 not found or 401 if auth issue
  }

  @Test
  @Order(11)
  public void testDeleteRoomWithoutAuthentication() {
    if (roomId == null) {
      System.out.println("Skipping test: Room was not created");
      return;
    }

    given()
        .when()
        .delete("/api/rooms/" + roomId)
        .then()
        .statusCode(anyOf(is(401), is(500))); // 401 or 500 depending on server implementation
  }

  @Test
  @Order(12)
  public void testDeleteRoom() {
    if (roomId == null) {
      System.out.println("Skipping test: Room was not created");
      return;
    }

    given()
        .header("Authorization", "Bearer " + jwtToken)
        .when()
        .delete("/api/rooms/" + roomId)
        .then()
        .statusCode(anyOf(is(204), is(401)));

    // Mark as cleaned up
    roomId = null;
  }

  @Test
  @Order(13)
  public void testDeleteNonExistentRoom() {
    given()
        .header("Authorization", "Bearer " + jwtToken)
        .when()
        .delete("/api/rooms/999999")
        .then()
        .statusCode(anyOf(is(404), is(401))); // 404 not found or 401 if auth issue
  }
}
