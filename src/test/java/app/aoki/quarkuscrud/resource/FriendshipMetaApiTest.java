package app.aoki.quarkuscrud.resource;

import app.aoki.quarkuscrud.entity.User;
import app.aoki.quarkuscrud.service.UserService;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FriendshipMetaApiTest {

  @Inject UserService userService;

  @Test
  @Order(1)
  public void testGetFriendshipMeta_Success() {
    User sender = userService.createAnonymousUser();
    User recipient = userService.createAnonymousUser();

    // Create friendship
    Map<String, Object> friendshipRequest = new HashMap<>();
    friendshipRequest.put("meta", Map.of("note", "Met at conference"));

    RestAssured.given()
        .auth()
        .oauth2(sender.getAnonymousJwt())
        .contentType(ContentType.JSON)
        .body(friendshipRequest)
        .when()
        .put("/api/users/" + recipient.getId() + "/friendship")
        .then()
        .statusCode(200);

    // Sender can read friendship metadata
    RestAssured.given()
        .auth()
        .oauth2(sender.getAnonymousJwt())
        .when()
        .get("/api/friendships/" + recipient.getId() + "/meta")
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON);
  }

  @Test
  @Order(2)
  public void testUpdateFriendshipMeta_Success() {
    User sender = userService.createAnonymousUser();
    User recipient = userService.createAnonymousUser();

    // Create friendship
    RestAssured.given()
        .auth()
        .oauth2(sender.getAnonymousJwt())
        .contentType(ContentType.JSON)
        .body("{}")
        .when()
        .put("/api/users/" + recipient.getId() + "/friendship")
        .then()
        .statusCode(200);

    Map<String, Object> meta = new HashMap<>();
    meta.put("note", "Good friend");
    meta.put("tags", java.util.List.of("colleague", "mentor"));

    Map<String, Object> request = new HashMap<>();
    request.put("usermeta", meta);

    // Sender can update friendship metadata
    RestAssured.given()
        .auth()
        .oauth2(sender.getAnonymousJwt())
        .contentType(ContentType.JSON)
        .body(request)
        .when()
        .put("/api/friendships/" + recipient.getId() + "/meta")
        .then()
        .statusCode(200);
  }

  @Test
  @Order(3)
  public void testGetFriendshipMeta_NotFound() {
    User sender = userService.createAnonymousUser();
    User notFriend = userService.createAnonymousUser();

    // No friendship exists
    RestAssured.given()
        .auth()
        .oauth2(sender.getAnonymousJwt())
        .when()
        .get("/api/friendships/" + notFriend.getId() + "/meta")
        .then()
        .statusCode(404);
  }

  @Test
  @Order(4)
  public void testUpdateFriendshipMeta_NotFound() {
    User sender = userService.createAnonymousUser();
    User notFriend = userService.createAnonymousUser();

    Map<String, Object> request = new HashMap<>();
    request.put("usermeta", Map.of("note", "test"));

    // No friendship exists
    RestAssured.given()
        .auth()
        .oauth2(sender.getAnonymousJwt())
        .contentType(ContentType.JSON)
        .body(request)
        .when()
        .put("/api/friendships/" + notFriend.getId() + "/meta")
        .then()
        .statusCode(404);
  }
}
