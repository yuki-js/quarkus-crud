package app.aoki;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Integration tests for authentication functionality. Tests the complete authentication flow
 * including guest user creation and validation.
 */
@QuarkusTest
@TestMethodOrder(OrderAnnotation.class)
public class AuthenticationIntegrationTest {

  private static String guestTokenCookie;

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
            .extract()
            .response();

    // Extract guest token cookie for subsequent tests
    guestTokenCookie = response.getCookie("guest_token");
    assertNotNull(guestTokenCookie, "Guest token cookie should be set");
    assertFalse(guestTokenCookie.isEmpty(), "Guest token cookie should not be empty");
  }

  @Test
  @Order(2)
  public void testGetCurrentUserWithValidToken() {
    given()
        .cookie("guest_token", guestTokenCookie)
        .when()
        .get("/api/auth/me")
        .then()
        .statusCode(200)
        .body("id", notNullValue())
        .body("createdAt", notNullValue());
  }

  @Test
  @Order(3)
  public void testGetCurrentUserWithoutToken() {
    given()
        .when()
        .get("/api/auth/me")
        .then()
        .statusCode(401)
        .body("error", equalTo("No guest token found"));
  }

  @Test
  @Order(4)
  public void testGetCurrentUserWithInvalidToken() {
    given()
        .cookie("guest_token", "invalid-token-12345")
        .when()
        .get("/api/auth/me")
        .then()
        .statusCode(401)
        .body("error", equalTo("Invalid guest token"));
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
            .extract()
            .response();

    String token1 = response1.getCookie("guest_token");
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
            .extract()
            .response();

    String token2 = response2.getCookie("guest_token");
    Long userId2 = response2.jsonPath().getLong("id");

    // Verify tokens are different
    assertNotEquals(token1, token2, "Guest tokens should be unique");

    // Verify user IDs are different
    assertNotEquals(userId1, userId2, "User IDs should be unique");
  }
}
