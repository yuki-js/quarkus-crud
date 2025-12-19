package app.aoki.quarkuscrud.service;

import io.quarkus.test.junit.QuarkusIntegrationTest;

/**
 * Native integration test variant of ProfileServiceTest. This test runs against the native binary
 * to ensure profile service behaves identically in native mode.
 */
@QuarkusIntegrationTest
public class ProfileServiceIT extends ProfileServiceTest {
  // This will run the same tests as ProfileServiceTest but in native mode
}
