package ssg.rwamp.api.webservices;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility for dynamically loading and inspecting annotation classes
 * without requiring compile-time dependencies.
 * <p>
 * Mirrors xLib's {@code AnnotationsBasedMethodsProvider} pattern:
 * annotation class names are resolved via {@code Class.forName()} at runtime.
 * If the class is absent, methods return {@code null} and the provider
 * reports itself as non-operable.
 *
 * @since 0.1.0
 */
public class AnnotationScanner {

    private static final Logger LOG = Logger.getLogger(AnnotationScanner.class.getName());

    private final ClassLoader classLoader;

    /** Map from logical name to concrete annotation class (lazily loaded) */
    private final Map<String, Class<?>> annotationCache = new HashMap<>();

    public AnnotationScanner() {
        this(Thread.currentThread().getContextClassLoader());
    }

    public AnnotationScanner(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    /**
     * Attempts to load an annotation class by its fully-qualified name.
     * Returns {@code null} if the class is not found or is not an annotation.
     */
    public Class<?> loadAnnotation(String className) {
        return annotationCache.computeIfAbsent(className, name -> {
            try {
                Class<?> clazz = classLoader.loadClass(name);
                if (!clazz.isAnnotation()) {
                    LOG.fine(() -> name + " is not an annotation type");
                    return null;
                }
                return clazz;
            } catch (ClassNotFoundException e) {
                LOG.fine(() -> "Annotation class not found: " + name);
                return null;
            }
        });
    }

    /**
     * Checks if the given annotation instance matches any of the registered class names.
     */
    public boolean matches(Annotation annotation, String... classNames) {
        if (annotation == null) return false;
        for (String name : classNames) {
            Class<?> clazz = loadAnnotation(name);
            if (clazz != null && clazz.isAssignableFrom(annotation.annotationType())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the first annotation on the method that matches any of the given class names.
     */
    public Annotation findMethodAnnotation(Method method, String... classNames) {
        for (Annotation a : method.getDeclaredAnnotations()) {
            if (matches(a, classNames)) return a;
        }
        return null;
    }

    /**
     * Returns the first annotation on the class that matches any of the given class names.
     */
    public Annotation findClassAnnotation(Class<?> clazz, String... classNames) {
        for (Annotation a : clazz.getAnnotations()) {
            if (matches(a, classNames)) return a;
        }
        return null;
    }

    /**
     * Reads a property value from an annotation by method name (simple reflection).
     * Returns {@code null} if the annotation is null or the property does not exist.
     */
    @SuppressWarnings("unchecked")
    public <T> T getAnnotationValue(Annotation annotation, String propertyName, Class<T> expectedType) {
        if (annotation == null || propertyName == null) return null;
        try {
            Method method = annotation.annotationType().getDeclaredMethod(propertyName);
            Object value = method.invoke(annotation);
            if (value == null) return null;
            if (expectedType.isInstance(value)) {
                return (T) value;
            }
            // Handle array-to-list conversion for common annotation array properties
            if (value.getClass().isArray() && List.class.isAssignableFrom(expectedType)) {
                return (T) Arrays.asList((Object[]) value);
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Convenience: read a String value from an annotation.
     */
    public String getString(Annotation annotation, String propertyName) {
        return getAnnotationValue(annotation, propertyName, String.class);
    }

    /**
     * Convenience: read a String[] or List<String> value from an annotation.
     */
    @SuppressWarnings("unchecked")
    public List<String> getStringList(Annotation annotation, String propertyName) {
        if (annotation == null || propertyName == null) return List.of();
        try {
            Method method = annotation.annotationType().getDeclaredMethod(propertyName);
            Object value = method.invoke(annotation);
            if (value == null) return List.of();
            if (value instanceof List<?> list) {
                var result = new ArrayList<String>();
                for (Object item : list) {
                    if (item instanceof String s) result.add(s);
                }
                return result;
            }
            if (value instanceof String[] arr) {
                return List.of(arr);
            }
        } catch (Exception ignored) {
        }
        return List.of();
    }

    /**
     * Convenience: read a boolean value from an annotation.
     */
    public Boolean getBoolean(Annotation annotation, String propertyName) {
        return getAnnotationValue(annotation, propertyName, Boolean.class);
    }

    /**
     * Returns all annotation class names that are actually loadable.
     */
    public Map<String, Class<?>> loadableAnnotations(String... classNames) {
        var result = new LinkedHashMap<String, Class<?>>();
        for (String name : classNames) {
            Class<?> clazz = loadAnnotation(name);
            if (clazz != null) result.put(name, clazz);
        }
        return result;
    }

    /**
     * Checks if at least one of the given annotation class names is loadable.
     */
    public boolean hasAny(String... classNames) {
        for (String name : classNames) {
            if (loadAnnotation(name) != null) return true;
        }
        return false;
    }

    /**
     * Checks if all of the given annotation class names are loadable.
     */
    public boolean hasAll(String... classNames) {
        for (String name : classNames) {
            if (loadAnnotation(name) == null) return false;
        }
        return true;
    }
}
