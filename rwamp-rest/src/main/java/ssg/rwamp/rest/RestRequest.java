package ssg.rwamp.rest;

import java.util.List;
import java.util.Map;

/**
 * Represents an HTTP request to be bridged to WAMP.
 * <p>
 * This is a transport-agnostic representation that can be populated
 * from any HTTP framework (Servlet, Netty, Vert.x, etc.).
 *
 * @param method     HTTP method (GET, POST, PUT, DELETE)
 * @param path       request path (e.g. "/com/example/add")
 * @param pathParams positional arguments extracted from path segments
 * @param queryParams keyword arguments extracted from query string
 * @param body       request body (for POST/PUT), or null
 */
public record RestRequest(String method, String path,
                          List<String> pathParams,
                          Map<String, String> queryParams,
                          Object body) {
}
