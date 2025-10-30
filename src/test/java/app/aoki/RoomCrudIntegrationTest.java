package app.aoki;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;

/**
 * Integration tests for Room CRUD operations. Tests create, read, update, and delete functionality
 * for rooms.
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RoomCrudIntegrationTest {

  private static String guestToken;
  private static Long roomId;

  @BeforeAll
  public static void setup() {
    // Create a guest user for testing
    Response response = given().contentType(ContentType.JSON).post("/api/auth/guest");
    guestToken = response.getCookie("guest_token");
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
        .statusCode(401)
        .body("error", equalTo("Authentication required"));
  }

  @Test
  @Order(2)
  public void testCreateRoomWithAuthentication() {
    Response response =
        given()
            .cookie("guest_token", guestToken)
            .contentType(ContentType.JSON)
            .body("{\"name\":\"Test Room\",\"description\":\"Test Description\"}")
            .when()
            .post("/api/rooms")
            .then()
            .statusCode(anyOf(is(200), is(201)))
            .body("id", notNullValue())
            .body("name", equalTo("Test Room"))
            .body("description", equalTo("Test Description"))
            .body("userId", notNullValue())
            .body("createdAt", notNullValue())
            .extract()
            .response();

    roomId = response.jsonPath().getLong("id");
    assertNotNull(roomId, "Room ID should not be null");
  }

  @Test
  @Order(3)
  public void testGetRoomById() {
    given()
        .when()
        .get("/api/rooms/" + roomId)
        .then()
        .statusCode(200)
        .body("id", equalTo(roomId.intValue()))
        .body("name", equalTo("Test Room"))
        .body("description", equalTo("Test Description"));
  }

  @Test
  @Order(4)
  public void testGetNonExistentRoom() {
    given()
        .when()
        .get("/api/rooms/999999")
        .then()
        .statusCode(404)
        .body("error", equalTo("Room not found"));
  }

  @Test
  @Order(5)
  public void testGetAllRooms() {
    given()
        .when()
        .get("/api/rooms")
        .then()
        .statusCode(200)
        .body("$", hasSize(greaterThanOrEqualTo(1)))
        .body("[0].id", notNullValue())
        .body("[0].name", notNullValue());
  }

  @Test
  @Order(6)
  public void testGetMyRooms() {
    given()
        .cookie("guest_token", guestToken)
        .when()
        .get("/api/rooms/my")
        .then()
        .statusCode(200)
        .body("$", hasSize(greaterThanOrEqualTo(1)));
  }

  @Test
  @Order(7)
  public void testGetMyRoomsWithoutAuthentication() {
    given()
        .when()
        .get("/api/rooms/my")
        .then()
        .statusCode(401)
        .body("error", equalTo("Authentication required"));
  }

  @Test
  @Order(8)
  public void testUpdateRoom() {
    given()
        .cookie("guest_token", guestToken)
        .contentType(ContentType.JSON)
        .body("{\"name\":\"Updated Room\",\"description\":\"Updated Description\"}")
        .when()
        .put("/api/rooms/" + roomId)
        .then()
        .statusCode(200)
        .body("id", equalTo(roomId.intValue()))
        .body("name", equalTo("Updated Room"))
        .body("description", equalTo("Updated Description"));
  }

  @Test
  @Order(9)
  public void testUpdateRoomWithoutAuthentication() {
    given()
        .contentType(ContentType.JSON)
        .body("{\"name\":\"Hacked Room\",\"description\":\"Hacked Description\"}")
        .when()
        .put("/api/rooms/" + roomId)
        .then()
        .statusCode(401)
        .body("error", equalTo("Authentication required"));
  }

  @Test
  @Order(10)
  public void testUpdateNonExistentRoom() {
    given()
        .cookie("guest_token", guestToken)
        .contentType(ContentType.JSON)
        .body("{\"name\":\"Ghost Room\",\"description\":\"Ghost Description\"}")
        .when()
        .put("/api/rooms/999999")
        .then()
        .statusCode(404)
        .body("error", equalTo("Room not found"));
  }

  @Test
  @Order(11)
  public void testDeleteRoomWithoutAuthentication() {
    given()
        .when()
        .delete("/api/rooms/" + roomId)
        .then()
        .statusCode(401)
        .body("error", equalTo("Authentication required"));
  }

  @Test
  @Order(12)
  public void testDeleteRoom() {
    given()
        .cookie("guest_token", guestToken)
        .when()
        .delete("/api/rooms/" + roomId)
        .then()
        .statusCode(204);

    // Verify room is deleted
    given().when().get("/api/rooms/" + roomId).then().statusCode(404);
  }

  @Test
  @Order(13)
  public void testDeleteNonExistentRoom() {
    given()
        .cookie("guest_token", guestToken)
        .when()
        .delete("/api/rooms/999999")
        .then()
        .statusCode(404)
        .body("error", equalTo("Room not found"));
  }
}
