# Deployment & Operations

This template is built to move from dev-mode convenience to production-grade deployments without changing tooling. Gradle drives both the Quarkus build and the container image, while configuration is split between `application.properties` (prod defaults) and environment variables.

## Building artifacts

`./gradlew build` produces the runnable JAR along with the compiled OpenAPI spec packaged under `META-INF/resources`. To containerize the app, run `./gradlew jib`. Jib uses the Java 21 distroless image by default and publishes to `ghcr.io/yuki-js/quarkus-crud:${version}` unless you override `jib.to.image`. No Docker daemon is required; Jib assembles layers directly.

For native builds, add `-PnativeBuild=true` and adjust base images accordingly. The Jib configuration in `build.gradle` switches entrypoints and permissions based on that flag.

## Configuration strategy

Production settings live in `src/main/resources/application.properties`. Database credentials, JDBC URLs, and JWT keys are intentionally left blank so they must be supplied via environment variables or Kubernetes secrets (`QUARKUS_DATASOURCE_*`, `MP_JWT_VERIFY_PUBLICKEY_LOCATION`, `SMALLRYE_JWT_SIGN_KEY_LOCATION`). Observability endpoints remain stable (`/openapi`, `/swagger-ui`, `/healthz`, `/q/metrics`) making it simple to write ingress or probe definitions once.

When deploying to Kubernetes, mount secrets or config maps that populate those environment variables. Quarkus supports `.env` style files as well, but Kubernetes `EnvVar` entries keep intent clearer.

## Database lifecycle

Flyway runs automatically at startup. If you need zero-downtime rollouts, follow the usual Flyway practice: add backward-compatible migrations, deploy, then clean up old columns in a subsequent release. Because Dev Services mirrors PostgreSQL 15 (the same version expected in production), there are no surprises when migrations hit the live database.

## Observability endpoints

Expose `/healthz` as both liveness and readiness probes in Kubernetes. Metrics live at `/q/metrics`, which Prometheus Operator can scrape with a `ServiceMonitor`. Logs are structured JSON already, so a sidecar or Fluent Bit can ship them upstream without transformation.

## Promotion workflow

1. Run `./gradlew clean build test` locally or in CI.
2. Execute `./gradlew jib` (or `jibDockerBuild` if you need a local Docker image) with the registry credentials provided via `REGISTRY_USERNAME`/`REGISTRY_PASSWORD`. The task embeds OCI labels (`org.opencontainers.image.*`) for traceability.
3. Deploy the resulting image to your cluster or platform of choice. Because Quarkus HTTP is bound to `0.0.0.0:8080`, most platforms require only a simple service/ingress definition.

The combination of Jib, environment-driven configuration, and SmallRye health makes the repo production friendly without extra scaffolding.
