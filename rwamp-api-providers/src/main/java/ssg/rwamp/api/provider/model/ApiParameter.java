package ssg.rwamp.api.provider.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Describes a single parameter of an API operation.
 * <p>
 * Inspired by xLib's {@code APIParameter}.

 * @param name        parameter name
 * @param type        data type
 * @param kind        input, output, or bidirectional
 * @param required    whether the parameter is mandatory
 * @param description human-readable description
 * @param defaultValue default value (or null)
 * @param examples    sample values (never null, may be empty)
 * @param extensions  arbitrary metadata (never null, may be empty)
 */
public record ApiParameter(
        String name,
        ApiDataType type,
        ApiParameterKind kind,
        boolean required,
        String description,
        Object defaultValue,
        List<Object> examples,
        Map<String, Object> extensions
) {
    public ApiParameter {
        if (examples == null) examples = Collections.emptyList();
        if (extensions == null) extensions = Map.of();
    }

    public static ApiParameter input(String name, ApiDataType type) {
        return new ApiParameter(name, type, ApiParameterKind.INPUT, true, null, null, null, null);
    }

    public static ApiParameter output(String name, ApiDataType type) {
        return new ApiParameter(name, type, ApiParameterKind.OUTPUT, false, null, null, null, null);
    }
}
