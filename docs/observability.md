# Observability & Runtime Signals

The template assumes every environment—dev included—needs health checks, metrics, structured logs, and secure defaults. This document describes what is already wired so you can hook the service into dashboards or tweak behavior without spelunking through configuration files.

## Logging

Quarkus JSON logging is enabled by default (`quarkus.log.console.json=true`). Fields such as `service.name` and `service.version` are injected into every record so log aggregators can filter by deployment. The log level is `INFO`, while the application package (`app.aoki.quarkuscrud`) can be dialed up or down via `quarkus.log.category` if you need more detail when debugging.

In dev mode you get a formatted console pattern for readability, but production profiles stick to JSON so tools like Loki, Cloud Logging, or Elastic can parse events without guesswork.

## Metrics

Micrometer with the Prometheus registry ships enabled (`quarkus.micrometer.enabled=true`). Default binders collect HTTP server/client, Vert.x, and JVM metrics. Scrape them at `/q/metrics` (dev) or the same path when deployed; the endpoint integrates cleanly with Kubernetes `ServiceMonitor` objects or vanilla Prometheus scrapes.

## Health and readiness

SmallRye Health exposes aggregated checks. Dev profile uses `/q/health` for the Quarkus default UI integration, while prod profile remaps to `/healthz` to mimic the path Kubernetes probes often expect. You can add custom health checks under `app.aoki.quarkuscrud.support` (or another package) and they will be picked up automatically thanks to the `quarkus-smallrye-health` extension.

## JWT and auth signals

SmallRye JWT handles both verification (`mp.jwt.verify.*`) and token issuance (`smallrye.jwt.new-token.*`). Development mode reads keys from `src/main/resources/dev-keys`, but production should override `MP_JWT_VERIFY_PUBLICKEY_LOCATION` and `SMALLRYE_JWT_SIGN_KEY_LOCATION` with secrets. Because the defaults are explicit, missing environment variables fail fast during startup instead of producing silent 401s.

## CORS

The template enables CORS with regex origins (`/.*/`) so that browser-based clients can authenticate using cookies during development while still supporting credentials. Change `quarkus.http.cors.*` in `application.properties` to tighten this up when you know the set of frontend domains.

## Dev UI extras

The `quarkus-info` extension surfaces build metadata, git commit information, and Dev Services status directly in Dev UI. This is handy when multiple developers share the same template because you can prove which commit and migration level are running without SSHing into anything.

Taken together, these defaults make it trivial to plug the service into Kubernetes readiness probes, Prometheus scrapes, or log aggregation. Adjust the values, not the wiring.
