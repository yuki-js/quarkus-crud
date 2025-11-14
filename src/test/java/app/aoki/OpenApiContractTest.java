package app.aoki;

import static io.restassured.RestAssured.given;

import com.atlassian.oai.validator.OpenApiInteractionValidator;
import com.atlassian.oai.validator.restassured.OpenApiValidationFilter;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.*;

/**
 * Contract tests that validate API responses against the OpenAPI specification. These tests ensure
 * that the actual API behavior matches the documented OpenAPI spec.
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class OpenApiContractTest {

  private static OpenApiValidationFilter validationFilter;
  private static String jwtToken;
  private static Long testRoomId;

  @BeforeAll
  public static void setupValidationFilter() throws IOException {
    // Create validation filter using the OpenAPI spec from resources
    URL specUrl = OpenApiContractTest.class.getClassLoader().getResource("META-INF/openapi.yaml");
    if (specUrl == null) {
      throw new IllegalStateException("OpenAPI spec file not found");
    }

    // Read the spec file content
    String specContent = new String(specUrl.openStream().readAllBytes(), StandardCharsets.UTF_8);

    // Create validator with security validation disabled for JWT Bearer auth
    // Some OpenAPI validators may not properly recognize Authorization headers in all cases
    OpenApiInteractionValidator validator =
        OpenApiInteractionValidator.createForInlineApiSpecification(specContent)
            .withLevelResolver(
                com.atlassian.oai.validator.report.LevelResolver.create()
                    .withLevel(
                        "validation.request.security.missing",
                        com.atlassian.oai.validator.report.ValidationReport.Level.IGNORE)
                    .build())
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

    jwtToken = response.getHeader("Authorization").substring(7);
    Assertions.assertNotNull(jwtToken, "JWT token should be set in Authorization header");
  }

  @Test
  @Order(2)
  public void testGetCurrentUserContract() {
    // GET /api/auth/me should conform to OpenAPI spec
    given()
        .filter(validationFilter)
        .header("Authorization", "Bearer " + jwtToken)
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
            .header("Authorization", "Bearer " + jwtToken)
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
        .header("Authorization", "Bearer " + jwtToken)
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
        .header("Authorization", "Bearer " + jwtToken)
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
        .header("Authorization", "Bearer " + jwtToken)
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
