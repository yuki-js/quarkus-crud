package app.aoki.health;

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

  @Inject @Liveness Iterable<org.eclipse.microprofile.health.HealthCheck> livenessChecks;

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response health() {
    // Check if all liveness checks pass
    boolean allHealthy = true;
    for (org.eclipse.microprofile.health.HealthCheck check : livenessChecks) {
      HealthCheckResponse response = check.call();
      if (response.getStatus() != HealthCheckResponse.Status.UP) {
        allHealthy = false;
        break;
      }
    }

    Map<String, String> health = new HashMap<>();
    health.put("status", allHealthy ? "UP" : "DOWN");
    health.put("service", "quarkus-crud");

    return Response.status(allHealthy ? Response.Status.OK : Response.Status.SERVICE_UNAVAILABLE)
        .entity(health)
        .build();
  }
}
