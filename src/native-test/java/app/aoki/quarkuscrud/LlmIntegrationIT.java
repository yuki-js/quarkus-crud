package app.aoki.quarkuscrud;

import io.quarkus.test.junit.QuarkusIntegrationTest;
import org.junit.jupiter.api.Disabled;

/**
 * Native integration test variant of LlmIntegrationTest.
 *
 * <p>DISABLED: This test requires WireMock (via @QuarkusTestResource) which doesn't work with
 * native integration tests. QuarkusTestResource lifecycle managers are not started for
 * @QuarkusIntegrationTest because the native binary runs as a separate process.
 *
 * <p>The LLM functionality is already tested in JVM mode. Native mode testing would require
 * starting WireMock as an external service, which is beyond the scope of these tests.
 */
@QuarkusIntegrationTest
@Disabled("WireMock test resources don't work with native integration tests")
public class LlmIntegrationIT extends LlmIntegrationTest {
  // This would run the same tests as LlmIntegrationTest but in native mode
  // However, WireMock server is not available in native mode
}
