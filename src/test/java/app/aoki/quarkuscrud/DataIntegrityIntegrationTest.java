package app.aoki;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Integration tests for data integrity and special cases. Tests handling of special characters,
 * null values, and edge cases.
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DataIntegrityIntegrationTest {

  private static String jwtToken;

  @Test
  @Order(0)
  public void setup() {
    // Create a guest user for testing
    Response response = given().contentType(ContentType.JSON).post("/api/auth/guest");
    jwtToken = response.getHeader("Authorization").substring(7);
  }

  @Test
  @Order(1)
  public void testCreateEventWithSpecialCharacters() {
    given()
        .header("Authorization", "Bearer " + jwtToken)
        .contentType(ContentType.JSON)
        .body(
            "{\"name\":\"Event @#$% with & special chars\",\"description\":\"Testing special chars\"}")
        .when()
        .post("/api/events")
        .then()
        .statusCode(anyOf(is(200), is(201)))
        .body("name", equalTo("Event @#$% with & special chars"))
        .body("description", equalTo("Testing special chars"));
  }

  @Test
  public void testUpdateEventWithNullDescription() {
    // Create a event first
    Response createResponse =
        given()
            .header("Authorization", "Bearer " + jwtToken)
            .contentType(ContentType.JSON)
            .body("{\"name\":\"Event for Null Test\",\"description\":\"Initial description\"}")
            .post("/api/events");
    Long eventId = createResponse.jsonPath().getLong("id");

    // Update with null description
    given()
        .header("Authorization", "Bearer " + jwtToken)
        .contentType(ContentType.JSON)
        .body("{\"name\":\"Event with Null Description\",\"description\":null}")
        .when()
        .put("/api/events/" + eventId)
        .then()
        .statusCode(200)
        .body("name", equalTo("Event with Null Description"))
        .body("description", nullValue());
  }

  @Test
  public void testCreateMultipleEventsSameUser() {
    // Create first event
    Response event1 =
        given()
            .header("Authorization", "Bearer " + jwtToken)
            .contentType(ContentType.JSON)
            .body("{\"name\":\"Living Event\",\"description\":\"First event\"}")
            .post("/api/events")
            .then()
            .statusCode(anyOf(is(200), is(201)))
            .extract()
            .response();
    Long event1Id = event1.jsonPath().getLong("id");

    // Create second event
    Response event2 =
        given()
            .header("Authorization", "Bearer " + jwtToken)
            .contentType(ContentType.JSON)
            .body("{\"name\":\"Bedevent\",\"description\":\"Second event\"}")
            .post("/api/events")
            .then()
            .statusCode(anyOf(is(200), is(201)))
            .extract()
            .response();
    Long event2Id = event2.jsonPath().getLong("id");

    // Create third event
    Response event3 =
        given()
            .header("Authorization", "Bearer " + jwtToken)
            .contentType(ContentType.JSON)
            .body("{\"name\":\"Kitchen\",\"description\":\"Third event\"}")
            .post("/api/events")
            .then()
            .statusCode(anyOf(is(200), is(201)))
            .extract()
            .response();
    Long event3Id = event3.jsonPath().getLong("id");

    // Verify all events are in my events
    given()
        .header("Authorization", "Bearer " + jwtToken)
        .when()
        .get("/api/events/my")
        .then()
        .statusCode(200)
        .body("$", hasSize(greaterThanOrEqualTo(3)))
        .body("findAll { it.id == " + event1Id + " }.size()", equalTo(1))
        .body("findAll { it.id == " + event2Id + " }.size()", equalTo(1))
        .body("findAll { it.id == " + event3Id + " }.size()", equalTo(1));
  }

  @Test
  public void testEventNameWithEmojisAndUnicode() {
    given()
        .header("Authorization", "Bearer " + jwtToken)
        .contentType(ContentType.JSON)
        .body(
            "{\"name\":\"Event 🏠 with Unicode 日本語\",\"description\":\"Testing unicode support\"}")
        .when()
        .post("/api/events")
        .then()
        .statusCode(anyOf(is(200), is(201)))
        .body("name", equalTo("Event 🏠 with Unicode 日本語"));
  }

  @Test
  public void testLongEventName() {
    String longName = "A".repeat(250); // Test with 250 characters
    given()
        .header("Authorization", "Bearer " + jwtToken)
        .contentType(ContentType.JSON)
        .body("{\"name\":\"" + longName + "\",\"description\":\"Testing long name\"}")
        .when()
        .post("/api/events")
        .then()
        .statusCode(anyOf(is(200), is(201), is(400))); // May fail validation or succeed
  }

  @Test
  public void testEmptyEventName() {
    given()
        .header("Authorization", "Bearer " + jwtToken)
        .contentType(ContentType.JSON)
        .body("{\"name\":\"\",\"description\":\"Empty name test\"}")
        .when()
        .post("/api/events")
        .then()
        .statusCode(anyOf(is(200), is(201), is(400))); // May fail validation or succeed
  }
}
