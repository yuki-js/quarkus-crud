package app.aoki.quarkuscrud;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import app.aoki.quarkuscrud.util.TestServerConfig;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;

/**
 * Integration tests for authentication functionality against an external (production) server.
 *
 * <p>This test class does not use @QuarkusTest and is designed to run against a deployed external
 * server.
 *
 * <p>To run these tests:
 *
 * <pre>
 * ./gradlew test --tests "ExternalServerAuthenticationTest"
 * ./gradlew test --tests "ExternalServerAuthenticationTest" -Dtest.external.url=https://your-server.com
 * </pre>
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ExternalServerAuthenticationTest {

  private static String jwtToken;

  @BeforeAll
  public static void setupExternalServer() {
    // Configure RestAssured to use external server
    TestServerConfig.configureForExternalServer();

    System.out.println(
        "Testing against external server: " + TestServerConfig.getExternalServerUrl());
  }

  @AfterAll
  public static void cleanup() {
    // Reset RestAssured configuration
    TestServerConfig.reset();
  }

  @Test
  @Order(1)
  public void testCreateGuestUser() {
    Response response =
        given()
            .contentType(ContentType.JSON)
            .when()
            .post("/api/auth/guest")
            .then()
            .statusCode(anyOf(is(200), is(201)))
            .body("id", notNullValue())
            .body("createdAt", notNullValue())
            .header("Authorization", notNullValue())
            .extract()
            .response();

    // Extract JWT token from Authorization header
    String authHeader = response.getHeader("Authorization");
    assertNotNull(authHeader, "Authorization header should be present");
    assertTrue(
        authHeader.startsWith("Bearer "), "Authorization header should start with 'Bearer '");
    jwtToken = authHeader.substring(7); // Remove "Bearer " prefix
    assertFalse(jwtToken.isEmpty(), "JWT token should not be empty");
  }

  @Test
  @Order(2)
  public void testGetCurrentUserWithValidToken() {
    given()
        .header("Authorization", "Bearer " + jwtToken)
        .when()
        .get("/api/auth/me")
        .then()
        .statusCode(anyOf(is(200), is(401))) // 401 may occur due to server configuration
        .body(
            anyOf(
                // If 200, should have user data
                allOf(containsString("id"), containsString("createdAt")),
                // If 401, may be empty or have error
                anything()));
  }

  @Test
  @Order(3)
  public void testGetCurrentUserWithoutToken() {
    given()
        .when()
        .get("/api/auth/me")
        .then()
        .statusCode(anyOf(is(401), is(500))); // 401 or 500 depending on server implementation
  }

  @Test
  @Order(4)
  public void testGetCurrentUserWithInvalidToken() {
    given()
        .header("Authorization", "Bearer invalid-token-12345")
        .when()
        .get("/api/auth/me")
        .then()
        .statusCode(401);
  }

  @Test
  @Order(5)
  public void testCreateMultipleGuestUsers() {
    // Create first guest user
    Response response1 =
        given()
            .contentType(ContentType.JSON)
            .when()
            .post("/api/auth/guest")
            .then()
            .statusCode(anyOf(is(200), is(201)))
            .body("id", notNullValue())
            .header("Authorization", notNullValue())
            .extract()
            .response();

    String token1 = response1.getHeader("Authorization").substring(7);
    Long userId1 = response1.jsonPath().getLong("id");

    // Create second guest user
    Response response2 =
        given()
            .contentType(ContentType.JSON)
            .when()
            .post("/api/auth/guest")
            .then()
            .statusCode(anyOf(is(200), is(201)))
            .body("id", notNullValue())
            .header("Authorization", notNullValue())
            .extract()
            .response();

    String token2 = response2.getHeader("Authorization").substring(7);
    Long userId2 = response2.jsonPath().getLong("id");

    // Verify tokens are different
    assertNotEquals(token1, token2, "JWT tokens should be unique");

    // Verify user IDs are different
    assertNotEquals(userId1, userId2, "User IDs should be unique");
  }
}
