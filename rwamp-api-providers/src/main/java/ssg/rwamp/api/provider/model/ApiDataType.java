package ssg.rwamp.api.provider.model;

import java.util.Collections;
import java.util.Map;

/**
 * Describes a data type used in API parameter or return type definitions.
 * <p>
 * Inspired by xLib's {@code APIDataType} but simplified for RWAMP's use
 * as a lightweight metadata carrier (no DB integration).
 *
 * @param name       display name (e.g. "string", "User", "List&lt;String&gt;")
 * @param format     type format hint ("array", "object", or null for scalar)
 * @param javaType   fully-qualified Java class name
 * @param nullable   whether null values are allowed
 * @param properties sub-properties for object types (never null, may be empty)
 */
public record ApiDataType(
        String name,
        String format,
        String javaType,
        boolean nullable,
        Map<String, ApiDataType> properties
) {
    public ApiDataType {
        if (properties == null) properties = Map.of();
    }

    /** Predefined scalar types */
    public static final ApiDataType STRING   = new ApiDataType("string", null, "java.lang.String", false, null);
    public static final ApiDataType INTEGER  = new ApiDataType("integer", null, "java.lang.Integer", false, null);
    public static final ApiDataType NUMBER   = new ApiDataType("number", null, "java.lang.Double", false, null);
    public static final ApiDataType BOOLEAN  = new ApiDataType("boolean", null, "java.lang.Boolean", false, null);
    public static final ApiDataType OBJECT   = new ApiDataType("object", "object", "java.lang.Object", false, null);
    public static final ApiDataType NULLABLE_STRING = new ApiDataType("string", null, "java.lang.String", true, null);

    /**
     * Infer an {@code ApiDataType} from a Java {@code Class}.
     */
    public static ApiDataType fromClass(Class<?> clazz) {
        if (clazz == null || clazz == Object.class) return OBJECT;
        if (clazz == String.class) return STRING;
        if (clazz == int.class || clazz == Integer.class) return INTEGER;
        if (clazz == long.class || clazz == Long.class) return INTEGER;
        if (clazz == double.class || clazz == Double.class) return NUMBER;
        if (clazz == float.class || clazz == Float.class) return NUMBER;
        if (clazz == boolean.class || clazz == Boolean.class) return BOOLEAN;
        return new ApiDataType(clazz.getSimpleName(), null, clazz.getName(), true, null);
    }

    public boolean isScalar() {
        return format == null && properties().isEmpty();
    }

    public boolean isObject() {
        return "object".equals(format) || !properties().isEmpty();
    }
}
