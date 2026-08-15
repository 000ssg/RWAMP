package ssg.rwamp.api.webservices;

import ssg.rwamp.api.provider.ApiProvider;
import ssg.rwamp.api.provider.model.ApiDataType;
import ssg.rwamp.api.provider.model.ApiDefinition;
import ssg.rwamp.api.provider.model.ApiGroup;
import ssg.rwamp.api.provider.model.ApiOperation;
import ssg.rwamp.api.provider.model.ApiParameter;
import ssg.rwamp.api.provider.model.ApiParameterKind;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * Generic API provider that discovers operations via user-configured annotation class names.
 * <p>
 * Mirrors xLib's {@code AnnotationsBasedMethodsProvider} design: annotation classes
 * are resolved at runtime via {@code Class.forName()}, enabling zero compile-time
 * dependency on the annotation library.
 * <p>
 * The provider requires three annotation class names:
 * <ul>
 *   <li><b>Type annotation</b> — marks a class as an API service</li>
 *   <li><b>Method annotation</b> — marks a method as an API operation</li>
 *   <li><b>Parameter annotation</b> — describes a method parameter</li>
 * </ul>
 * <p>
 * If any required annotation class is not found on the classpath,
 * {@link #isOperable()} returns {@code false} and the provider is effectively disabled.
 *
 * <h3>Example: JAX-RS</h3>
 * <pre>{@code
 * var provider = new GenericAnnotationApiProvider("my-jaxrs-api")
 *     .setTypeAnnotations("jakarta.ws.rs.Path")
 *     .setMethodAnnotations("jakarta.ws.rs.GET", "jakarta.ws.rs.POST")
 *     .setParameterAnnotations("jakarta.ws.rs.PathParam", "jakarta.ws.rs.QueryParam");
 * }</pre>
 *
 * @since 0.1.0
 * @see ssg.rwamp.api.webservices.JaxRsApiProvider
 */
public class GenericAnnotationApiProvider implements ApiProvider {

    private final String apiName;
    private final AnnotationScanner scanner;

    private List<String> typeAnnotations = List.of();
    private List<String> methodAnnotations = List.of();
    private List<String> parameterAnnotations = List.of();

    // Property name defaults
    private String typePathProperty = "value";
    private String methodPathProperty = "value";
    private String httpMethodProperty = "value";
    private String paramNameProperty = "value";
    private String paramSourceProperty = null;

    // Consumes/produces annotations
    private List<String> consumesAnnotations = List.of();
    private List<String> producesAnnotations = List.of();
    private String consumesProperty = "value";
    private String producesProperty = "value";

    // Access/roles annotations
    private List<String> accessAnnotations = List.of();
    private String rolesProperty = "value";

    private static final Set<String> SKIP_METHODS = Set.of(
            "equals", "hashCode", "toString", "getClass", "notify", "notifyAll", "wait"
    );

    public GenericAnnotationApiProvider(String apiName) {
        this(apiName, new AnnotationScanner());
    }

    public GenericAnnotationApiProvider(String apiName, AnnotationScanner scanner) {
        this.apiName = apiName;
        this.scanner = scanner;
    }

    public GenericAnnotationApiProvider setTypeAnnotations(String... classNames) {
        this.typeAnnotations = List.of(classNames);
        return this;
    }

    public GenericAnnotationApiProvider setMethodAnnotations(String... classNames) {
        this.methodAnnotations = List.of(classNames);
        return this;
    }

    public GenericAnnotationApiProvider setParameterAnnotations(String... classNames) {
        this.parameterAnnotations = List.of(classNames);
        return this;
    }

    public GenericAnnotationApiProvider setConsumesAnnotations(String... classNames) {
        this.consumesAnnotations = List.of(classNames);
        return this;
    }

    public GenericAnnotationApiProvider setProducesAnnotations(String... classNames) {
        this.producesAnnotations = List.of(classNames);
        return this;
    }

    public GenericAnnotationApiProvider setAccessAnnotations(String... classNames) {
        this.accessAnnotations = List.of(classNames);
        return this;
    }

    public GenericAnnotationApiProvider setTypePathProperty(String name) {
        this.typePathProperty = name;
        return this;
    }

    public GenericAnnotationApiProvider setMethodPathProperty(String name) {
        this.methodPathProperty = name;
        return this;
    }

    public GenericAnnotationApiProvider setHttpMethodProperty(String name) {
        this.httpMethodProperty = name;
        return this;
    }

    public GenericAnnotationApiProvider setParamNameProperty(String name) {
        this.paramNameProperty = name;
        return this;
    }

    public GenericAnnotationApiProvider setParamSourceProperty(String name) {
        this.paramSourceProperty = name;
        return this;
    }

    public GenericAnnotationApiProvider setConsumesProperty(String name) {
        this.consumesProperty = name;
        return this;
    }

    public GenericAnnotationApiProvider setProducesProperty(String name) {
        this.producesProperty = name;
        return this;
    }

    public GenericAnnotationApiProvider setRolesProperty(String name) {
        this.rolesProperty = name;
        return this;
    }

    public boolean isOperable() {
        if (typeAnnotations.isEmpty() && methodAnnotations.isEmpty()) return false;
        boolean hasType = scanner.hasAny(typeAnnotations.toArray(String[]::new));
        boolean hasMethod = scanner.hasAny(methodAnnotations.toArray(String[]::new));
        return hasType || hasMethod;
    }

    @Override
    public boolean canHandle(Object target) {
        if (target == null) return false;
        if (!isOperable()) return false;

        Class<?> clazz = resolveClass(target);
        if (clazz == null) return false;

        if (!typeAnnotations.isEmpty()) {
            Annotation typeAnno = scanner.findClassAnnotation(clazz, typeAnnotations.toArray(String[]::new));
            if (typeAnno != null) return true;
        }

        if (!methodAnnotations.isEmpty()) {
            for (Method m : clazz.getDeclaredMethods()) {
                if (!Modifier.isPublic(m.getModifiers())) continue;
                if (Modifier.isStatic(m.getModifiers())) continue;
                if (SKIP_METHODS.contains(m.getName())) continue;
                Annotation methodAnno = scanner.findMethodAnnotation(m, methodAnnotations.toArray(String[]::new));
                if (methodAnno != null) return true;
            }
        }
        return false;
    }

    @Override
    public ApiDefinition build(Object target) {
        if (!canHandle(target)) {
            return new ApiDefinition(apiName, "1.0.0", null, Map.of(), Map.of(), Map.of());
        }

        Class<?> clazz = resolveClass(target);
        if (clazz == null) {
            return new ApiDefinition(apiName, "1.0.0", null, Map.of(), Map.of(), Map.of());
        }

        var groups = new LinkedHashMap<String, ApiGroup>();
        var globalTypes = new LinkedHashMap<String, ApiDataType>();

        String classPath = null;
        if (!typeAnnotations.isEmpty()) {
            Annotation typeAnno = scanner.findClassAnnotation(clazz, typeAnnotations.toArray(String[]::new));
            if (typeAnno != null) {
                String pathValue = scanner.getString(typeAnno, typePathProperty);
                if (pathValue != null && !pathValue.isEmpty()) {
                    classPath = pathValue;
                }
            }
        }

        if (classPath == null) {
            classPath = "/" + clazz.getSimpleName();
        }

        String groupName = classPath.stripLeading();

        var classConsumes = extractMediaTypes(clazz.getAnnotations(), consumesAnnotations, consumesProperty);
        var classProduces = extractMediaTypes(clazz.getAnnotations(), producesAnnotations, producesProperty);
        var classRoles = extractRoles(clazz.getAnnotations());

        var ops = new LinkedHashMap<String, ApiOperation>();
        for (Method m : clazz.getDeclaredMethods()) {
            if (!Modifier.isPublic(m.getModifiers())) continue;
            if (Modifier.isStatic(m.getModifiers())) continue;
            if (SKIP_METHODS.contains(m.getName())) continue;

            ApiOperation op = extractOperation(m, classPath, classConsumes, classProduces);
            if (op != null) {
                ops.put(op.name(), op);
            }

            for (var param : m.getParameters()) {
                registerType(globalTypes, param.getType());
            }
            registerType(globalTypes, m.getReturnType());
        }

        groups.put(groupName, new ApiGroup(
                groupName, null, ops, Map.of(), List.of(),
                classRoles.isEmpty() ? List.of() : classRoles
        ));

        return new ApiDefinition(apiName, "1.0.0", null, groups, globalTypes, Map.of());
    }

    private ApiOperation extractOperation(Method method, String classPath,
                                          List<String> classConsumes, List<String> classProduces) {
        Annotation methodAnno = null;
        String httpMethod = null;

        if (!methodAnnotations.isEmpty()) {
            methodAnno = scanner.findMethodAnnotation(method, methodAnnotations.toArray(String[]::new));
            if (methodAnno != null) {
                httpMethod = scanner.getString(methodAnno, httpMethodProperty);
                if (httpMethod == null || httpMethod.isEmpty()) {
                    httpMethod = methodAnno.annotationType().getSimpleName();
                }
            }
        }

        if (methodAnno == null && httpMethod == null) return null;

        String methodPath = null;
        if (methodAnno != null) {
            methodPath = scanner.getString(methodAnno, methodPathProperty);
        }

        String fullPath = classPath != null ? classPath : "";
        if (methodPath != null && !methodPath.isEmpty()) {
            fullPath = fullPath + (methodPath.startsWith("/") ? methodPath : "/" + methodPath);
        }
        if (fullPath.isEmpty()) {
            fullPath = "/" + method.getName();
        }

        String description = null;
        if (methodAnno != null) {
            String[] descProps = {"description", "summary"};
            for (String prop : descProps) {
                description = scanner.getString(methodAnno, prop);
                if (description != null && !description.isEmpty()) break;
            }
        }
        if (description == null || description.isEmpty()) {
            description = method.getName();
        }

        String opName = method.getName();

        // Build parameters
        var params = new ArrayList<ApiParameter>();
        java.lang.reflect.Parameter[] javaParams = method.getParameters();
        Annotation[][] paramAnnoArrays = method.getParameterAnnotations();

        for (int i = 0; i < javaParams.length; i++) {
            var jp = javaParams[i];
            String paramName = jp.getName();
            String paramSource = null;
            String paramDesc = null;
            boolean required = true;

            if (paramAnnoArrays != null && i < paramAnnoArrays.length) {
                for (Annotation pa : paramAnnoArrays[i]) {
                    if (scanner.matches(pa, parameterAnnotations.toArray(String[]::new))) {
                        String nameVal = scanner.getString(pa, paramNameProperty);
                        if (nameVal != null && !nameVal.isEmpty()) {
                            paramName = nameVal;
                        }
                        String sourceVal = scanner.getString(pa, paramSourceProperty);
                        if (sourceVal != null) {
                            paramSource = sourceVal;
                        } else {
                            paramSource = inferParameterSource(pa);
                        }
                        String descVal = scanner.getString(pa, "description");
                        if (descVal != null && !descVal.isEmpty()) {
                            paramDesc = descVal;
                        }
                        String defVal = scanner.getString(pa, "defaultValue");
                        if (defVal != null && !defVal.isEmpty() && !"__NOT_SET".equals(defVal)) {
                            required = false;
                        }
                        break;
                    }
                }
            }

            ApiDataType paramType = ApiDataType.fromClass(jp.getType());
            var paramExt = new java.util.LinkedHashMap<String, Object>();
            if (paramSource != null) paramExt.put("source", paramSource);
            params.add(new ApiParameter(
                    paramName, paramType, ApiParameterKind.INPUT, required, paramDesc,
                    null, null, paramExt
            ));
        }

        ApiDataType response = method.getReturnType() != void.class
                ? ApiDataType.fromClass(method.getReturnType()) : null;

        var mConsumes = extractMediaTypes(method.getAnnotations(), consumesAnnotations, consumesProperty);
        var mProduces = extractMediaTypes(method.getAnnotations(), producesAnnotations, producesProperty);
        if (mConsumes.isEmpty() && !classConsumes.isEmpty()) mConsumes = classConsumes;
        if (mProduces.isEmpty() && !classProduces.isEmpty()) mProduces = classProduces;

        var methodRoles = extractRoles(method.getAnnotations());

        var extensions = new LinkedHashMap<String, Object>();
        extensions.put("path", fullPath);
        if (httpMethod != null) extensions.put("httpMethod", httpMethod);
        if (!mConsumes.isEmpty()) extensions.put("consumes", mConsumes);
        if (!mProduces.isEmpty()) extensions.put("produces", mProduces);
        if (!methodRoles.isEmpty()) extensions.put("roles", methodRoles);

        return new ApiOperation(
                opName, description, description, opName,
                List.copyOf(params), response, null,
                List.of(), false, false, extensions
        );
    }

    private String inferParameterSource(Annotation annotation) {
        String simpleName = annotation.annotationType().getSimpleName().toLowerCase();
        if (simpleName.contains("pathparam") || simpleName.contains("pathparameter"))
            return WsParameter.Source.PATH.name();
        if (simpleName.contains("queryparam") || simpleName.contains("queryparameter"))
            return WsParameter.Source.QUERY.name();
        if (simpleName.contains("headerparam") || simpleName.contains("headerparameter"))
            return WsParameter.Source.HEADER.name();
        if (simpleName.contains("formparam") || simpleName.contains("formparameter"))
            return WsParameter.Source.FORM.name();
        if (simpleName.contains("cookieparam") || simpleName.contains("cookieparameter"))
            return WsParameter.Source.COOKIE.name();
        if (simpleName.contains("matrixparam") || simpleName.contains("matrixparameter"))
            return WsParameter.Source.MATRIX.name();
        return WsParameter.Source.BODY.name();
    }

    private List<String> extractMediaTypes(Annotation[] annotations, List<String> annotationNames, String propertyName) {
        if (annotationNames.isEmpty()) return List.of();
        var result = new ArrayList<String>();
        for (Annotation a : annotations) {
            if (scanner.matches(a, annotationNames.toArray(String[]::new))) {
                List<String> values = scanner.getStringList(a, propertyName);
                result.addAll(values);
            }
        }
        return List.copyOf(result);
    }

    private List<String> extractRoles(Annotation[] annotations) {
        if (accessAnnotations.isEmpty()) return List.of();
        var result = new ArrayList<String>();
        for (Annotation a : annotations) {
            if (scanner.matches(a, accessAnnotations.toArray(String[]::new))) {
                List<String> roles = scanner.getStringList(a, rolesProperty);
                result.addAll(roles);
            }
        }
        return List.copyOf(result);
    }

    private Class<?> resolveClass(Object target) {
        if (target instanceof Class<?> clazz) return clazz;
        if (target instanceof Object obj) return obj.getClass();
        return null;
    }

    private void registerType(Map<String, ApiDataType> globalTypes, Class<?> clazz) {
        if (clazz == null || clazz == void.class) return;
        String key = clazz.getName();
        if (!globalTypes.containsKey(key)) {
            globalTypes.put(key, ApiDataType.fromClass(clazz));
        }
    }

    @Override
    public String type() {
        return "generic-annotations";
    }
}
