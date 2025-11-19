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
  private static Long user1EventId;

  @Test
  @Order(0)
  public void setup() {
    // Create two different guest users for testing authorization
    Response user1Response = given().contentType(ContentType.JSON).post("/api/auth/guest");
    user1Token = user1Response.getHeader("Authorization").substring(7);

    Response user2Response = given().contentType(ContentType.JSON).post("/api/auth/guest");
    user2Token = user2Response.getHeader("Authorization").substring(7);

    // User 1 creates a event
    Response eventResponse =
        given()
            .header("Authorization", "Bearer " + user1Token)
            .contentType(ContentType.JSON)
            .body("{\"name\":\"User 1 Event\",\"description\":\"Owned by user 1\"}")
            .post("/api/events");
    user1EventId = eventResponse.jsonPath().getLong("id");
  }

  @Test
  @Order(1)
  public void testUserCannotUpdateAnotherUsersEvent() {
    given()
        .header("Authorization", "Bearer " + user2Token)
        .contentType(ContentType.JSON)
        .body("{\"name\":\"Hacked Event\",\"description\":\"Should fail\"}")
        .when()
        .put("/api/events/" + user1EventId)
        .then()
        .statusCode(403)
        .body("error", equalTo("You don't have permission to update this event"));
  }

  @Test
  @Order(2)
  public void testUserCannotDeleteAnotherUsersEvent() {
    given()
        .header("Authorization", "Bearer " + user2Token)
        .when()
        .delete("/api/events/" + user1EventId)
        .then()
        .statusCode(403)
        .body("error", equalTo("You don't have permission to delete this event"));
  }

  @Test
  @Order(3)
  public void testUserCanUpdateOwnEvent() {
    given()
        .header("Authorization", "Bearer " + user1Token)
        .contentType(ContentType.JSON)
        .body("{\"name\":\"Updated by Owner\",\"description\":\"Owner update\"}")
        .when()
        .put("/api/events/" + user1EventId)
        .then()
        .statusCode(200)
        .body("name", equalTo("Updated by Owner"));
  }

  @Test
  @Order(4)
  public void testUserCanDeleteOwnEvent() {
    given()
        .header("Authorization", "Bearer " + user1Token)
        .when()
        .delete("/api/events/" + user1EventId)
        .then()
        .statusCode(204);
  }

  @Test
  @Order(5)
  public void testMultiUserEventIsolation() {
    // User 1 creates a event
    Response event1Response =
        given()
            .header("Authorization", "Bearer " + user1Token)
            .contentType(ContentType.JSON)
            .body("{\"name\":\"User 1 Private Event\",\"description\":\"Private\"}")
            .post("/api/events");
    Long event1Id = event1Response.jsonPath().getLong("id");

    // User 2 creates a event
    Response event2Response =
        given()
            .header("Authorization", "Bearer " + user2Token)
            .contentType(ContentType.JSON)
            .body("{\"name\":\"User 2 Private Event\",\"description\":\"Private\"}")
            .post("/api/events");
    Long event2Id = event2Response.jsonPath().getLong("id");

    // User 1 should only see their own event in /my
    given()
        .header("Authorization", "Bearer " + user1Token)
        .when()
        .get("/api/events/my")
        .then()
        .statusCode(200)
        .body("findAll { it.id == " + event1Id + " }.size()", equalTo(1))
        .body("findAll { it.id == " + event2Id + " }.size()", equalTo(0));

    // User 2 should only see their own event in /my
    given()
        .header("Authorization", "Bearer " + user2Token)
        .when()
        .get("/api/events/my")
        .then()
        .statusCode(200)
        .body("findAll { it.id == " + event1Id + " }.size()", equalTo(0))
        .body("findAll { it.id == " + event2Id + " }.size()", equalTo(1));

    // But both events should be visible in /api/events (public list)
    given()
        .when()
        .get("/api/events")
        .then()
        .statusCode(200)
        .body("findAll { it.id == " + event1Id + " }.size()", equalTo(1))
        .body("findAll { it.id == " + event2Id + " }.size()", equalTo(1));
  }
}
