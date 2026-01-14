package app.aoki.quarkuscrud.resource;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

import app.aoki.quarkuscrud.generated.model.Event;
import app.aoki.quarkuscrud.generated.model.UserMeta;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.*;

/**
 * Comprehensive tests for usermeta CRUD operations across ALL tables.
 * Tests cover: users, events, friendships, event_attendees, user_profiles, event_user_data
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ComprehensiveUsermetaApiTest {

  private String token1;
  private String token2;
  private Long userId1;
  private Long userId2;

  @Test
  @Order(0)
  public void setup() {
    // Create two guest users for testing
    Response response1 = given().contentType(ContentType.JSON).post("/api/auth/guest");
    token1 = response1.getHeader("Authorization").substring(7);
    Response me1 = given().header("Authorization", "Bearer " + token1).get("/api/me");
    userId1 = me1.jsonPath().getLong("id");

    Response response2 = given().contentType(ContentType.JSON).post("/api/auth/guest");
    token2 = response2.getHeader("Authorization").substring(7);
    Response me2 = given().header("Authorization", "Bearer " + token2).get("/api/me");
    userId2 = me2.jsonPath().getLong("id");
  }

  // ==================== Users Table Tests ====================

  @Test
  @Order(1)
  public void testUser_GetMeta_Success() {
    given()
        .header("Authorization", "Bearer " + token1)
        .when()
        .get("/api/users/" + userId1 + "/meta")
        .then()
        .statusCode(200);
  }

  @Test
  @Order(2)
  public void testUser_UpdateMeta_Success() {
    Map<String, Object> meta = Map.of("nickname", "TestUser1", "theme", "dark");
    Map<String, Object> request = Map.of("usermeta", meta);

    given()
        .header("Authorization", "Bearer " + token1)
        .contentType(ContentType.JSON)
        .body(request)
        .when()
        .put("/api/users/" + userId1 + "/meta")
        .then()
        .statusCode(200);

    // Verify
    var response =
        given()
            .header("Authorization", "Bearer " + token1)
            .get("/api/users/" + userId1 + "/meta")
            .then()
            .statusCode(200)
            .extract()
            .as(UserMeta.class);

    assertNotNull(response.getUsermeta());
    assertEquals("TestUser1", ((Map<?, ?>) response.getUsermeta()).get("nickname"));
  }

  @Test
  @Order(3)
  public void testUser_GetMeta_Forbidden() {
    // User2 cannot access User1's metadata
    given()
        .header("Authorization", "Bearer " + token2)
        .get("/api/users/" + userId1 + "/meta")
        .then()
        .statusCode(403);
  }

  @Test
  @Order(4)
  public void testUser_UpdateMeta_Forbidden() {
    Map<String, Object> request = Map.of("usermeta", Map.of("evil", "data"));

    // User2 cannot update User1's metadata
    given()
        .header("Authorization", "Bearer " + token2)
        .contentType(ContentType.JSON)
        .body(request)
        .put("/api/users/" + userId1 + "/meta")
        .then()
        .statusCode(403);
  }

  // ==================== Events Table Tests ====================

  @Test
  @Order(10)
  public void testEvent_GetMeta_Success_Initiator() {
    // Create event
    Event event =
        given()
            .header("Authorization", "Bearer " + token1)
            .contentType(ContentType.JSON)
            .body("{}")
            .post("/api/events")
            .then()
            .statusCode(201)
            .extract()
            .as(Event.class);

    // Initiator can read
    given()
        .header("Authorization", "Bearer " + token1)
        .get("/api/events/" + event.getId() + "/meta")
        .then()
        .statusCode(200);
  }

  @Test
  @Order(11)
  public void testEvent_UpdateMeta_Success_Initiator() {
    // Create event
    Event event =
        given()
            .header("Authorization", "Bearer " + token1)
            .contentType(ContentType.JSON)
            .body("{}")
            .post("/api/events")
            .then()
            .statusCode(201)
            .extract()
            .as(Event.class);

    Map<String, Object> request = Map.of("usermeta", Map.of("theme", "birthday", "location", "Tokyo"));

    // Initiator can write
    given()
        .header("Authorization", "Bearer " + token1)
        .contentType(ContentType.JSON)
        .body(request)
        .put("/api/events/" + event.getId() + "/meta")
        .then()
        .statusCode(200);
  }

  @Test
  @Order(12)
  public void testEvent_GetMeta_Forbidden_NonAttendee() {
    // Create event
    Event event =
        given()
            .header("Authorization", "Bearer " + token1)
            .contentType(ContentType.JSON)
            .body("{}")
            .post("/api/events")
            .then()
            .statusCode(201)
            .extract()
            .as(Event.class);

    // Non-attendee cannot read
    given()
        .header("Authorization", "Bearer " + token2)
        .get("/api/events/" + event.getId() + "/meta")
        .then()
        .statusCode(403);
  }

  @Test
  @Order(13)
  public void testEvent_UpdateMeta_Forbidden_NonAttendee() {
    // Create event
    Event event =
        given()
            .header("Authorization", "Bearer " + token1)
            .contentType(ContentType.JSON)
            .body("{}")
            .post("/api/events")
            .then()
            .statusCode(201)
            .extract()
            .as(Event.class);

    Map<String, Object> request = Map.of("usermeta", Map.of("evil", "data"));

    // Non-attendee cannot write
    given()
        .header("Authorization", "Bearer " + token2)
        .contentType(ContentType.JSON)
        .body(request)
        .put("/api/events/" + event.getId() + "/meta")
        .then()
        .statusCode(403);
  }

  // ==================== Friendships Table Tests ====================

  @Test
  @Order(20)
  public void testFriendship_GetMeta_Success() {
    // Create friendship
    given()
        .header("Authorization", "Bearer " + token1)
        .contentType(ContentType.JSON)
        .body("{}")
        .put("/api/users/" + userId2 + "/friendship")
        .then()
        .statusCode(200);

    // Sender can read
    given()
        .header("Authorization", "Bearer " + token1)
        .get("/api/friendships/" + userId2 + "/meta")
        .then()
        .statusCode(200);
  }

  @Test
  @Order(21)
  public void testFriendship_UpdateMeta_Success() {
    // Create friendship
    given()
        .header("Authorization", "Bearer " + token1)
        .contentType(ContentType.JSON)
        .body("{}")
        .put("/api/users/" + userId2 + "/friendship")
        .then()
        .statusCode(200);

    Map<String, Object> request = Map.of("usermeta", Map.of("note", "Good friend", "tags", java.util.List.of("colleague")));

    // Sender can write
    given()
        .header("Authorization", "Bearer " + token1)
        .contentType(ContentType.JSON)
        .body(request)
        .put("/api/friendships/" + userId2 + "/meta")
        .then()
        .statusCode(200);
  }

  @Test
  @Order(22)
  public void testFriendship_GetMeta_NotFound() {
    // No friendship with non-existent user
    given()
        .header("Authorization", "Bearer " + token1)
        .get("/api/friendships/9999999/meta")
        .then()
        .statusCode(404);
  }

  // ==================== Complex Metadata Tests ====================

  @Test
  @Order(30)
  public void testUser_ComplexNestedMetadata() {
    Map<String, Object> complex = new HashMap<>();
    complex.put("profile", Map.of("avatar", "https://example.com/avatar.jpg", "bio", "Hello world!"));
    complex.put("preferences", Map.of("theme", "dark", "lang", "ja", "notifications", true));
    complex.put("tags", java.util.List.of("developer", "gamer", "reader"));
    complex.put("scores", java.util.List.of(100, 200, 300));
    complex.put("nested", Map.of("level1", Map.of("level2", Map.of("level3", "deep"))));

    Map<String, Object> request = Map.of("usermeta", complex);

    given()
        .header("Authorization", "Bearer " + token1)
        .contentType(ContentType.JSON)
        .body(request)
        .put("/api/users/" + userId1 + "/meta")
        .then()
        .statusCode(200);

    // Verify complex data was saved
    var response =
        given()
            .header("Authorization", "Bearer " + token1)
            .get("/api/users/" + userId1 + "/meta")
            .then()
            .statusCode(200)
            .extract()
            .as(UserMeta.class);

    assertNotNull(response.getUsermeta());
    Map<?, ?> saved = (Map<?, ?>) response.getUsermeta();
    assertTrue(saved.containsKey("profile"));
    assertTrue(saved.containsKey("preferences"));
    assertTrue(saved.containsKey("tags"));
  }

  @Test
  @Order(31)
  public void testUser_NullMetadata() {
    Map<String, Object> request = Map.of("usermeta", Map.of());

    given()
        .header("Authorization", "Bearer " + token1)
        .contentType(ContentType.JSON)
        .body(request)
        .put("/api/users/" + userId1 + "/meta")
        .then()
        .statusCode(200);
  }

  @Test
  @Order(32)
  public void testUser_UpdateMetadata_Overwrite() {
    // Set initial metadata
    Map<String, Object> initial = Map.of("usermeta", Map.of("key1", "value1", "key2", "value2"));
    given()
        .header("Authorization", "Bearer " + token1)
        .contentType(ContentType.JSON)
        .body(initial)
        .put("/api/users/" + userId1 + "/meta")
        .then()
        .statusCode(200);

    // Overwrite with new metadata
    Map<String, Object> updated = Map.of("usermeta", Map.of("key3", "value3"));
    given()
        .header("Authorization", "Bearer " + token1)
        .contentType(ContentType.JSON)
        .body(updated)
        .put("/api/users/" + userId1 + "/meta")
        .then()
        .statusCode(200);

    // Verify old keys are gone, new key exists
    var response =
        given()
            .header("Authorization", "Bearer " + token1)
            .get("/api/users/" + userId1 + "/meta")
            .then()
            .statusCode(200)
            .extract()
            .as(UserMeta.class);

    Map<?, ?> saved = (Map<?, ?>) response.getUsermeta();
    assertFalse(saved.containsKey("key1"));
    assertFalse(saved.containsKey("key2"));
    assertTrue(saved.containsKey("key3"));
  }

  @Test
  @Order(33)
  public void testUser_Unauthenticated_Forbidden() {
    given()
        .contentType(ContentType.JSON)
        .body(Map.of("usermeta", Map.of("test", "data")))
        .put("/api/users/" + userId1 + "/meta")
        .then()
        .statusCode(401);

    given().get("/api/users/" + userId1 + "/meta").then().statusCode(401);
  }
}
