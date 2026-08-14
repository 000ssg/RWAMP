package ssg.rwamp.rest;

import java.util.Map;

/**
 * Represents an HTTP response from a bridged WAMP call.
 *
 * @param statusCode HTTP status code (200, 404, 500, etc.)
 * @param body       response body (result data or error details)
 */
public record RestResponse(int statusCode, Object body) {

    public static RestResponse ok(Object result) {
        return new RestResponse(200, result);
    }

    public static RestResponse notFound(String message) {
        return new RestResponse(404, Map.of("error", message));
    }

    public static RestResponse serverError(String message) {
        return new RestResponse(500, Map.of("error", message));
    }
}
