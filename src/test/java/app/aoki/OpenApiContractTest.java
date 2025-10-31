package app.aoki;

import static io.restassured.RestAssured.given;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.restassured.OpenApiValidationFilter;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;

/**
 * Contract tests that validate API responses against the OpenAPI specification. These tests ensure
 * that the actual API behavior matches the documented OpenAPI spec.
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class OpenApiContractTest {

  private static OpenApiValidationFilter validationFilter;
  private static String guestToken;
  private static Long testRoomId;

  @BeforeAll
  public static void setupValidationFilter() {
    // Create validation filter using the OpenAPI spec from resources
    OpenApiInteractionValidator validator =
        OpenApiInteractionValidator.createForInlineApiSpecification(
                OpenApiContractTest.class
                    .getClassLoader()
                    .getResource("META-INF/openapi.yaml"))
            .build();
    validationFilter = new OpenApiValidationFilter(validator);
  }

  @Test
  @Order(1)
  public void testCreateGuestUserContract() {
    // POST /api/auth/guest should conform to OpenAPI spec
    var response =
        given()
            .filter(validationFilter)
            .contentType(ContentType.JSON)
            .when()
            .post("/api/auth/guest")
            .then()
            .statusCode(200)
            .extract()
            .response();

    guestToken = response.getCookie("guest_token");
    Assertions.assertNotNull(guestToken, "Guest token cookie should be set");
  }

  @Test
  @Order(2)
  public void testGetCurrentUserContract() {
    // GET /api/auth/me should conform to OpenAPI spec
    given()
        .filter(validationFilter)
        .cookie("guest_token", guestToken)
        .when()
        .get("/api/auth/me")
        .then()
        .statusCode(200);
  }

  @Test
  @Order(3)
  public void testGetAllRoomsContract() {
    // GET /api/rooms should conform to OpenAPI spec
    given().filter(validationFilter).when().get("/api/rooms").then().statusCode(200);
  }

  @Test
  @Order(4)
  public void testCreateRoomContract() {
    // POST /api/rooms should conform to OpenAPI spec
    var response =
        given()
            .filter(validationFilter)
            .cookie("guest_token", guestToken)
            .contentType(ContentType.JSON)
            .body("{\"name\":\"Contract Test Room\",\"description\":\"Testing OpenAPI contract\"}")
            .when()
            .post("/api/rooms")
            .then()
            .statusCode(201)
            .extract()
            .response();

    testRoomId = response.jsonPath().getLong("id");
    Assertions.assertNotNull(testRoomId, "Created room should have an ID");
  }

  @Test
  @Order(5)
  public void testGetRoomByIdContract() {
    // GET /api/rooms/{id} should conform to OpenAPI spec
    given().filter(validationFilter).when().get("/api/rooms/" + testRoomId).then().statusCode(200);
  }

  @Test
  @Order(6)
  public void testGetMyRoomsContract() {
    // GET /api/rooms/my should conform to OpenAPI spec
    given()
        .filter(validationFilter)
        .cookie("guest_token", guestToken)
        .when()
        .get("/api/rooms/my")
        .then()
        .statusCode(200);
  }

  @Test
  @Order(7)
  public void testUpdateRoomContract() {
    // PUT /api/rooms/{id} should conform to OpenAPI spec
    given()
        .filter(validationFilter)
        .cookie("guest_token", guestToken)
        .contentType(ContentType.JSON)
        .body(
            "{\"name\":\"Updated Contract Test Room\",\"description\":\"Updated via contract"
                + " test\"}")
        .when()
        .put("/api/rooms/" + testRoomId)
        .then()
        .statusCode(200);
  }

  @Test
  @Order(8)
  public void testDeleteRoomContract() {
    // DELETE /api/rooms/{id} should conform to OpenAPI spec
    given()
        .filter(validationFilter)
        .cookie("guest_token", guestToken)
        .when()
        .delete("/api/rooms/" + testRoomId)
        .then()
        .statusCode(204);
  }

  @Test
  @Order(9)
  public void testGetNonExistentRoomContract() {
    // GET /api/rooms/{id} for non-existent room should conform to OpenAPI spec (404)
    given().filter(validationFilter).when().get("/api/rooms/999999").then().statusCode(404);
  }

  @Test
  @Order(10)
  public void testUnauthorizedAccessContract() {
    // POST /api/rooms without auth should conform to OpenAPI spec (401)
    given()
        .filter(validationFilter)
        .contentType(ContentType.JSON)
        .body("{\"name\":\"Unauthorized Room\",\"description\":\"Should fail\"}")
        .when()
        .post("/api/rooms")
        .then()
        .statusCode(401);
  }
}
