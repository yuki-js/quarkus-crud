package app.aoki.quarkuscrud.service;

import io.quarkus.test.junit.QuarkusIntegrationTest;

/**
 * Native integration test variant of EventServiceTest. This test runs against the native binary to
 * ensure event service behaves identically in native mode.
 */
@QuarkusIntegrationTest
public class EventServiceIT extends EventServiceTest {
  // This will run the same tests as EventServiceTest but in native mode
}
