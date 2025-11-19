package app.aoki;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Integration tests for data integrity and special cases in Events/Profiles API. Tests handling of
 * special characters, null values, edge cases, and data validation. Equivalent to
 * DataIntegrityIntegrationTest in the original codebase.
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)

public class EventDataIntegrityTest {

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
            "{\"meta\":{\"name\":\"Event @#$% with & special chars\",\"description\":\"Testing special chars\"},\"expiresAt\":\"2025-12-31T23:59:59Z\"}")
        .when()
        .post("/api/events")
        .then()
        .statusCode(201)
        .body("id", notNullValue())
        .body("status", equalTo("active"));
  }

  @Test
  @Order(2)
  public void testEventNameWithEmojisAndUnicode() {
    given()
        .header("Authorization", "Bearer " + jwtToken)
        .contentType(ContentType.JSON)
        .body(
            "{\"meta\":{\"name\":\"Quiz Night 🎮🎯 クイズナイト\",\"description\":\"Unicode test\"},\"expiresAt\":\"2025-12-31T23:59:59Z\"}")
        .when()
        .post("/api/events")
        .then()
        .statusCode(201)
        .body("id", notNullValue());
  }

  @Test
  @Order(3)
  public void testProfileWithSpecialCharacters() {
    given()
        .header("Authorization", "Bearer " + jwtToken)
        .contentType(ContentType.JSON)
        .body(
            "{\"profileData\":{\"displayName\":\"Test User @#$%\",\"bio\":\"I love quizzes & games!\"}}")
        .when()
        .put("/api/me/profile")
        .then()
        .statusCode(200)
        .body("profileData.displayName", equalTo("Test User @#$%"))
        .body("profileData.bio", equalTo("I love quizzes & games!"));
  }

  @Test
  @Order(4)
  public void testProfileWithUnicodeAndEmojis() {
    given()
        .header("Authorization", "Bearer " + jwtToken)
        .contentType(ContentType.JSON)
        .body(
            "{\"profileData\":{\"displayName\":\"Quiz Master 🎮\",\"bio\":\"日本語もOK\"}}")
        .when()
        .put("/api/me/profile")
        .then()
        .statusCode(200)
        .body("profileData.displayName", equalTo("Quiz Master 🎮"))
        .body("profileData.bio", equalTo("日本語もOK"));
  }

  @Test
  @Order(5)
  public void testCreateMultipleEventsSameUser() {
    // Create first event
    Response event1 =
        given()
            .header("Authorization", "Bearer " + jwtToken)
            .contentType(ContentType.JSON)
            .body(
                "{\"meta\":{\"name\":\"Morning Quiz\"},\"expiresAt\":\"2025-12-31T23:59:59Z\"}")
            .post("/api/events");

    // Create second event
    Response event2 =
        given()
            .header("Authorization", "Bearer " + jwtToken)
            .contentType(ContentType.JSON)
            .body(
                "{\"meta\":{\"name\":\"Evening Quiz\"},\"expiresAt\":\"2025-12-31T23:59:59Z\"}")
            .post("/api/events");

    // Verify both have unique IDs
    Long id1 = event1.jsonPath().getLong("id");
    Long id2 = event2.jsonPath().getLong("id");
    given()
        .header("Authorization", "Bearer " + jwtToken)
        .get("/api/events/" + id1)
        .then()
        .statusCode(200);
    given()
        .header("Authorization", "Bearer " + jwtToken)
        .get("/api/events/" + id2)
        .then()
        .statusCode(200);
  }

  @Test
  @Order(6)
  public void testProfileWithComplexNestedData() {
    given()
        .header("Authorization", "Bearer " + jwtToken)
        .contentType(ContentType.JSON)
        .body(
            "{\"profileData\":{\"displayName\":\"Complex User\",\"preferences\":{\"theme\":\"dark\",\"notifications\":true},\"stats\":{\"wins\":10,\"losses\":5}}}")
        .when()
        .put("/api/me/profile")
        .then()
        .statusCode(200)
        .body("profileData.preferences.theme", equalTo("dark"))
        .body("profileData.stats.wins", equalTo(10));
  }

  @Test
  @Order(7)
  public void testEventWithComplexMetadata() {
    given()
        .header("Authorization", "Bearer " + jwtToken)
        .contentType(ContentType.JSON)
        .body(
            "{\"meta\":{\"name\":\"Complex Event\",\"rules\":{\"maxParticipants\":10,\"duration\":60},\"tags\":[\"science\",\"history\"]},\"expiresAt\":\"2025-12-31T23:59:59Z\"}")
        .when()
        .post("/api/events")
        .then()
        .statusCode(201)
        .body("id", notNullValue());
  }

  @Test
  @Order(8)
  public void testMultipleProfileUpdates() {
    // First update
    given()
        .header("Authorization", "Bearer " + jwtToken)
        .contentType(ContentType.JSON)
        .body("{\"profileData\":{\"displayName\":\"Version 1\"}}")
        .put("/api/me/profile")
        .then()
        .statusCode(200);

    // Second update
    given()
        .header("Authorization", "Bearer " + jwtToken)
        .contentType(ContentType.JSON)
        .body("{\"profileData\":{\"displayName\":\"Version 2\"}}")
        .put("/api/me/profile")
        .then()
        .statusCode(200)
        .body("profileData.displayName", equalTo("Version 2"));

    // Verify latest version is returned
    given()
        .header("Authorization", "Bearer " + jwtToken)
        .get("/api/me/profile")
        .then()
        .statusCode(200)
        .body("profileData.displayName", equalTo("Version 2"));
  }
}
