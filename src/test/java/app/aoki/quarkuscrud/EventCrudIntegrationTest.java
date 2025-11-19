package app.aoki;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;

/**
 * Integration tests for Event CRUD operations. Tests create, read, update, and delete functionality
 * for events.
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
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
        .body("{\"name\":\"Test Event\",\"description\":\"Test Description\"}")
        .when()
        .post("/api/events")
        .then()
        .statusCode(401)
        .body("error", equalTo("Authentication required"));
  }

  @Test
  @Order(2)
  public void testCreateEventWithAuthentication() {
    Response response =
        given()
            .header("Authorization", "Bearer " + jwtToken)
            .contentType(ContentType.JSON)
            .body("{\"name\":\"Test Event\",\"description\":\"Test Description\"}")
            .when()
            .post("/api/events")
            .then()
            .statusCode(anyOf(is(200), is(201)))
            .body("id", notNullValue())
            .body("name", equalTo("Test Event"))
            .body("description", equalTo("Test Description"))
            .body("initiatorUserId", notNullValue())
            .body("status", equalTo("created"))
            .body("expiresAt", notNullValue())
            .body("createdAt", notNullValue())
            .body("updatedAt", notNullValue())
            .extract()
            .response();

    eventId = response.jsonPath().getLong("id");
    assertNotNull(eventId, "Event ID should not be null");
  }

  @Test
  @Order(3)
  public void testGetEventById() {
    given()
        .when()
        .get("/api/events/" + eventId)
        .then()
        .statusCode(200)
        .body("id", equalTo(eventId.intValue()))
        .body("name", equalTo("Test Event"))
        .body("description", equalTo("Test Description"));
  }

  @Test
  @Order(4)
  public void testGetNonExistentEvent() {
    given()
        .when()
        .get("/api/events/999999")
        .then()
        .statusCode(404)
        .body("error", equalTo("Event not found"));
  }

  @Test
  @Order(5)
  public void testGetAllEvents() {
    given()
        .when()
        .get("/api/events")
        .then()
        .statusCode(200)
        .body("$", hasSize(greaterThanOrEqualTo(1)))
        .body("[0].id", notNullValue())
        .body("[0].name", notNullValue());
  }

  @Test
  @Order(6)
  public void testGetMyEvents() {
    given()
        .header("Authorization", "Bearer " + jwtToken)
        .when()
        .get("/api/events/my")
        .then()
        .statusCode(200)
        .body("$", hasSize(greaterThanOrEqualTo(1)));
  }

  @Test
  @Order(7)
  public void testGetMyEventsWithoutAuthentication() {
    given()
        .when()
        .get("/api/events/my")
        .then()
        .statusCode(401)
        .body("error", equalTo("Authentication required"));
  }

  @Test
  @Order(8)
  public void testUpdateEvent() {
    given()
        .header("Authorization", "Bearer " + jwtToken)
        .contentType(ContentType.JSON)
        .body("{\"name\":\"Updated Event\",\"description\":\"Updated Description\"}")
        .when()
        .put("/api/events/" + eventId)
        .then()
        .statusCode(200)
        .body("id", equalTo(eventId.intValue()))
        .body("name", equalTo("Updated Event"))
        .body("description", equalTo("Updated Description"));
  }

  @Test
  @Order(9)
  public void testUpdateEventWithoutAuthentication() {
    given()
        .contentType(ContentType.JSON)
        .body("{\"name\":\"Hacked Event\",\"description\":\"Hacked Description\"}")
        .when()
        .put("/api/events/" + eventId)
        .then()
        .statusCode(401)
        .body("error", equalTo("Authentication required"));
  }

  @Test
  @Order(10)
  public void testUpdateNonExistentEvent() {
    given()
        .header("Authorization", "Bearer " + jwtToken)
        .contentType(ContentType.JSON)
        .body("{\"name\":\"Ghost Event\",\"description\":\"Ghost Description\"}")
        .when()
        .put("/api/events/999999")
        .then()
        .statusCode(404)
        .body("error", equalTo("Event not found"));
  }

  @Test
  @Order(11)
  public void testDeleteEventWithoutAuthentication() {
    given()
        .when()
        .delete("/api/events/" + eventId)
        .then()
        .statusCode(401)
        .body("error", equalTo("Authentication required"));
  }

  @Test
  @Order(12)
  public void testDeleteEvent() {
    given()
        .header("Authorization", "Bearer " + jwtToken)
        .when()
        .delete("/api/events/" + eventId)
        .then()
        .statusCode(204);

    // Verify event is deleted
    given().when().get("/api/events/" + eventId).then().statusCode(404);
  }

  @Test
  @Order(13)
  public void testDeleteNonExistentEvent() {
    given()
        .header("Authorization", "Bearer " + jwtToken)
        .when()
        .delete("/api/events/999999")
        .then()
        .statusCode(404)
        .body("error", equalTo("Event not found"));
  }
}
