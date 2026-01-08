package app.aoki.quarkuscrud;

import io.quarkus.test.junit.QuarkusIntegrationTest;

/**
 * Native integration test variant of OpenAiApiValidationTest. This test runs against the native
 * binary to ensure OpenAI API validation behaves identically in native mode.
 */
@QuarkusIntegrationTest
public class OpenAiApiValidationIT extends OpenAiApiValidationTest {
  // This will run the same tests as OpenAiApiValidationTest but in native mode
}
