package app.aoki.quarkuscrud.service;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import java.sql.Connection;
import java.sql.ResultSet;

@QuarkusTest
public class CheckMigrationTest {
  @Inject DataSource dataSource;

  @Test
  public void checkColumns() throws Exception {
    try (Connection conn = dataSource.getConnection()) {
      ResultSet rs = conn.getMetaData().getColumns(null, null, "event_invitation_codes", null);
      System.out.println("=== event_invitation_codes columns ===");
      while (rs.next()) {
        System.out.println(rs.getString("COLUMN_NAME") + " - " + rs.getString("TYPE_NAME"));
      }
    }
  }
}
