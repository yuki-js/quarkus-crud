package app.aoki.quarkuscrud;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Integration tests for Friendship operations. Tests friendship creation (profile card exchange)
 * and retrieval.
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class FriendshipIntegrationTest {

  private static String user1Token;
  private static String user2Token;
  private static Long user1Id;
  private static Long user2Id;

  @Test
  @Order(0)
  public void setup() {
    // Create two guest users for testing friendships
    Response user1Response = given().contentType(ContentType.JSON).post("/api/auth/guest");
    user1Token = user1Response.getHeader("Authorization").substring(7);
    user1Id = user1Response.jsonPath().getLong("id");

    Response user2Response = given().contentType(ContentType.JSON).post("/api/auth/guest");
    user2Token = user2Response.getHeader("Authorization").substring(7);
    user2Id = user2Response.jsonPath().getLong("id");

    // Set up profiles for both users
    given()
        .header("Authorization", "Bearer " + user1Token)
        .contentType(ContentType.JSON)
        .body("{\"profileData\":{\"displayName\":\"User One\",\"bio\":\"First user\"}}")
        .put("/api/me/profile");

    given()
        .header("Authorization", "Bearer " + user2Token)
        .contentType(ContentType.JSON)
        .body("{\"profileData\":{\"displayName\":\"User Two\",\"bio\":\"Second user\"}}")
        .put("/api/me/profile");
  }

  @Test
  @Order(1)
  public void testCreateFriendshipWithoutAuthentication() {
    given()
        .contentType(ContentType.JSON)
        .body("{}")
        .when()
        .post("/api/users/" + user2Id + "/friendship")
        .then()
        .statusCode(401);
  }

  @Test
  @Order(2)
  public void testReceiveFriendship() {
    // User 1 sends their profile card to User 2
    given()
        .header("Authorization", "Bearer " + user1Token)
        .contentType(ContentType.JSON)
        .body("{}")
        .when()
        .post("/api/users/" + user2Id + "/friendship")
        .then()
        .statusCode(anyOf(is(200), is(201)))
        .body("id", notNullValue())
        .body("senderUserId", equalTo(user1Id.intValue()))
        .body("recipientUserId", equalTo(user2Id.intValue()));
  }

  @Test
  @Order(3)
  public void testListReceivedFriendships() {
    // User 2 lists their received friendships
    given()
        .header("Authorization", "Bearer " + user2Token)
        .when()
        .get("/api/me/friendships/received")
        .then()
        .statusCode(200)
        .body("size()", greaterThanOrEqualTo(1))
        .body("[0].senderUserId", equalTo(user1Id.intValue()));
  }

  @Test
  @Order(4)
  public void testReceiveFriendshipTwice() {
    // User 1 tries to send profile card again to User 2
    given()
        .header("Authorization", "Bearer " + user1Token)
        .contentType(ContentType.JSON)
        .body("{}")
        .when()
        .post("/api/users/" + user2Id + "/friendship")
        .then()
        .statusCode(anyOf(is(200), is(201), is(409))); // May return 409 Conflict
  }

  @Test
  @Order(5)
  public void testBidirectionalFriendship() {
    // User 2 sends profile card back to User 1
    given()
        .header("Authorization", "Bearer " + user2Token)
        .contentType(ContentType.JSON)
        .body("{}")
        .when()
        .post("/api/users/" + user1Id + "/friendship")
        .then()
        .statusCode(anyOf(is(200), is(201)))
        .body("senderUserId", equalTo(user2Id.intValue()))
        .body("recipientUserId", equalTo(user1Id.intValue()));

    // User 1 should now have a received friendship from User 2
    given()
        .header("Authorization", "Bearer " + user1Token)
        .when()
        .get("/api/me/friendships/received")
        .then()
        .statusCode(200)
        .body("size()", greaterThanOrEqualTo(1));
  }

  @Test
  @Order(6)
  public void testReceiveFriendshipToNonExistentUser() {
    given()
        .header("Authorization", "Bearer " + user1Token)
        .contentType(ContentType.JSON)
        .body("{}")
        .when()
        .post("/api/users/999999/friendship")
        .then()
        .statusCode(404);
  }
}
