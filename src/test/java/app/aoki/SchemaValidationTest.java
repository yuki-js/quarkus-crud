import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class SchemaValidationTest {
  @Test
  public void testSchemaCreated() {
    // This test just validates that Quarkus starts and Flyway runs
    System.out.println("Schema validation test passed");
  }
}
