package app.aoki.quarkuscrud.service;

import io.quarkus.test.junit.QuarkusIntegrationTest;

/**
 * Native integration test variant of UserServiceTest. This test runs against the native binary to
 * ensure user service behaves identically in native mode.
 */
@QuarkusIntegrationTest
public class UserServiceIT extends UserServiceTest {
  // This will run the same tests as UserServiceTest but in native mode
}
