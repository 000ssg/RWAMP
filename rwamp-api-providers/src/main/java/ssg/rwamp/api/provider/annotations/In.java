package ssg.rwamp.api.provider.annotations;

/**
 * Source location for an API parameter value.
 * <p>
 * Maps to OpenAPI {@code in} field for REST; ignored for WAMP-only APIs.
 */
public enum In {
    /** Auto-detect based on method signature and context */
    AUTO,

    /** Extracted from the path (e.g. /users/{id}) */
    PATH,

    /** Extracted from query string (e.g. ?page=1) */
    QUERY,

    /** Extracted from request headers */
    HEADER,

    /** Extracted from request body */
    BODY,

    /** Extracted from form data */
    FORM,

    /** Injected by the framework (auth context, session, etc.) */
    COOKIE
}
