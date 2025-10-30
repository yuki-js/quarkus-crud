package app.aoki;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Integration tests for data integrity and special cases. Tests handling of special characters,
 * null values, and edge cases.
 */
@QuarkusTest
public class DataIntegrityIntegrationTest {

  private static String guestToken;

  @BeforeAll
  public static void setup() {
    // Create a guest user for testing
    Response response = given().contentType(ContentType.JSON).post("/api/auth/guest");
    guestToken = response.getCookie("guest_token");
  }

  @Test
  public void testCreateRoomWithSpecialCharacters() {
    given()
        .cookie("guest_token", guestToken)
        .contentType(ContentType.JSON)
        .body(
            "{\"name\":\"Room @#$% with & special chars\",\"description\":\"Testing special chars\"}")
        .when()
        .post("/api/rooms")
        .then()
        .statusCode(anyOf(is(200), is(201)))
        .body("name", equalTo("Room @#$% with & special chars"))
        .body("description", equalTo("Testing special chars"));
  }

  @Test
  public void testUpdateRoomWithNullDescription() {
    // Create a room first
    Response createResponse =
        given()
            .cookie("guest_token", guestToken)
            .contentType(ContentType.JSON)
            .body("{\"name\":\"Room for Null Test\",\"description\":\"Initial description\"}")
            .post("/api/rooms");
    Long roomId = createResponse.jsonPath().getLong("id");

    // Update with null description
    given()
        .cookie("guest_token", guestToken)
        .contentType(ContentType.JSON)
        .body("{\"name\":\"Room with Null Description\",\"description\":null}")
        .when()
        .put("/api/rooms/" + roomId)
        .then()
        .statusCode(200)
        .body("name", equalTo("Room with Null Description"))
        .body("description", nullValue());
  }

  @Test
  public void testCreateMultipleRoomsSameUser() {
    // Create first room
    Response room1 =
        given()
            .cookie("guest_token", guestToken)
            .contentType(ContentType.JSON)
            .body("{\"name\":\"Living Room\",\"description\":\"First room\"}")
            .post("/api/rooms")
            .then()
            .statusCode(anyOf(is(200), is(201)))
            .extract()
            .response();
    Long room1Id = room1.jsonPath().getLong("id");

    // Create second room
    Response room2 =
        given()
            .cookie("guest_token", guestToken)
            .contentType(ContentType.JSON)
            .body("{\"name\":\"Bedroom\",\"description\":\"Second room\"}")
            .post("/api/rooms")
            .then()
            .statusCode(anyOf(is(200), is(201)))
            .extract()
            .response();
    Long room2Id = room2.jsonPath().getLong("id");

    // Create third room
    Response room3 =
        given()
            .cookie("guest_token", guestToken)
            .contentType(ContentType.JSON)
            .body("{\"name\":\"Kitchen\",\"description\":\"Third room\"}")
            .post("/api/rooms")
            .then()
            .statusCode(anyOf(is(200), is(201)))
            .extract()
            .response();
    Long room3Id = room3.jsonPath().getLong("id");

    // Verify all rooms are in my rooms
    given()
        .cookie("guest_token", guestToken)
        .when()
        .get("/api/rooms/my")
        .then()
        .statusCode(200)
        .body("$", hasSize(greaterThanOrEqualTo(3)))
        .body("findAll { it.id == " + room1Id + " }.size()", equalTo(1))
        .body("findAll { it.id == " + room2Id + " }.size()", equalTo(1))
        .body("findAll { it.id == " + room3Id + " }.size()", equalTo(1));
  }

  @Test
  public void testRoomNameWithEmojisAndUnicode() {
    given()
        .cookie("guest_token", guestToken)
        .contentType(ContentType.JSON)
        .body("{\"name\":\"Room 🏠 with Unicode 日本語\",\"description\":\"Testing unicode support\"}")
        .when()
        .post("/api/rooms")
        .then()
        .statusCode(anyOf(is(200), is(201)))
        .body("name", equalTo("Room 🏠 with Unicode 日本語"));
  }

  @Test
  public void testLongRoomName() {
    String longName = "A".repeat(250); // Test with 250 characters
    given()
        .cookie("guest_token", guestToken)
        .contentType(ContentType.JSON)
        .body("{\"name\":\"" + longName + "\",\"description\":\"Testing long name\"}")
        .when()
        .post("/api/rooms")
        .then()
        .statusCode(anyOf(is(200), is(201), is(400))); // May fail validation or succeed
  }

  @Test
  public void testEmptyRoomName() {
    given()
        .cookie("guest_token", guestToken)
        .contentType(ContentType.JSON)
        .body("{\"name\":\"\",\"description\":\"Empty name test\"}")
        .when()
        .post("/api/rooms")
        .then()
        .statusCode(anyOf(is(200), is(201), is(400))); // May fail validation or succeed
  }
}
