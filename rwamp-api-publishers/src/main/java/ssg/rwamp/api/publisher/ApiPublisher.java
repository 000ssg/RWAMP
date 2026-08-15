package ssg.rwamp.api.publisher;

import ssg.rwamp.api.provider.model.ApiDefinition;

/**
 * Strategy interface for publishing API definitions to various formats.
 * <p>
 * Publishers take a structured {@link ApiDefinition} (produced by an
 * {@link ssg.rwamp.api.provider.ApiProvider}) and expose it in a specific
 * format: OpenAPI JSON/YAML, WAMP meta-procedures, interactive HTML, etc.
 * <p>
 * Inspired by xLib's {@code API_Publisher} but with a focus on standard
 * API publishing mechanisms.
 *
 * @since 0.1.0
 */
public interface ApiPublisher {

    /**
     * Publishes the given API definition.
     * <p>
     * The return type depends on the publisher implementation:
     * <ul>
     *   <li>OpenApiPublisher → JSON/YAML string</li>
     *   <li>WampApiPublisher → registration result</li>
     *   <li>JsDocPublisher → HTML string</li>
     * </ul>
     *
     * @param definition the API to publish
     * @return publisher-specific result (may be null)
     */
    Object publish(ApiDefinition definition);

    /**
     * Publisher type identifier (e.g. "openapi", "wamp", "jsdoc").
     *
     * @return the type label
     */
    String type();
}
