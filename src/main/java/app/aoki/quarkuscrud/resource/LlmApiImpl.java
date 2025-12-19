package app.aoki.quarkuscrud.resource;

import app.aoki.quarkuscrud.entity.User;
import app.aoki.quarkuscrud.generated.api.LlmApi;
import app.aoki.quarkuscrud.generated.model.FakeNamesRequest;
import app.aoki.quarkuscrud.generated.model.FakeNamesResponse;
import app.aoki.quarkuscrud.service.LlmService;
import app.aoki.quarkuscrud.service.RateLimiterService;
import app.aoki.quarkuscrud.support.Authenticated;
import app.aoki.quarkuscrud.support.AuthenticatedUser;
import app.aoki.quarkuscrud.support.ErrorResponse;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.Response;
import java.util.List;
import org.jboss.logging.Logger;

@ApplicationScoped
@Path("/api/llm/fake-names")
public class LlmApiImpl implements LlmApi {

  private static final Logger LOG = Logger.getLogger(LlmApiImpl.class);

  @Inject LlmService llmService;

  @Inject RateLimiterService rateLimiterService;

  @Inject AuthenticatedUser authenticatedUser;

  @Inject MeterRegistry meterRegistry;

  @Override
  @Authenticated
  public Response generateFakeNames(FakeNamesRequest request) {
    User user = authenticatedUser.get();
    LOG.infof(
        "Request received: generate fake names for user %d (input: %s, variance: %.2f)",
        user.getId(), request.getInputName(), request.getVariance());

    Timer.Sample sample = Timer.start(meterRegistry);

    try {
      // Check rate limit
      if (!rateLimiterService.allowRequest(user.getId())) {
        LOG.warnf("Rate limit exceeded for user %d", user.getId());
        meterRegistry.counter("api.llm.rate_limit_exceeded").increment();
        return Response.status(Response.Status.TOO_MANY_REQUESTS)
            .entity(
                new ErrorResponse(
                    "Rate limit exceeded. Please try again later. (Limit: 100 requests per"
                        + " minute per user, 300 requests per minute globally)"))
            .build();
      }

      // Validate request
      if (request.getInputName() == null || request.getInputName().isBlank()) {
        return Response.status(Response.Status.BAD_REQUEST)
            .entity(new ErrorResponse("Input name is required"))
            .build();
      }

      if (request.getVariance() == null
          || request.getVariance() < 0.0
          || request.getVariance() > 1.0) {
        return Response.status(Response.Status.BAD_REQUEST)
            .entity(new ErrorResponse("Variance must be between 0.0 and 1.0"))
            .build();
      }

      // Generate fake names
      List<String> fakeNames =
          llmService.generateFakeNames(request.getInputName(), request.getVariance());

      // Build response
      FakeNamesResponse response = new FakeNamesResponse();
      response.setOutput(new java.util.LinkedHashSet<>(fakeNames));

      meterRegistry.counter("api.llm.fake_names.success").increment();
      LOG.infof("Successfully generated %d fake names for user %d", fakeNames.size(), user.getId());

      return Response.ok(response).build();

    } catch (Exception e) {
      LOG.errorf(e, "Failed to generate fake names for user %d", user.getId());
      meterRegistry.counter("api.llm.fake_names.error").increment();
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
          .entity(new ErrorResponse("Failed to generate fake names: " + e.getMessage()))
          .build();
    } finally {
      sample.stop(meterRegistry.timer("api.llm.fake_names.duration"));
    }
  }
}
