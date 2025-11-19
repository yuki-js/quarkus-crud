package app.aoki;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Integration tests for authorization and access control in the new Events/Profiles/Friendships
 * API. Tests that users can only access and modify their own resources appropriately. Equivalent to
 * AuthorizationIntegrationTest in the original codebase.
 */
@QuarkusTest
@TestMethodOrder(OrderAnnotation.class)
public class EventAuthorizationIntegrationTest {

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
    assertNotNull(user1Token, "User 1 token should not be null");
    assertNotNull(user1Id, "User 1 ID should not be null");

    Response user2Response = given().contentType(ContentType.JSON).post("/api/auth/guest");
    user2Token = user2Response.getHeader("Authorization").substring(7);
    user2Id = user2Response.jsonPath().getLong("id");
    assertNotNull(user2Token, "User 2 token should not be null");
    assertNotNull(user2Id, "User 2 ID should not be null");

    // User 1 creates an event
    Response eventResponse =
        given()
            .header("Authorization", "Bearer " + user1Token)
            .contentType(ContentType.JSON)
            .body("{\"meta\":{\"name\":\"User 1 Event\"},\"expiresAt\":\"2025-12-31T23:59:59Z\"}")
            .post("/api/events");
    user1EventId = eventResponse.jsonPath().getLong("id");
  }

  @Test
  @Order(1)
  public void testUserCanAccessOwnProfile() {
    given()
        .header("Authorization", "Bearer " + user1Token)
        .when()
        .get("/api/me")
        .then()
        .statusCode(200)
        .body("id", equalTo(user1Id.intValue()));
  }

  @Test
  @Order(2)
  public void testUserCanUpdateOwnProfile() {
    given()
        .header("Authorization", "Bearer " + user1Token)
        .contentType(ContentType.JSON)
        .body("{\"profileData\":{\"displayName\":\"User 1\"}}")
        .when()
        .put("/api/me/profile")
        .then()
        .statusCode(200)
        .body("userId", equalTo(user1Id.intValue()))
        .body("profileData.displayName", equalTo("User 1"));
  }

  @Test
  @Order(3)
  public void testUserCanViewOtherUsersPublicProfile() {
    // User 2 should be able to view User 1's profile
    given()
        .header("Authorization", "Bearer " + user2Token)
        .when()
        .get("/api/users/" + user1Id + "/profile")
        .then()
        // Should return 404 if no profile exists, or 200 if it exists
        .statusCode(anyOf(is(200), is(404)));
  }

  @Test
  @Order(4)
  public void testMultiUserEventIsolation() {
    // User 1 creates an event
    Response event1Response =
        given()
            .header("Authorization", "Bearer " + user1Token)
            .contentType(ContentType.JSON)
            .body("{\"meta\":{\"name\":\"User 1 Event\"},\"expiresAt\":\"2025-12-31T23:59:59Z\"}")
            .post("/api/events");
    Long event1Id = event1Response.jsonPath().getLong("id");

    // User 2 creates an event
    Response event2Response =
        given()
            .header("Authorization", "Bearer " + user2Token)
            .contentType(ContentType.JSON)
            .body("{\"meta\":{\"name\":\"User 2 Event\"},\"expiresAt\":\"2025-12-31T23:59:59Z\"}")
            .post("/api/events");
    Long event2Id = event2Response.jsonPath().getLong("id");

    // User 1 should see their events
    given()
        .header("Authorization", "Bearer " + user1Token)
        .when()
        .get("/api/users/" + user1Id + "/events")
        .then()
        .statusCode(200)
        .body("findAll { it.id == " + event1Id + " }.size()", equalTo(1))
        .body("findAll { it.id == " + event2Id + " }.size()", equalTo(0));

    // User 2 should see their events
    given()
        .header("Authorization", "Bearer " + user2Token)
        .when()
        .get("/api/users/" + user2Id + "/events")
        .then()
        .statusCode(200)
        .body("findAll { it.id == " + event1Id + " }.size()", equalTo(0))
        .body("findAll { it.id == " + event2Id + " }.size()", equalTo(1));
  }

  @Test
  @Order(5)
  public void testFriendshipCreation() {
    // User 2 records that they received User 1's profile card
    given()
        .header("Authorization", "Bearer " + user2Token)
        .contentType(ContentType.JSON)
        .body("{}")
        .when()
        .post("/api/users/" + user1Id + "/friendship")
        .then()
        .statusCode(201)
        .body("senderUserId", equalTo(user1Id.intValue()))
        .body("recipientUserId", equalTo(user2Id.intValue()));
  }

  @Test
  @Order(6)
  public void testUserCanListReceivedFriendships() {
    given()
        .header("Authorization", "Bearer " + user2Token)
        .when()
        .get("/api/me/friendships/received")
        .then()
        .statusCode(200)
        .body("$", hasSize(greaterThanOrEqualTo(1)))
        .body("[0].recipientUserId", equalTo(user2Id.intValue()));
  }

  @Test
  @Order(7)
  public void testCannotCreateDuplicateFriendship() {
    // Try to create the same friendship again
    given()
        .header("Authorization", "Bearer " + user2Token)
        .contentType(ContentType.JSON)
        .body("{}")
        .when()
        .post("/api/users/" + user1Id + "/friendship")
        .then()
        .statusCode(409)
        .body("error", containsString("already exists"));
  }
}
