package app.aoki;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

/**
 * Schema validation test that ensures the application starts successfully and database migrations
 * (Flyway) execute without errors. This is a smoke test to catch database schema issues early.
 */
@QuarkusTest
public class SchemaValidationTest {

  @Test
  public void testApplicationStartsSuccessfully() {
    // This test validates that:
    // 1. Quarkus application starts successfully
    // 2. Database connection is established
    // 3. Flyway migrations execute without errors
    // 4. All database tables and constraints are created correctly
    // If any of these fail, the test will fail during Quarkus startup
  }
}
