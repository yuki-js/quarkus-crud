package app.aoki.resource;

import app.aoki.exception.DatabaseHealthCheck;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.HashMap;
import java.util.Map;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;

/**
 * Legacy /healthz endpoint for backward compatibility. Returns a simplified health status. For
 * detailed health checks, use /q/health endpoints provided by SmallRye Health.
 */
@Path("/healthz")
public class HealthzResource {

  @Inject @Liveness DatabaseHealthCheck databaseHealthCheck;

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response health() {
    // Check database health
    HealthCheckResponse dbHealth = databaseHealthCheck.call();
    boolean allHealthy = dbHealth.getStatus() == HealthCheckResponse.Status.UP;

    Map<String, String> health = new HashMap<>();
    health.put("status", allHealthy ? "UP" : "DOWN");
    health.put("service", "quarkus-crud");

    return Response.status(allHealthy ? Response.Status.OK : Response.Status.SERVICE_UNAVAILABLE)
        .entity(health)
        .build();
  }
}
