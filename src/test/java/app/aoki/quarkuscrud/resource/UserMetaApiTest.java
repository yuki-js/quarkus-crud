package app.aoki.quarkuscrud.resource;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

import app.aoki.quarkuscrud.generated.model.MetaData;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UserMetaApiTest {

  private String createGuestUserAndGetToken() {
    Response response = given().contentType(ContentType.JSON).post("/api/auth/guest");
    return response.getHeader("Authorization").substring(7);
  }

  @Test
  @Order(1)
  public void testGetUserMeta_Success() {
    String token = createGuestUserAndGetToken();
    Response meResponse =
        given().header("Authorization", "Bearer " + token).when().get("/api/me");
    Long userId = meResponse.jsonPath().getLong("id");

    given()
        .header("Authorization", "Bearer " + token)
        .when()
        .get("/api/users/" + userId + "/meta")
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON);
  }

  @Test
  @Order(2)
  public void testUpdateUserMeta_Success() {
    String token = createGuestUserAndGetToken();
    Response meResponse =
        given().header("Authorization", "Bearer " + token).when().get("/api/me");
    Long userId = meResponse.jsonPath().getLong("id");

    Map<String, Object> meta = new HashMap<>();
    meta.put("nickname", "TestUser");
    meta.put("preferences", Map.of("theme", "dark", "language", "ja"));

    Map<String, Object> request = new HashMap<>();
    request.put("usermeta", meta);

    given()
        .header("Authorization", "Bearer " + token)
        .contentType(ContentType.JSON)
        .body(request)
        .when()
        .put("/api/users/" + userId + "/meta")
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON);

    // Verify the metadata was saved
    var response =
        given()
            .header("Authorization", "Bearer " + token)
            .when()
            .get("/api/users/" + userId + "/meta")
            .then()
            .statusCode(200)
            .extract()
            .as(MetaData.class);

    assertNotNull(response.getUsermeta());
    @SuppressWarnings("unchecked")
    Map<String, Object> savedMeta = (Map<String, Object>) response.getUsermeta();
    assertEquals("TestUser", savedMeta.get("nickname"));
  }

  @Test
  @Order(3)
  public void testGetUserMeta_Forbidden() {
    String token1 = createGuestUserAndGetToken();
    Response me1Response =
        given().header("Authorization", "Bearer " + token1).when().get("/api/me");
    Long userId1 = me1Response.jsonPath().getLong("id");

    String token2 = createGuestUserAndGetToken();

    // User2 tries to access User1's metadata
    given()
        .header("Authorization", "Bearer " + token2)
        .when()
        .get("/api/users/" + userId1 + "/meta")
        .then()
        .statusCode(403);
  }

  @Test
  @Order(4)
  public void testUpdateUserMeta_Forbidden() {
    String token1 = createGuestUserAndGetToken();
    Response me1Response =
        given().header("Authorization", "Bearer " + token1).when().get("/api/me");
    Long userId1 = me1Response.jsonPath().getLong("id");

    String token2 = createGuestUserAndGetToken();

    Map<String, Object> request = new HashMap<>();
    request.put("usermeta", Map.of("malicious", "data"));

    // User2 tries to update User1's metadata
    given()
        .header("Authorization", "Bearer " + token2)
        .contentType(ContentType.JSON)
        .body(request)
        .when()
        .put("/api/users/" + userId1 + "/meta")
        .then()
        .statusCode(403);
  }

  @Test
  @Order(5)
  public void testUpdateUserMeta_Unauthenticated() {
    String token = createGuestUserAndGetToken();
    Response meResponse =
        given().header("Authorization", "Bearer " + token).when().get("/api/me");
    Long userId = meResponse.jsonPath().getLong("id");

    Map<String, Object> request = new HashMap<>();
    request.put("usermeta", Map.of("test", "data"));

    given()
        .contentType(ContentType.JSON)
        .body(request)
        .when()
        .put("/api/users/" + userId + "/meta")
        .then()
        .statusCode(401);
  }

  @Test
  @Order(6)
  public void testUpdateUserMeta_NullMeta() {
    String token = createGuestUserAndGetToken();
    Response meResponse =
        given().header("Authorization", "Bearer " + token).when().get("/api/me");
    Long userId = meResponse.jsonPath().getLong("id");

    Map<String, Object> request = new HashMap<>();
    request.put("usermeta", null);

    given()
        .header("Authorization", "Bearer " + token)
        .contentType(ContentType.JSON)
        .body(request)
        .when()
        .put("/api/users/" + userId + "/meta")
        .then()
        .statusCode(200);
  }

  @Test
  @Order(7)
  public void testUpdateUserMeta_ComplexNestedData() {
    String token = createGuestUserAndGetToken();
    Response meResponse =
        given().header("Authorization", "Bearer " + token).when().get("/api/me");
    Long userId = meResponse.jsonPath().getLong("id");

    Map<String, Object> complexMeta = new HashMap<>();
    complexMeta.put("profile", Map.of("avatar", "url", "bio", "Hello world"));
    complexMeta.put("settings", Map.of("notifications", true, "privacy", "public"));
    complexMeta.put("tags", java.util.List.of("developer", "gamer", "reader"));

    Map<String, Object> request = new HashMap<>();
    request.put("usermeta", complexMeta);

    given()
        .header("Authorization", "Bearer " + token)
        .contentType(ContentType.JSON)
        .body(request)
        .when()
        .put("/api/users/" + userId + "/meta")
        .then()
        .statusCode(200);
  }
}
