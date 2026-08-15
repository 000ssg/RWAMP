package ssg.rwamp.api.provider.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Describes a callable API operation (procedure or function).
 * <p>
 * Inspired by xLib's {@code APIProcedure} / {@code APIFunction}.

 * @param name         unique identifier (URI-style or simple name)
 * @param description  human-readable description
 * @param summary      short one-line summary
 * @param operationId  stable ID for code generation
 * @param parameters   ordered input parameters (never null, may be empty)
 * @param response     return type (null for void procedures)
 * @param errors       possible errors (never null, may be empty)
 * @param tags         grouping labels (never null, may be empty)
 * @param deprecated   whether the operation is deprecated
 * @param hidden       whether to hide from auto-generated docs
 * @param extensions   arbitrary metadata (never null, may be empty)
 */
public record ApiOperation(
        String name,
        String description,
        String summary,
        String operationId,
        List<ApiParameter> parameters,
        ApiDataType response,
        List<ApiError> errors,
        List<String> tags,
        boolean deprecated,
        boolean hidden,
        Map<String, Object> extensions
) {
    public ApiOperation {
        if (parameters == null) parameters = Collections.emptyList();
        if (errors == null) errors = Collections.emptyList();
        if (tags == null) tags = Collections.emptyList();
        if (extensions == null) extensions = Map.of();
    }

    /** Minimal constructor for simple procedures */
    public static ApiOperation procedure(String name, List<ApiParameter> params) {
        return new ApiOperation(name, null, null, name, params, null, null, null, false, false, null);
    }

    /** Minimal constructor for functions with a return type */
    public static ApiOperation function(String name, List<ApiParameter> params, ApiDataType response) {
        return new ApiOperation(name, null, null, name, params, response, null, null, false, false, null);
    }
}
