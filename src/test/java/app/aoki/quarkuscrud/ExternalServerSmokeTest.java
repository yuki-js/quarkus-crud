package app.aoki.quarkuscrud;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import app.aoki.quarkuscrud.util.TestServerConfig;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.*;

/**
 * Simple smoke tests for the external production server. These tests verify basic functionality
 * without requiring complex state management.
 *
 * <p>To run these tests:
 *
 * <pre>
 * ./gradlew test --tests "ExternalServerSmokeTest"
 * ./gradlew test --tests "ExternalServerSmokeTest" -Dtest.external.url=https://your-server.com
 * </pre>
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ExternalServerSmokeTest {

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
  public void testHealthEndpoint() {
    given()
        .when()
        .get("/healthz")
        .then()
        .statusCode(200)
        .body("status", equalTo("UP"))
        .body("service", equalTo("quarkus-crud"));
  }

  @Test
  @Order(2)
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

    String jwtToken = authHeader.substring(7);
    assertFalse(jwtToken.isEmpty(), "JWT token should not be empty");

    // Verify JWT has the expected structure (3 parts separated by dots)
    String[] parts = jwtToken.split("\\.");
    assertEquals(3, parts.length, "JWT token should have 3 parts");
  }

  @Test
  @Order(3)
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

  @Test
  @Order(4)
  public void testOpenApiEndpoint() {
    given()
        .when()
        .get("/openapi")
        .then()
        .statusCode(200)
        .contentType(anyOf(containsString("yaml"), containsString("text")));
  }

  @Test
  @Order(5)
  public void testSwaggerUIEndpoint() {
    given().when().get("/swagger-ui").then().statusCode(anyOf(is(200), is(301), is(302)));
  }
}
