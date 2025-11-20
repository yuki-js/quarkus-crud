# GitHub Actions Performance Optimization

## Overview

This document describes the optimization work performed to reduce GitHub Actions execution time for the quarkus-crud project.

## Problem Statement

GitHub Actions workflows were taking excessively long to execute:
- Native builds: ~5.5 minutes (330 seconds)
- JVM builds: ~3 minutes  
- CI workflow: ~3 minutes
- Total workflow time: Up to 9 minutes

## Solutions Implemented

### 1. Gradle Build Cache

**Changes:**
- Added `org.gradle.caching=true` in `gradle.properties`
- Configured build cache directory in `settings.gradle`
- Added `build-cache/` to `.gitignore`

**Impact:** Enables reuse of task outputs across builds, reducing redundant compilation.

### 2. Gradle Configuration Cache

**Changes:**
- Added `org.gradle.configuration-cache=true` in `gradle.properties`

**Impact:** Saves build configuration state to avoid re-evaluating build scripts on every run.

### 3. Gradle Parallel Execution

**Changes:**
- Added `org.gradle.parallel=true` in `gradle.properties`

**Impact:** Enables parallel execution of independent tasks.

### 4. Gradle JVM Optimization

**Changes:**
- Added `org.gradle.jvmargs=-Xmx2048m -XX:MaxMetaspaceSize=512m` in `gradle.properties`

**Impact:** Optimizes memory usage for faster builds.

### 5. GitHub Actions Caching

**CI Workflow (`ci.yml`):**
- Cache Node.js dependencies for Spectral
- Cache OpenAPI Generator CLI JAR
- Cache Gradle dependencies and build cache
- Use `actions/cache@v4` for all caching

**Publish Workflow (`publish-jib.yml`):**
- Cache Gradle build cache for both JVM and native builds
- Cache GraalVM setup
- Cache Quarkus native build artifacts
- Separate cache keys for native vs JVM builds

**Dev UI Test Workflow (`dev-ui-test.yml`):**
- Cache Gradle build cache
- Reuse pre-built artifacts

### 6. Workflow Optimizations

**CI Workflow:**
- Removed redundant `generateOpenApiModels` step (now part of `compileJava`)
- Combined build and test into single step
- Added `--build-cache` and `--parallel` flags to all Gradle commands

**Publish Workflow:**
- Added `--build-cache` to all Gradle and Jib commands
- Cached GraalVM setup to avoid repeated downloads

## Expected Performance Improvements

- **Native builds:** 30-40% reduction (from ~5.5min to ~3-4min)
- **JVM builds:** 40-50% reduction (from ~3min to ~1.5-2min)
- **CI workflow:** 40-50% reduction (from ~3min to ~1.5-2min)
- **Overall:** Approximately 40-50% reduction in total CI/CD time

## Verification Steps

All optimizations have been tested locally:
1. ✅ Gradle build cache works (`./gradlew clean --build-cache`)
2. ✅ OpenAPI model generation with caching
3. ✅ Full build with tests passes (58 tests)
4. ✅ Spotless check passes
5. ✅ Checkstyle passes

## Cache Strategy

### Cache Keys

- **Gradle dependencies:** `${{ runner.os }}-gradle-${{ hashFiles('**/*.gradle*', '**/gradle-wrapper.properties') }}`
- **Native builds:** `${{ runner.os }}-gradle-native-${{ hashFiles(...) }}`
- **GraalVM cache:** `${{ runner.os }}-graalvm-${{ hashFiles('**/build.gradle', '**/gradle.properties') }}`
- **Spectral:** `${{ runner.os }}-spectral-${{ hashFiles('**/package-lock.json') }}`
- **OpenAPI Generator:** `openapi-generator-cli-7.10.0`

### Cache Paths

- `~/.gradle/caches` - Gradle dependencies
- `~/.gradle/wrapper` - Gradle wrapper
- `.gradle/` - Project-specific Gradle files
- `build-cache/` - Local build cache
- `~/.cache/quarkus` - Quarkus native build cache
- `~/.npm` - npm global packages
- `~/.openapi-generator` - OpenAPI Generator CLI

## Monitoring

After these changes are merged, monitor the following in GitHub Actions:
1. Total workflow execution time
2. Individual job execution times
3. Cache hit rates in workflow logs
4. Build step durations

## Future Optimization Opportunities

1. **Larger runners:** Consider using larger GitHub-hosted runners if budget allows
2. **Workflow concurrency:** Add concurrency controls to cancel outdated runs
3. **Conditional execution:** Skip expensive jobs for documentation-only changes
4. **Remote build cache:** Consider using a remote build cache service (e.g., Gradle Enterprise)

## Technical Notes

- Gradle daemon is disabled in CI (`org.gradle.daemon=false`) as it's not beneficial for single-use CI runs
- The `--no-daemon` flag is explicitly used in workflow steps for clarity
- Configuration cache is experimental but stable for this project
- Build cache is stored locally in the `build-cache/` directory

## References

- [Gradle Build Cache](https://docs.gradle.org/current/userguide/build_cache.html)
- [Gradle Configuration Cache](https://docs.gradle.org/current/userguide/configuration_cache.html)
- [GitHub Actions Cache](https://docs.github.com/en/actions/using-workflows/caching-dependencies-to-speed-up-workflows)
- [Quarkus Build Performance](https://quarkus.io/guides/performance-measure)
