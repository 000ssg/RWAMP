package ssg.rwamp.api.provider.model;

import java.util.Map;

/**
 * Describes an error that an API operation may produce.

 * @param code        error identifier
 * @param description human-readable explanation
 * @param extensions  arbitrary metadata (never null, may be empty)
 */
public record ApiError(
        String code,
        String description,
        Map<String, Object> extensions
) {
    public ApiError {
        if (extensions == null) extensions = Map.of();
    }
    
    public static ApiError of(String code, String description) {
        return new ApiError(code, description, null);
    }
}
