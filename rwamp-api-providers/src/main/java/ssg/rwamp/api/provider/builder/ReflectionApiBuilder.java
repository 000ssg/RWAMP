package ssg.rwamp.api.provider.builder;

import java.lang.annotation.Annotation;

import ssg.rwamp.api.provider.annotations.*;
import ssg.rwamp.api.provider.model.*;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Builds {@link ApiDefinition} from Java classes using reflection and annotations.
 * <p>
 * This is the RWAMP equivalent of xLib's {@code Reflective_API_Builder}.
 * It scans classes annotated with {@link ApiService}, extracts operations
 * from methods (optionally annotated with {@link ApiOperation}), and builds
 * a complete API model including parameters, types, and grouping.
 * <p>
 * Supports:
 * <ul>
 *   <li>Annotation-based discovery ({@code @ApiService}, {@code @ApiOperation})</li>
 *   <li>Convention-based discovery (public non-static methods, getters/setters)</li>
 *   <li>Type introspection for parameters and return types</li>
 *   <li>Nested type schema generation for complex return types</li>
 * </ul>
 *
 * @since 0.1.0
 */
public class ReflectionApiBuilder {

    private static final Set<String> SKIP_METHODS = Set.of(
            "equals", "hashCode", "toString", "getClass", "notify", "notifyAll", "wait"
    );

    private final Map<Class<?>, ApiDataType> typeCache = new IdentityHashMap<>();
    private boolean includeGetters;
    private boolean includeSetters;
    private boolean includeAllPublic;

    public ReflectionApiBuilder() {
        this.includeGetters = false;
        this.includeSetters = false;
        this.includeAllPublic = false;
    }

    /** Include JavaBean getters as read operations */
    public ReflectionApiBuilder includeGetters(boolean v) { this.includeGetters = v; return this; }

    /** Include JavaBean setters as write operations */
    public ReflectionApiBuilder includeSetters(boolean v) { this.includeSetters = v; return this; }

    /** Include all public methods even without annotations */
    public ReflectionApiBuilder includeAllPublic(boolean v) { this.includeAllPublic = v; return this; }

    /**
     * Build an API definition from the given classes.
     */
    public ApiDefinition build(String apiName, Class<?>... types) {
        var groups = new LinkedHashMap<String, ApiGroup>();
        var globalTypes = new LinkedHashMap<String, ApiDataType>();

        for (Class<?> type : types) {
            var apiService = type.getAnnotation(ApiService.class);
            String groupName = apiService != null ? (apiService.name().isEmpty() ? type.getSimpleName() : apiService.name())
                    : type.getSimpleName();
            String groupDesc = apiService != null ? apiService.description() : null;
            List<String> groupTags = apiService != null ? List.of(apiService.tags()) : List.of();

            var ops = new LinkedHashMap<String, ApiOperation>();
            for (Method m : type.getDeclaredMethods()) {
                if (SKIP_METHODS.contains(m.getName())) continue;
                if (Modifier.isStatic(m.getModifiers())) continue;
                if (!Modifier.isPublic(m.getModifiers())) continue;

                ApiOperation op = extractOperation(type, m, apiService);
                if (op != null) {
                    ops.put(op.name(), op);
                }
            }

            // Extract type definitions from parameter and return types
            for (Method m : type.getDeclaredMethods()) {
                if (!Modifier.isPublic(m.getModifiers())) continue;
                for (Parameter p : m.getParameters()) {
                    registerType(globalTypes, p.getType());
                }
                registerType(globalTypes, m.getReturnType());
            }

            groups.put(groupName, new ApiGroup(groupName, groupDesc, ops, null, null, groupTags));
        }

        return new ApiDefinition(apiName, "1.0.0", null, groups, globalTypes, Map.of());
    }

