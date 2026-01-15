package app.aoki.quarkuscrud;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * Integration test to verify that event organizers (initiators) can save quiz data without
 * manually joining their own event.
 *
 * <p>This test addresses the bug where organizers could not save quizzes because they were not
 * automatically added as attendees when creating an event.
 *
 * <p>Expected behavior: When a user creates an event, they should be automatically added as an
 * attendee and should be able to save quiz data immediately.
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class OrganizerQuizSaveTest {

  private static String organizerToken;
  private static Long organizerId;
  private static Long eventId;
  private static String invitationCode;

  @Test
  @Order(0)
  public void setup() {
    // Create organizer user
    Response organizerResponse = given().contentType(ContentType.JSON).post("/api/auth/guest");
    organizerToken = organizerResponse.getHeader("Authorization").substring(7);
    Response organizerUserResponse =
        given().header("Authorization", "Bearer " + organizerToken).when().get("/api/me");
    organizerId = organizerUserResponse.jsonPath().getLong("id");
    assertNotNull(organizerId, "Organizer ID should not be null");
  }

  @Test
  @Order(1)
  public void testCreateEvent() {
    // Organizer creates an event
    Response eventResponse =
        given()
            .header("Authorization", "Bearer " + organizerToken)
            .contentType(ContentType.JSON)
            .body("{\"meta\":{\"name\":\"Quiz Event\",\"description\":\"Test Quiz Event\"}}")
            .when()
            .post("/api/events")
            .then()
            .statusCode(201)
            .body("id", notNullValue())
            .body("invitationCode", notNullValue())
            .body("initiatorId", equalTo(organizerId.intValue()))
            .extract()
            .response();

    eventId = eventResponse.jsonPath().getLong("id");
    invitationCode = eventResponse.jsonPath().getString("invitationCode");
    assertNotNull(eventId, "Event ID should not be null");
    assertNotNull(invitationCode, "Invitation code should not be null");
  }

  @Test
  @Order(2)
  public void testOrganizerIsAutomaticallyAttendee() {
    // Verify that the organizer appears in the attendees list
    given()
        .header("Authorization", "Bearer " + organizerToken)
        .when()
        .get("/api/events/" + eventId + "/attendees")
        .then()
        .statusCode(200)
        .body("size()", equalTo(1))
        .body("[0].attendeeUserId", equalTo(organizerId.intValue()))
        .body("[0].eventId", equalTo(eventId.intValue()));
  }

  @Test
  @Order(3)
  public void testOrganizerCanSaveQuizWithoutManuallyJoining() {
    // Organizer saves quiz data WITHOUT manually joining via /api/events/join-by-code
    // This is the core test case for the bug fix
    Response updateResponse =
        given()
            .header("Authorization", "Bearer " + organizerToken)
            .contentType(ContentType.JSON)
            .body(
                """
                {
                  "userData": {
                    "myQuiz": {
                      "question": "What is the capital of Japan?",
                      "choices": ["Tokyo", "Osaka", "Kyoto", "Nagoya"],
                      "correctAnswer": 0
                    },
                    "fakeAnswers": {
                      "1": false,
                      "2": false,
                      "3": false
                    },
                    "updatedAt": "2026-01-15T07:00:00Z"
                  }
                }
                """)
            .when()
            .put("/api/events/" + eventId + "/users/" + organizerId)
            .then()
            .statusCode(200)
            .body("id", notNullValue())
            .body("eventId", equalTo(eventId.intValue()))
            .body("userId", equalTo(organizerId.intValue()))
            .body("userData.myQuiz.question", equalTo("What is the capital of Japan?"))
            .body("userData.myQuiz.choices.size()", equalTo(4))
            .body("userData.myQuiz.correctAnswer", equalTo(0))
            .extract()
            .response();

    assertNotNull(updateResponse.jsonPath().getLong("id"), "User data ID should not be null");
  }

  @Test
  @Order(4)
  public void testOrganizerCanRetrieveQuizData() {
    // Verify that the organizer can retrieve their quiz data
    given()
        .header("Authorization", "Bearer " + organizerToken)
        .when()
        .get("/api/events/" + eventId + "/users/" + organizerId)
        .then()
        .statusCode(200)
        .body("eventId", equalTo(eventId.intValue()))
        .body("userId", equalTo(organizerId.intValue()))
        .body("userData.myQuiz.question", equalTo("What is the capital of Japan?"))
        .body("userData.myQuiz.choices.size()", equalTo(4))
        .body("userData.myQuiz.correctAnswer", equalTo(0))
        .body("userData.fakeAnswers", notNullValue());
  }

  @Test
  @Order(5)
  public void testOrganizerCanUpdateQuizData() {
    // Verify that the organizer can update their quiz data multiple times
    given()
        .header("Authorization", "Bearer " + organizerToken)
        .contentType(ContentType.JSON)
        .body(
            """
            {
              "userData": {
                "myQuiz": {
                  "question": "What is the capital of France?",
                  "choices": ["Paris", "Lyon", "Marseille", "Nice"],
                  "correctAnswer": 0
                },
                "fakeAnswers": {
                  "1": true,
                  "2": false,
                  "3": false
                },
                "updatedAt": "2026-01-15T08:00:00Z"
              }
            }
            """)
        .when()
        .put("/api/events/" + eventId + "/users/" + organizerId)
        .then()
        .statusCode(200)
        .body("userData.myQuiz.question", equalTo("What is the capital of France?"))
        .body("userData.myQuiz.choices[0]", equalTo("Paris"))
        .body("userData.fakeAnswers.1", equalTo(true));
  }

  @Test
  @Order(6)
  public void testRegularParticipantCanAlsoSaveQuiz() {
    // Create a regular participant to verify they can also save quizzes
    Response participantResponse = given().contentType(ContentType.JSON).post("/api/auth/guest");
    String participantToken = participantResponse.getHeader("Authorization").substring(7);
    Response participantUserResponse =
        given().header("Authorization", "Bearer " + participantToken).when().get("/api/me");
    Long participantId = participantUserResponse.jsonPath().getLong("id");

    // Participant joins the event
    given()
        .header("Authorization", "Bearer " + participantToken)
        .contentType(ContentType.JSON)
        .body("{\"invitationCode\":\"" + invitationCode + "\"}")
        .when()
        .post("/api/events/join-by-code")
        .then()
        .statusCode(201);

    // Participant saves quiz data
    given()
        .header("Authorization", "Bearer " + participantToken)
        .contentType(ContentType.JSON)
        .body(
            """
            {
              "userData": {
                "myQuiz": {
                  "question": "What is 2 + 2?",
                  "choices": ["3", "4", "5", "6"],
                  "correctAnswer": 1
                },
                "fakeAnswers": {}
              }
            }
            """)
        .when()
        .put("/api/events/" + eventId + "/users/" + participantId)
        .then()
        .statusCode(200)
        .body("userData.myQuiz.question", equalTo("What is 2 + 2?"));
  }

  @Test
  @Order(7)
  public void testAttendeesListContainsBothOrganizerAndParticipant() {
    // Verify both organizer and participant are in the attendees list
    given()
        .header("Authorization", "Bearer " + organizerToken)
        .when()
        .get("/api/events/" + eventId + "/attendees")
        .then()
        .statusCode(200)
        .body("size()", equalTo(2)); // Organizer + 1 participant
  }
}
