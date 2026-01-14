package app.aoki.quarkuscrud.resource;

import static org.junit.jupiter.api.Assertions.*;

import app.aoki.quarkuscrud.entity.User;
import app.aoki.quarkuscrud.generated.model.Event;
import app.aoki.quarkuscrud.generated.model.MetaData;
import app.aoki.quarkuscrud.service.EventService;
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
public class EventMetaApiTest {

  @Inject UserService userService;
  @Inject EventService eventService;

  @Test
  @Order(1)
  public void testGetEventMeta_Success_Attendee() {
    User initiator = userService.createAnonymousUser();
    Event event =
        RestAssured.given()
            .auth()
            .oauth2(initiator.getAnonymousJwt())
            .contentType(ContentType.JSON)
            .body("{}")
            .when()
            .post("/api/events")
            .then()
            .statusCode(201)
            .extract()
            .as(Event.class);

    RestAssured.given()
        .auth()
        .oauth2(initiator.getAnonymousJwt())
        .when()
        .get("/api/events/" + event.getId() + "/meta")
        .then()
        .statusCode(200)
        .contentType(ContentType.JSON);
  }

  @Test
  @Order(2)
  public void testUpdateEventMeta_Success_Attendee() {
    User initiator = userService.createAnonymousUser();
    Event event =
        RestAssured.given()
            .auth()
            .oauth2(initiator.getAnonymousJwt())
            .contentType(ContentType.JSON)
            .body("{}")
            .when()
            .post("/api/events")
            .then()
            .statusCode(201)
            .extract()
            .as(Event.class);

    Map<String, Object> meta = new HashMap<>();
    meta.put("theme", "birthday");
    meta.put("location", "Tokyo");

    Map<String, Object> request = new HashMap<>();
    request.put("usermeta", meta);

    RestAssured.given()
        .auth()
        .oauth2(initiator.getAnonymousJwt())
        .contentType(ContentType.JSON)
        .body(request)
        .when()
        .put("/api/events/" + event.getId() + "/meta")
        .then()
        .statusCode(200);
  }

  @Test
  @Order(3)
  public void testGetEventMeta_Forbidden_NonAttendee() {
    User initiator = userService.createAnonymousUser();
    User other = userService.createAnonymousUser();
    Event event =
        RestAssured.given()
            .auth()
            .oauth2(initiator.getAnonymousJwt())
            .contentType(ContentType.JSON)
            .body("{}")
            .when()
            .post("/api/events")
            .then()
            .statusCode(201)
            .extract()
            .as(Event.class);

    // Non-attendee tries to access event metadata
    RestAssured.given()
        .auth()
        .oauth2(other.getAnonymousJwt())
        .when()
        .get("/api/events/" + event.getId() + "/meta")
        .then()
        .statusCode(403);
  }

  @Test
  @Order(4)
  public void testUpdateEventMeta_Forbidden_NonAttendee() {
    User initiator = userService.createAnonymousUser();
    User other = userService.createAnonymousUser();
    Event event =
        RestAssured.given()
            .auth()
            .oauth2(initiator.getAnonymousJwt())
            .contentType(ContentType.JSON)
            .body("{}")
            .when()
            .post("/api/events")
            .then()
            .statusCode(201)
            .extract()
            .as(Event.class);

    Map<String, Object> request = new HashMap<>();
    request.put("usermeta", Map.of("malicious", "data"));

    // Non-attendee tries to update event metadata
    RestAssured.given()
        .auth()
        .oauth2(other.getAnonymousJwt())
        .contentType(ContentType.JSON)
        .body(request)
        .when()
        .put("/api/events/" + event.getId() + "/meta")
        .then()
        .statusCode(403);
  }

  @Test
  @Order(5)
  public void testGetEventMeta_Success_SecondaryAttendee() {
    User initiator = userService.createAnonymousUser();
    User attendee = userService.createAnonymousUser();
    Event event =
        RestAssured.given()
            .auth()
            .oauth2(initiator.getAnonymousJwt())
            .contentType(ContentType.JSON)
            .body("{}")
            .when()
            .post("/api/events")
            .then()
            .statusCode(201)
            .extract()
            .as(Event.class);

    // Join event as attendee
    var invitationCodes = eventService.getEventInvitationCodes(event.getId());
    var code = invitationCodes.get(0).getInvitationCode();
    RestAssured.given()
        .auth()
        .oauth2(attendee.getAnonymousJwt())
        .contentType(ContentType.JSON)
        .body("{\"invitationCode\":\"" + code + "\"}")
        .when()
        .post("/api/events/join-by-code")
        .then()
        .statusCode(200);

    // Attendee can read metadata
    RestAssured.given()
        .auth()
        .oauth2(attendee.getAnonymousJwt())
        .when()
        .get("/api/events/" + event.getId() + "/meta")
        .then()
        .statusCode(200);
  }

  @Test
  @Order(6)
  public void testUpdateEventMeta_Success_SecondaryAttendee() {
    User initiator = userService.createAnonymousUser();
    User attendee = userService.createAnonymousUser();
    Event event =
        RestAssured.given()
            .auth()
            .oauth2(initiator.getAnonymousJwt())
            .contentType(ContentType.JSON)
            .body("{}")
            .when()
            .post("/api/events")
            .then()
            .statusCode(201)
            .extract()
            .as(Event.class);

    // Join event as attendee
    var invitationCodes = eventService.getEventInvitationCodes(event.getId());
    var code = invitationCodes.get(0).getInvitationCode();
    RestAssured.given()
        .auth()
        .oauth2(attendee.getAnonymousJwt())
        .contentType(ContentType.JSON)
        .body("{\"invitationCode\":\"" + code + "\"}")
        .when()
        .post("/api/events/join-by-code")
        .then()
        .statusCode(200);

    Map<String, Object> request = new HashMap<>();
    request.put("usermeta", Map.of("notes", "My notes"));

    // Attendee can write metadata (based on corrected authorization rules)
    RestAssured.given()
        .auth()
        .oauth2(attendee.getAnonymousJwt())
        .contentType(ContentType.JSON)
        .body(request)
        .when()
        .put("/api/events/" + event.getId() + "/meta")
        .then()
        .statusCode(200);
  }
}
