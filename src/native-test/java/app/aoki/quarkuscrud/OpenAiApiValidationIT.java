package app.aoki.quarkuscrud;

import io.quarkus.test.junit.QuarkusIntegrationTest;
import org.junit.jupiter.api.Disabled;

/**
 * Native integration test variant of OpenAiApiValidationTest.
 *
 * <p>DISABLED: This test uses WireMock which doesn't work with native integration tests.
 * QuarkusTestResource lifecycle managers are not started for @QuarkusIntegrationTest because the
 * native binary runs as a separate process.
 *
 * <p>The OpenAI API validation is already tested in JVM mode. Native mode testing would require an
 * external mock server, which is beyond the scope of these tests.
 */
@QuarkusIntegrationTest
@Disabled("WireMock test resources don't work with native integration tests")
public class OpenAiApiValidationIT extends OpenAiApiValidationTest {
  // This would run the same tests as OpenAiApiValidationTest but in native mode
  // However, WireMock server is not available in native mode
}
