package app.aoki.quarkuscrud.support;

/**
 * Standard error response format for the API.
 *
 * @param error the error message
 */
public record ErrorResponse(String error) {}