    private ApiOperation extractOperation(Class<?> declaringClass, Method method, ApiService apiService) {
        var annotation = method.getAnnotation(Operation.class);

        // Check exclusion
        if (method.getAnnotation(ApiIgnore.class) != null) return null;
        if (annotation != null && annotation.exclude()) return null;

        // Convention-based: include getter/setter or all public
        boolean byConvention = false;
        String methodName = method.getName();
        if (includeGetters && methodName.startsWith("get") && methodName.length() > 3 && method.getParameterCount() == 0) {
            byConvention = true;
        } else if (includeSetters && methodName.startsWith("set") && methodName.length() > 3 && method.getParameterCount() == 1) {
            byConvention = true;
        } else if (includeAllPublic) {
            byConvention = true;
        }

        // If no annotation and not by convention, skip
        if (annotation == null && !byConvention) return null;

        // Build name
        String opName;
        if (annotation != null && !annotation.name().isEmpty()) {
            opName = annotation.name();
        } else {
            opName = toKebabCase(methodName);
        }

        // Build path
        String basePath = apiService != null ? apiService.path() : "";
        String opPath;
        if (annotation != null && !annotation.path().isEmpty()) {
            opPath = basePath + annotation.path();
        } else {
            opPath = basePath + "/" + opName;
        }

        // Build summary/description
        String summary = annotation != null ? annotation.summary() : null;
        String description = annotation != null ? annotation.description() : null;

        // Build operationId
        String operationId = annotation != null ? (annotation.operationId().isEmpty() ? methodName : annotation.operationId()) : methodName;

        // Build parameters
        var params = new ArrayList<ApiParameter>();
        Parameter[] javaParams = method.getParameters();
        Annotation[][] paramAnnoArrays = method.getParameterAnnotations();

        for (int i = 0; i < javaParams.length; i++) {
            Parameter jp = javaParams[i];
            ApiParam ap = null;
            if (paramAnnoArrays != null && i < paramAnnoArrays.length) {
                for (Annotation a : paramAnnoArrays[i]) {
                    if (a instanceof ApiParam) { ap = (ApiParam) a; break; }
                }
            }
            String paramName = ap != null && !ap.name().isEmpty() ? ap.name() : jp.getName();
            ApiDataType paramType = ApiDataType.fromClass(jp.getType());
            boolean required = ap != null ? ap.required() : true;
            String paramDesc = ap != null ? ap.description() : null;

            params.add(new ApiParameter(
                    paramName, paramType, ApiParameterKind.INPUT, required, paramDesc,
                    ap != null ? ap.defaultValue() : null, null, null
            ));
        }

        // Build response type
        ApiDataType response = method.getReturnType() != void.class
                ? ApiDataType.fromClass(method.getReturnType()) : null;

        // Build extensions with path and HTTP methods
        var extensions = new LinkedHashMap<String, Object>();
        extensions.put("path", opPath);
        if (annotation != null && annotation.httpMethods().length > 0) {
            extensions.put("httpMethods", List.of(annotation.httpMethods()));
        }

        // Build tags
        List<String> tags = new ArrayList<>();
        if (apiService != null) {
            tags.addAll(List.of(apiService.tags()));
        }
        if (annotation != null) {
            tags.addAll(List.of(annotation.tags()));
        }

        // Build roles from access annotations
        var roles = new ArrayList<String>();
        var accessAnno = method.getAnnotation(ApiAccess.class);
        if (accessAnno != null) roles.addAll(List.of(accessAnno.roles()));
        if (apiService != null) roles.addAll(List.of(apiService.roles()));
        if (!roles.isEmpty()) extensions.put("roles", roles);

        return new ApiOperation(
                opName, description, summary, operationId,
                List.copyOf(params), response, null,
                List.copyOf(tags),
                annotation != null && annotation.deprecated(),
                annotation != null && annotation.hidden(),
                extensions
        );
    }

    /**
     * Register a Java type as an ApiDataType in the global types map,
     * building object schemas for complex types.
     */
    private void registerType(Map<String, ApiDataType> globalTypes, Class<?> clazz) {
        if (clazz == null || clazz == void.class) return;
        String key = clazz.getName();
        if (globalTypes.containsKey(key)) return;

        ApiDataType type = typeCache.computeIfAbsent(clazz, c -> {
            if (isScalarType(c)) {
                return ApiDataType.fromClass(c);
            }
            // Build object type with properties from fields
            var props = new LinkedHashMap<String, ApiDataType>();
            for (var field : c.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) continue;
                var fieldAnno = field.getAnnotation(ApiProperty.class);
                if (fieldAnno != null && fieldAnno.hidden()) continue;
                String fieldName = fieldAnno != null && !fieldAnno.name().isEmpty() ? fieldAnno.name() : field.getName();
                props.put(fieldName, ApiDataType.fromClass(field.getType()));
            }
            return new ApiDataType(c.getSimpleName(), "object", c.getName(), true, props);
        });

        globalTypes.put(key, type);
    }

    private boolean isScalarType(Class<?> clazz) {
        return clazz.isPrimitive() || clazz == String.class
                || clazz == Integer.class || clazz == Long.class
                || clazz == Double.class || clazz == Float.class
                || clazz == Boolean.class || clazz == Byte.class
                || clazz == Short.class || clazz == Character.class
                || clazz == Void.class;
    }



    /** Convert camelCase method name to kebab-case */
    public static String toKebabCase(String name) {
        // Strip getter/setter prefix
        String stripped = name;
        if ((stripped.startsWith("get") || stripped.startsWith("set")) && stripped.length() > 3) {
            stripped = stripped.substring(3);
        } else if (stripped.startsWith("is") && stripped.length() > 2) {
            stripped = stripped.substring(2);
        }
        // Insert hyphens before uppercase letters, then lowercase everything
        return stripped.replaceAll("([a-z])([A-Z])", "$1-$2").toLowerCase();
    }
}
