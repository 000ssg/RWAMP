package ssg.rwamp.api.provider.model;

/**
 * Direction of an API parameter.
 */
public enum ApiParameterKind {
    /** Input parameter (request → handler) */
    INPUT,

    /** Output parameter (handler → response) */
    OUTPUT,

    /** Bidirectional parameter */
    INPUT_OUTPUT
}
