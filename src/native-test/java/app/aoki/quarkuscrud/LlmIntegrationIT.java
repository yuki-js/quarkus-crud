package app.aoki.quarkuscrud;

import io.quarkus.test.junit.QuarkusIntegrationTest;

/**
 * Native integration test variant of LlmIntegrationTest. This test runs against the native binary
 * to ensure LLM integration behaves identically in native mode.
 */
@QuarkusIntegrationTest
public class LlmIntegrationIT extends LlmIntegrationTest {
  // This will run the same tests as LlmIntegrationTest but in native mode
}
