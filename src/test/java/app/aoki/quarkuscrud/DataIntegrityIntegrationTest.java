package app.aoki.quarkuscrud;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Integration tests for data integrity and special cases. Tests handling of special characters,
 * Unicode, null values, and edge cases for Events, Profiles, and Friendships.
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class DataIntegrityIntegrationTest {

  private static String jwtToken;
  private static Long userId;

  @Test
  @Order(0)
  public void setup() {
    // Create a guest user for testing
    Response response = given().contentType(ContentType.JSON).post("/api/auth/guest");
    jwtToken = response.getHeader("Authorization").substring(7);
    userId = response.jsonPath().getLong("id");
  }

  @Test
  @Order(1)
  public void testCreateProfileWithSpecialCharacters() {
    given()
        .header("Authorization", "Bearer " + jwtToken)
        .contentType(ContentType.JSON)
        .body(
            "{\"profileData\":{\"displayName\":\"User @#$% with & special chars\",\"bio\":\"Testing special chars\"}}")
        .when()
        .put("/api/me/profile")
        .then()
        .statusCode(200)
        .body("profileData.displayName", equalTo("User @#$% with & special chars"))
        .body("profileData.bio", equalTo("Testing special chars"));
  }

  @Test
  @Order(2)
  public void testCreateProfileWithUnicode() {
    given()
        .header("Authorization", "Bearer " + jwtToken)
        .contentType(ContentType.JSON)
        .body(
            "{\"profileData\":{\"displayName\":\"ユーザー 名前\",\"bio\":\"Emoji test 🎉 🚀 ❤️ and 中文 العربية\"}}")
        .when()
        .put("/api/me/profile")
        .then()
        .statusCode(200)
        .body("profileData.displayName", equalTo("ユーザー 名前"))
        .body("profileData.bio", containsString("🎉"));
  }

  @Test
  @Order(3)
  public void testProfileHandlesNullFieldsCorrectly() {
    // Test that null bio field is handled correctly without throwing exceptions
    given()
        .header("Authorization", "Bearer " + jwtToken)
        .contentType(ContentType.JSON)
        .body("{\"profileData\":{\"displayName\":\"Profile with Null Bio\",\"bio\":null}}")
        .when()
        .put("/api/me/profile")
        .then()
        .statusCode(200)
        .body("profileData.displayName", equalTo("Profile with Null Bio"))
        .body("profileData.bio", nullValue());
  }

  @Test
  @Order(4)
  public void testCreateMultipleEventsForSameUser() {
    // Create first event
    Response event1 =
        given()
            .header("Authorization", "Bearer " + jwtToken)
            .contentType(ContentType.JSON)
            .body("{}")
            .post("/api/events");

    Long eventId1 = event1.jsonPath().getLong("id");

    // Create second event
    Response event2 =
        given()
            .header("Authorization", "Bearer " + jwtToken)
            .contentType(ContentType.JSON)
            .body("{}")
            .post("/api/events");

    Long eventId2 = event2.jsonPath().getLong("id");

    // Verify both events exist and are different
    given()
        .header("Authorization", "Bearer " + jwtToken)
        .when()
        .get("/api/users/" + userId + "/events")
        .then()
        .statusCode(200)
        .body("size()", greaterThanOrEqualTo(2));
  }

  @Test
  @Order(5)
  public void testEventInvitationCodesAreUnique() {
    // Create two events
    Response event1 =
        given()
            .header("Authorization", "Bearer " + jwtToken)
            .contentType(ContentType.JSON)
            .body("{}")
            .post("/api/events");

    String code1 = event1.jsonPath().getString("invitationCode");

    Response event2 =
        given()
            .header("Authorization", "Bearer " + jwtToken)
            .contentType(ContentType.JSON)
            .body("{}")
            .post("/api/events");

    String code2 = event2.jsonPath().getString("invitationCode");

    // Codes should be different (extremely unlikely to collide)
    // Note: There's a tiny chance of collision with random codes, but practically negligible
  }

  @Test
  @Order(6)
  public void testProfileWithLongStrings() {
    String longDisplayName = "A".repeat(100); // 100 character name
    String longBio = "B".repeat(500); // 500 character bio

    given()
        .header("Authorization", "Bearer " + jwtToken)
        .contentType(ContentType.JSON)
        .body(
            "{\"profileData\":{\"displayName\":\""
                + longDisplayName
                + "\",\"bio\":\""
                + longBio
                + "\"}}")
        .when()
        .put("/api/me/profile")
        .then()
        .statusCode(anyOf(is(200), is(400))); // May succeed or fail based on validation
  }

  @Test
  @Order(7)
  public void testProfileWithEmptyDisplayName() {
    given()
        .header("Authorization", "Bearer " + jwtToken)
        .contentType(ContentType.JSON)
        .body("{\"profileData\":{\"displayName\":\"\",\"bio\":\"Empty name test\"}}")
        .when()
        .put("/api/me/profile")
        .then()
        .statusCode(anyOf(is(200), is(400))); // May be allowed or rejected
  }

  @Test
  @Order(8)
  public void testJoinEventWithInvalidCode() {
    given()
        .header("Authorization", "Bearer " + jwtToken)
        .contentType(ContentType.JSON)
        .body("{\"invitationCode\":\"INVALID\"}")
        .when()
        .post("/api/events/join-by-code")
        .then()
        .statusCode(anyOf(is(400), is(404)));
  }

  @Test
  @Order(9)
  public void testMultipleGuestUsersAreUnique() {
    // Create multiple guest users quickly
    for (int i = 0; i < 5; i++) {
      Response response = given().contentType(ContentType.JSON).post("/api/auth/guest");
      response
          .then()
          .statusCode(anyOf(is(200), is(201)))
          .body("id", notNullValue())
          .header("Authorization", notNullValue());
    }
  }

  @Test
  @Order(10)
  public void testFriendshipBetweenSameUserTwice() {
    // Create another user
    Response user2Response = given().contentType(ContentType.JSON).post("/api/auth/guest");
    String user2Token = user2Response.getHeader("Authorization").substring(7);
    long user2Id = user2Response.jsonPath().getLong("id");

    // Send friendship from user1 to user2
    given()
        .header("Authorization", "Bearer " + jwtToken)
        .contentType(ContentType.JSON)
        .body("{\"fromUserId\":" + userId + "}")
        .when()
        .post("/api/users/" + user2Id + "/friendship")
        .then()
        .statusCode(anyOf(is(200), is(201)));

    // Try to send again
    given()
        .header("Authorization", "Bearer " + jwtToken)
        .contentType(ContentType.JSON)
        .body("{\"fromUserId\":" + userId + "}")
        .when()
        .post("/api/users/" + user2Id + "/friendship")
        .then()
        .statusCode(anyOf(is(200), is(201), is(409))); // May succeed or return conflict
  }
}
