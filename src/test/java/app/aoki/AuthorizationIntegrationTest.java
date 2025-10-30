package app.aoki;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Integration tests for authorization and access control. Tests that users can only modify their
 * own resources.
 */
@QuarkusTest
@TestMethodOrder(OrderAnnotation.class)
public class AuthorizationIntegrationTest {

  private static String user1Token;
  private static String user2Token;
  private static Long user1RoomId;

  @Test
  @Order(0)
  public void setup() {
    // Create two different guest users for testing authorization
    Response user1Response = given().contentType(ContentType.JSON).post("/api/auth/guest");
    user1Token = user1Response.getCookie("guest_token");

    Response user2Response = given().contentType(ContentType.JSON).post("/api/auth/guest");
    user2Token = user2Response.getCookie("guest_token");

    // User 1 creates a room
    Response roomResponse =
        given()
            .cookie("guest_token", user1Token)
            .contentType(ContentType.JSON)
            .body("{\"name\":\"User 1 Room\",\"description\":\"Owned by user 1\"}")
            .post("/api/rooms");
    user1RoomId = roomResponse.jsonPath().getLong("id");
  }

  @Test
  @Order(1)
  public void testUserCannotUpdateAnotherUsersRoom() {
    given()
        .cookie("guest_token", user2Token)
        .contentType(ContentType.JSON)
        .body("{\"name\":\"Hacked Room\",\"description\":\"Should fail\"}")
        .when()
        .put("/api/rooms/" + user1RoomId)
        .then()
        .statusCode(403)
        .body("error", equalTo("You don't have permission to update this room"));
  }

  @Test
  @Order(2)
  public void testUserCannotDeleteAnotherUsersRoom() {
    given()
        .cookie("guest_token", user2Token)
        .when()
        .delete("/api/rooms/" + user1RoomId)
        .then()
        .statusCode(403)
        .body("error", equalTo("You don't have permission to delete this room"));
  }

  @Test
  @Order(3)
  public void testUserCanUpdateOwnRoom() {
    given()
        .cookie("guest_token", user1Token)
        .contentType(ContentType.JSON)
        .body("{\"name\":\"Updated by Owner\",\"description\":\"Owner update\"}")
        .when()
        .put("/api/rooms/" + user1RoomId)
        .then()
        .statusCode(200)
        .body("name", equalTo("Updated by Owner"));
  }

  @Test
  @Order(4)
  public void testUserCanDeleteOwnRoom() {
    given()
        .cookie("guest_token", user1Token)
        .when()
        .delete("/api/rooms/" + user1RoomId)
        .then()
        .statusCode(204);
  }

  @Test
  @Order(5)
  public void testMultiUserRoomIsolation() {
    // User 1 creates a room
    Response room1Response =
        given()
            .cookie("guest_token", user1Token)
            .contentType(ContentType.JSON)
            .body("{\"name\":\"User 1 Private Room\",\"description\":\"Private\"}")
            .post("/api/rooms");
    Long room1Id = room1Response.jsonPath().getLong("id");

    // User 2 creates a room
    Response room2Response =
        given()
            .cookie("guest_token", user2Token)
            .contentType(ContentType.JSON)
            .body("{\"name\":\"User 2 Private Room\",\"description\":\"Private\"}")
            .post("/api/rooms");
    Long room2Id = room2Response.jsonPath().getLong("id");

    // User 1 should only see their own room in /my
    given()
        .cookie("guest_token", user1Token)
        .when()
        .get("/api/rooms/my")
        .then()
        .statusCode(200)
        .body("findAll { it.id == " + room1Id + " }.size()", equalTo(1))
        .body("findAll { it.id == " + room2Id + " }.size()", equalTo(0));

    // User 2 should only see their own room in /my
    given()
        .cookie("guest_token", user2Token)
        .when()
        .get("/api/rooms/my")
        .then()
        .statusCode(200)
        .body("findAll { it.id == " + room1Id + " }.size()", equalTo(0))
        .body("findAll { it.id == " + room2Id + " }.size()", equalTo(1));

    // But both rooms should be visible in /api/rooms (public list)
    given()
        .when()
        .get("/api/rooms")
        .then()
        .statusCode(200)
        .body("findAll { it.id == " + room1Id + " }.size()", equalTo(1))
        .body("findAll { it.id == " + room2Id + " }.size()", equalTo(1));
  }
}
