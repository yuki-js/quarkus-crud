package app.aoki;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

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
 * that the actual API behavior matches the documented OpenAPI spec for the Events/Profiles/Friendships API.
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class OpenApiContractTest {

  private static OpenApiValidationFilter validationFilter;
  private static String jwtToken;
  private static Long testEventId;
  private static Long userId;

  @BeforeAll
  public static void setupValidationFilter() throws IOException {
    // Create validation filter using the OpenAPI spec from resources
    URL specUrl = OpenApiContractTest.class.getClassLoader().getResource("META-INF/openapi.yaml");
    if (specUrl == null) {
      throw new IllegalStateException("OpenAPI spec file not found");
    }

    // Read the spec file content
    String specContent = new String(specUrl.openStream().readAllBytes(), StandardCharsets.UTF_8);

    // Create validator with security validation disabled for bearer auth  
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
    userId = response.jsonPath().getLong("id");
    Assertions.assertNotNull(jwtToken, "JWT token should be set in Authorization header");
  }

  @Test
  @Order(2)
  public void testGetCurrentUserContract() {
    // GET /api/me should conform to OpenAPI spec
    given()
        .filter(validationFilter)
        .header("Authorization", "Bearer " + jwtToken)
        .when()
        .get("/api/me")
        .then()
        .statusCode(200);
  }

  @Test
  @Order(3)
  public void testGetUserByIdContract() {
    // GET /api/users/{userId} should conform to OpenAPI spec
    given()
        .filter(validationFilter)
        .header("Authorization", "Bearer " + jwtToken)
        .when()
        .get("/api/users/" + userId)
        .then()
        .statusCode(200);
  }

  @Test
  @Order(4)
  public void testCreateEventContract() {
    // POST /api/events should conform to OpenAPI spec
    var response =
        given()
            .filter(validationFilter)
            .header("Authorization", "Bearer " + jwtToken)
            .contentType(ContentType.JSON)
            .body(
                "{\"meta\":{\"name\":\"Contract Test Event\"},\"expiresAt\":\"2025-12-31T23:59:59Z\"}")
            .when()
            .post("/api/events")
            .then()
            .statusCode(201)
            .extract()
            .response();

    testEventId = response.jsonPath().getLong("id");
    Assertions.assertNotNull(testEventId, "Event ID should be set in response");
  }

  @Test
  @Order(5)
  public void testGetEventByIdContract() {
    // GET /api/events/{eventId} should conform to OpenAPI spec
    given()
        .filter(validationFilter)
        .header("Authorization", "Bearer " + jwtToken)
        .when()
        .get("/api/events/" + testEventId)
        .then()
        .statusCode(200);
  }

  @Test
  @Order(6)
  public void testListEventsByUserContract() {
    // GET /api/users/{userId}/events should conform to OpenAPI spec
    given()
        .filter(validationFilter)
        .header("Authorization", "Bearer " + jwtToken)
        .when()
        .get("/api/users/" + userId + "/events")
        .then()
        .statusCode(200);
  }

  @Test
  @Order(7)
  public void testUpdateProfileContract() {
    // PUT /api/me/profile should conform to OpenAPI spec
    given()
        .filter(validationFilter)
        .header("Authorization", "Bearer " + jwtToken)
        .contentType(ContentType.JSON)
        .body("{\"profileData\":{\"displayName\":\"Contract Test User\"}}")
        .when()
        .put("/api/me/profile")
        .then()
        .statusCode(200);
  }

  @Test
  @Order(8)
  public void testGetMyProfileContract() {
    // GET /api/me/profile should conform to OpenAPI spec
    given()
        .filter(validationFilter)
        .header("Authorization", "Bearer " + jwtToken)
        .when()
        .get("/api/me/profile")
        .then()
        .statusCode(200);
  }

  @Test
  @Order(9)
  public void testGetUserProfileContract() {
    // GET /api/users/{userId}/profile should conform to OpenAPI spec  
    given()
        .filter(validationFilter)
        .header("Authorization", "Bearer " + jwtToken)
        .when()
        .get("/api/users/" + userId + "/profile")
        .then()
        .statusCode(anyOf(is(200), is(404))); // Profile may not exist initially
  }

  @Test
  @Order(10)
  public void testJoinEventByCodeContract() {
    // Create another user to join the event
    var user2Response =
        given()
            .filter(validationFilter)
            .contentType(ContentType.JSON)
            .when()
            .post("/api/auth/guest")
            .then()
            .extract()
            .response();
    
    String user2Token = user2Response.getHeader("Authorization").substring(7);
    
    // Get the invitation code from the event
    String invitationCode = 
        given()
            .header("Authorization", "Bearer " + jwtToken)
            .when()
            .get("/api/events/" + testEventId)
            .then()
            .extract()
            .jsonPath()
            .getString("invitationCode");
    
    // POST /api/events/join-by-code should conform to OpenAPI spec
    given()
        .filter(validationFilter)
        .header("Authorization", "Bearer " + user2Token)
        .contentType(ContentType.JSON)
        .body("{\"invitationCode\":\"" + invitationCode + "\"}")
        .when()
        .post("/api/events/join-by-code")
        .then()
        .statusCode(201);
  }

  @Test
  @Order(11)
  public void testListEventAttendeesContract() {
    // GET /api/events/{eventId}/attendees should conform to OpenAPI spec
    given()
        .filter(validationFilter)
        .header("Authorization", "Bearer " + jwtToken)
        .when()
        .get("/api/events/" + testEventId + "/attendees")
        .then()
        .statusCode(200);
  }

  @Test
  @Order(12)
  public void testUnauthorizedAccessContract() {
    // Requests without authentication should conform to OpenAPI spec (401 response)
    given()
        .filter(validationFilter)
        .when()
        .get("/api/me")
        .then()
        .statusCode(401);
  }
}
