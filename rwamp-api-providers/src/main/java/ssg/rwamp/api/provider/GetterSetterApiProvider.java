package ssg.rwamp.api.provider;

import ssg.rwamp.api.provider.model.*;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * Derives API operations from JavaBean getter/setter method pairs.
 * <p>
 * For every {@code getX()/setX()} pair found on a class, this provider
 * creates two operations: a read (from the getter) and a write (from the setter).
 * This mimics xLib's property-based API discovery where bean properties
 * become API data points.
 * <p>
 * Useful for exposing CRUD-like operations on data classes without
 * requiring explicit annotations.
 * <p>
 * Usage:
 * <pre>{@code
 * var provider = new GetterSetterApiProvider("user-api");
 * var api = provider.build(User.class);
 * // Produces operations: "get-user" (read), "set-user" (write)
 * }</pre>
 *
 * @since 0.1.0
 */
public class GetterSetterApiProvider implements ApiProvider {

    private final String apiName;

    /**
     * Creates a provider that scans for getter/setter pairs.
     *
     * @param apiName the root API name
     */
    public GetterSetterApiProvider(String apiName) {
        this.apiName = apiName;
    }

    @Override
    public boolean canHandle(Object target) {
        if (target == null || !(target instanceof Class<?> clazz)) return false;
        for (Method m : clazz.getMethods()) {
            String name = m.getName();
            if ((name.startsWith("get") || name.startsWith("is")) && name.length() > 2
                    && !Modifier.isStatic(m.getModifiers()) && Modifier.isPublic(m.getModifiers())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public ApiDefinition build(Object target) {
        if (!(target instanceof Class<?> clazz)) {
            return new ApiDefinition(apiName, "1.0.0", null, null, null, null);
        }

        var ops = new LinkedHashMap<String, ApiOperation>();
        var globalTypes = new LinkedHashMap<String, ApiDataType>();
        String groupName = clazz.getSimpleName();

        for (Method m : clazz.getDeclaredMethods()) {
            if (Modifier.isStatic(m.getModifiers()) || !Modifier.isPublic(m.getModifiers())) continue;

            String name = m.getName();
            // Process getters
            if ((name.startsWith("get") || (name.startsWith("is") && m.getReturnType() == boolean.class))
                    && name.length() > 2 && m.getParameterCount() == 0) {
                String propName = toPropertyName(name);
                ApiDataType returnType = ApiDataType.fromClass(m.getReturnType());
                registerType(globalTypes, m.getReturnType());

                ops.put("get-" + toKebabCase(propName),
                        ApiOperation.function(
                                "get-" + toKebabCase(propName),
                                List.of(),
                                returnType
                        ));
            }

            // Process setters
            if (name.startsWith("set") && name.length() > 3 && m.getParameterCount() == 1) {
                String propName = toPropertyName(name);
                var param = m.getParameters()[0];
                ApiDataType paramType = ApiDataType.fromClass(param.getType());
                registerType(globalTypes, param.getType());

                ops.put("set-" + toKebabCase(propName),
                        ApiOperation.procedure(
                                "set-" + toKebabCase(propName),
                                List.of(new ApiParameter(
                                        propName, paramType, ApiParameterKind.INPUT, true, null, null, null, null
                                ))
                        ));
            }
        }

        var group = new ApiGroup(groupName, null, ops, null, null, List.of("bean"));
        return new ApiDefinition(apiName, "1.0.0", null,
                Map.of(groupName, group), globalTypes, Map.of());
    }

    private void registerType(Map<String, ApiDataType> map, Class<?> clazz) {
        if (clazz == null || clazz.isPrimitive() || clazz == String.class || clazz == Void.class) return;
        String key = clazz.getName();
        if (!map.containsKey(key)) {
            map.put(key, ApiDataType.fromClass(clazz));
        }
    }

    private String toPropertyName(String methodName) {
        String prop = methodName.startsWith("is") ? methodName.substring(2) : methodName.substring(3);
        if (!prop.isEmpty() && Character.isUpperCase(prop.charAt(0))) {
            prop = prop.substring(0, 1).toLowerCase() + prop.substring(1);
        }
        return prop;
    }

    private String toKebabCase(String name) {
        return name.replaceAll("([a-z])([A-Z])", "$1-$2").toLowerCase();
    }

    @Override
    public String type() {
        return "getter-setter";
    }
}
