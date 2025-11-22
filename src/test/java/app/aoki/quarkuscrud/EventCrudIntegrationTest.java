package app.aoki.quarkuscrud;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Integration tests for Event CRUD operations. Tests create, read, and join functionality for
 * events. Note: Events don't support traditional update/delete operations - they use status
 * transitions instead.
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class EventCrudIntegrationTest {

  private static String jwtToken;
  private static Long eventId;

  @Test
  @Order(0)
  public void setup() {
    // Create a guest user for testing
    Response response = given().contentType(ContentType.JSON).post("/api/auth/guest");
    jwtToken = response.getHeader("Authorization").substring(7);
  }

  @Test
  @Order(1)
  public void testCreateEventWithoutAuthentication() {
    given()
        .contentType(ContentType.JSON)
        .body("{\"title\":\"Test Event\",\"description\":\"Test Description\"}")
        .when()
        .post("/api/events")
        .then()
        .statusCode(401)
        .body("error", equalTo("No JWT token found"));
  }

  @Test
  @Order(2)
  public void testCreateEventWithAuthentication() {
    Response response =
        given()
            .header("Authorization", "Bearer " + jwtToken)
            .contentType(ContentType.JSON)
            .body("{}")
            .when()
            .post("/api/events")
            .then()
            .statusCode(anyOf(is(200), is(201)))
            .body("id", notNullValue())
            .body("invitationCode", notNullValue())
            .extract()
            .response();

    eventId = response.jsonPath().getLong("id");
    assertNotNull(eventId, "Event ID should not be null");
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
        .body("status", notNullValue())
        .body("initiatorId", notNullValue());
  }

  @Test
  @Order(4)
  public void testGetNonExistentEvent() {
    given()
        .header("Authorization", "Bearer " + jwtToken)
        .when()
        .get("/api/events/999999")
        .then()
        .statusCode(404);
  }

  @Test
  @Order(5)
  public void testListEventsByUser() {
    // Get current user's ID
    Response userResponse =
        given().header("Authorization", "Bearer " + jwtToken).when().get("/api/me");
    long userId = userResponse.jsonPath().getLong("id");

    // List events for this user
    given()
        .header("Authorization", "Bearer " + jwtToken)
        .when()
        .get("/api/users/" + userId + "/events")
        .then()
        .statusCode(200)
        .body("size()", greaterThanOrEqualTo(1));
  }

  @Test
  @Order(6)
  public void testJoinEventByCode() {
    // Create a new user to join the event
    Response newUserResponse = given().contentType(ContentType.JSON).post("/api/auth/guest");
    String newUserToken = newUserResponse.getHeader("Authorization").substring(7);

    // Get the invitation code for the event
    Response eventResponse =
        given().header("Authorization", "Bearer " + jwtToken).when().get("/api/events/" + eventId);
    String invitationCode = eventResponse.jsonPath().getString("invitationCode");

    // Join the event using the code
    given()
        .header("Authorization", "Bearer " + newUserToken)
        .contentType(ContentType.JSON)
        .body("{\"invitationCode\":\"" + invitationCode + "\"}")
        .when()
        .post("/api/events/join-by-code")
        .then()
        .statusCode(anyOf(is(200), is(201)))
        .body("id", notNullValue());
  }

  @Test
  @Order(7)
  public void testListEventAttendees() {
    given()
        .header("Authorization", "Bearer " + jwtToken)
        .when()
        .get("/api/events/" + eventId + "/attendees")
        .then()
        .statusCode(200)
        .body("size()", greaterThanOrEqualTo(1));
  }
}
