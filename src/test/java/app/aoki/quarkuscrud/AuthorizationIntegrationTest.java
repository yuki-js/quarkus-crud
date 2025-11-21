package app.aoki.quarkuscrud;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Integration tests for authorization and access control. Tests that users can only modify their
 * own resources and validates multi-user scenarios.
 */
@QuarkusTest
@TestMethodOrder(OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AuthorizationIntegrationTest {

  private static String user1Token;
  private static String user2Token;
  private static Long user1Id;
  private static Long user2Id;
  private static Long user1EventId;

  @Test
  @Order(0)
  public void setup() {
    // Create two different guest users for testing authorization
    Response user1Response = given().contentType(ContentType.JSON).post("/api/auth/guest");
    user1Token = user1Response.getHeader("Authorization").substring(7);
    user1Id = user1Response.jsonPath().getLong("id");

    Response user2Response = given().contentType(ContentType.JSON).post("/api/auth/guest");
    user2Token = user2Response.getHeader("Authorization").substring(7);
    user2Id = user2Response.jsonPath().getLong("id");

    // User 1 creates an event
    Response eventResponse =
        given()
            .header("Authorization", "Bearer " + user1Token)
            .contentType(ContentType.JSON)
            .body("{}")
            .post("/api/events");
    user1EventId = eventResponse.jsonPath().getLong("id");

    // Set up profiles
    given()
        .header("Authorization", "Bearer " + user1Token)
        .contentType(ContentType.JSON)
        .body("{\"profileData\":{\"displayName\":\"User One\"}}")
        .put("/api/me/profile");

    given()
        .header("Authorization", "Bearer " + user2Token)
        .contentType(ContentType.JSON)
        .body("{\"profileData\":{\"displayName\":\"User Two\"}}")
        .put("/api/me/profile");
  }

  @Test
  @Order(1)
  public void testUserCanAccessOwnEvent() {
    given()
        .header("Authorization", "Bearer " + user1Token)
        .when()
        .get("/api/events/" + user1EventId)
        .then()
        .statusCode(200)
        .body("id", equalTo(user1EventId.intValue()))
        .body("initiatorId", equalTo(user1Id.intValue()));
  }

  @Test
  @Order(2)
  public void testAnotherUserCanViewEvent() {
    // User 2 should be able to view User 1's event (events are generally viewable)
    given()
        .header("Authorization", "Bearer " + user2Token)
        .when()
        .get("/api/events/" + user1EventId)
        .then()
        .statusCode(200)
        .body("id", equalTo(user1EventId.intValue()));
  }

  @Test
  @Order(3)
  public void testUserCannotUpdateAnotherUsersProfile() {
    // User 2 tries to update User 1's profile (should fail)
    // The API only allows updating /api/me/profile, so this is inherently protected
    // But we can verify User 2 cannot update as if they were User 1
    given()
        .header("Authorization", "Bearer " + user2Token)
        .contentType(ContentType.JSON)
        .body("{\"profileData\":{\"displayName\":\"Hacked Name\"}}")
        .when()
        .put("/api/me/profile")
        .then()
        .statusCode(200)
        .body(
            "profileData.displayName",
            equalTo("Hacked Name")); // This updates User 2's profile, not User 1's

    // Verify User 1's profile is unchanged
    given()
        .header("Authorization", "Bearer " + user1Token)
        .when()
        .get("/api/me/profile")
        .then()
        .statusCode(200)
        .body("profileData.displayName", equalTo("User One"));
  }

  @Test
  @Order(4)
  public void testUserCanUpdateOwnProfile() {
    given()
        .header("Authorization", "Bearer " + user1Token)
        .contentType(ContentType.JSON)
        .body("{\"profileData\":{\"displayName\":\"Updated by Owner\",\"bio\":\"Owner update\"}}")
        .when()
        .put("/api/me/profile")
        .then()
        .statusCode(200)
        .body("profileData.displayName", equalTo("Updated by Owner"))
        .body("profileData.bio", equalTo("Owner update"));
  }

  @Test
  @Order(5)
  public void testUserCanListTheirOwnEvents() {
    given()
        .header("Authorization", "Bearer " + user1Token)
        .when()
        .get("/api/users/" + user1Id + "/events")
        .then()
        .statusCode(200)
        .body("size()", greaterThanOrEqualTo(1))
        .body("[0].initiatorId", equalTo(user1Id.intValue()));
  }

  @Test
  @Order(6)
  public void testUserCanListAnotherUsersEvents() {
    // User 2 can view User 1's events (public information)
    given()
        .header("Authorization", "Bearer " + user2Token)
        .when()
        .get("/api/users/" + user1Id + "/events")
        .then()
        .statusCode(200);
  }

  @Test
  @Order(7)
  public void testUnauthorizedAccessReturns401() {
    given()
        .when()
        .get("/api/me")
        .then()
        .statusCode(401)
        .body("error", equalTo("No JWT token found"));
  }

  @Test
  @Order(8)
  public void testUserCanReceiveFriendshipFromAnother() {
    // User 1 sends friendship to User 2
    given()
        .header("Authorization", "Bearer " + user1Token)
        .contentType(ContentType.JSON)
        .body("{\"fromUserId\":" + user1Id + "}")
        .when()
        .post("/api/users/" + user2Id + "/friendship")
        .then()
        .statusCode(anyOf(is(200), is(201)))
        .body("senderUserId", equalTo(user1Id.intValue()))
        .body("recipientUserId", equalTo(user2Id.intValue()));

    // User 2 can see the received friendship
    given()
        .header("Authorization", "Bearer " + user2Token)
        .when()
        .get("/api/me/friendships/received")
        .then()
        .statusCode(200)
        .body("size()", greaterThanOrEqualTo(1));
  }
}
