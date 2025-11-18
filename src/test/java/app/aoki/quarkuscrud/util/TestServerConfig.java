package app.aoki.quarkuscrud.util;

import io.restassured.RestAssured;
import io.restassured.config.RedirectConfig;
import io.restassured.config.SSLConfig;

/**
 * Configuration utility for setting up tests to run against either internal (Quarkus test server)
 * or external (production) servers.
 *
 * <p>This utility allows the same test suite to be executed against different environments by
 * configuring RestAssured appropriately.
 *
 * <p>Usage:
 *
 * <pre>
 * // For internal tests (default with @QuarkusTest)
 * TestServerConfig.configureForInternalServer();
 *
 * // For external tests
 * TestServerConfig.configureForExternalServer("https://quarkus-crud.ouchiserver.aokiapp.com");
 * </pre>
 */
public class TestServerConfig {

  public static final String EXTERNAL_URL_PROPERTY = "test.external.url";
  public static final String DEFAULT_EXTERNAL_URL =
      "https://quarkus-crud.ouchiserver.aokiapp.com";

  /**
   * Configures RestAssured for testing against an internal Quarkus test server. This is the
   * default configuration used with @QuarkusTest.
   */
  public static void configureForInternalServer() {
    // When using @QuarkusTest, RestAssured is automatically configured
    // This method exists for explicit configuration if needed
    RestAssured.reset();
  }

  /**
   * Configures RestAssured for testing against an external server.
   *
   * @param serverUrl The URL of the external server (e.g.,
   *     "https://quarkus-crud.ouchiserver.aokiapp.com")
   */
  public static void configureForExternalServer(String serverUrl) {
    if (serverUrl == null || serverUrl.trim().isEmpty()) {
      throw new IllegalArgumentException("Server URL cannot be null or empty");
    }

    try {
      java.net.URL url = new java.net.URL(serverUrl);

      // Set base URI (protocol + host)
      RestAssured.baseURI = url.getProtocol() + "://" + url.getHost();

      // Set port
      int port = url.getPort();
      if (port == -1) {
        // Use default ports based on protocol
        port = url.getProtocol().equals("https") ? 443 : 80;
      }
      RestAssured.port = port;

      // Set base path if present
      String path = url.getPath();
      if (path != null && !path.isEmpty() && !path.equals("/")) {
        RestAssured.basePath = path;
      }

      // Configure SSL for HTTPS
      if (url.getProtocol().equals("https")) {
        RestAssured.useRelaxedHTTPSValidation();
        RestAssured.config =
            RestAssured.config()
                .sslConfig(SSLConfig.sslConfig().relaxedHTTPSValidation())
                .redirect(RedirectConfig.redirectConfig().followRedirects(true));
      }

    } catch (java.net.MalformedURLException e) {
      throw new IllegalArgumentException("Invalid server URL: " + serverUrl, e);
    }
  }

  /**
   * Configures RestAssured for testing against an external server using the URL from system
   * property or default.
   */
  public static void configureForExternalServer() {
    String externalUrl = System.getProperty(EXTERNAL_URL_PROPERTY, DEFAULT_EXTERNAL_URL);
    configureForExternalServer(externalUrl);
  }

  /**
   * Checks if tests are configured to run against an external server.
   *
   * @return true if external server mode is enabled, false otherwise
   */
  public static boolean isExternalServerMode() {
    return System.getProperty(EXTERNAL_URL_PROPERTY) != null
        || Boolean.getBoolean("test.external.enabled");
  }

  /**
   * Gets the configured external server URL.
   *
   * @return the external server URL, or null if not configured
   */
  public static String getExternalServerUrl() {
    return System.getProperty(EXTERNAL_URL_PROPERTY, DEFAULT_EXTERNAL_URL);
  }

  /**
   * Resets RestAssured to its default configuration.
   */
  public static void reset() {
    RestAssured.reset();
  }
}
