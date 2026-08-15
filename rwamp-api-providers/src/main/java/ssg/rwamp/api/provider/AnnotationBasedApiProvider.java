package ssg.rwamp.api.provider;

import ssg.rwamp.api.provider.annotations.ApiService;
import ssg.rwamp.api.provider.builder.ReflectionApiBuilder;
import ssg.rwamp.api.provider.model.ApiDefinition;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/**
 * Discovers API operations from classes annotated with {@link ApiService}.
 * <p>
 * This is the RWAMP equivalent of xLib's {@code XMethodsProvider} combined
 * with {@code AnnotationsBasedMethodsProvider}. It scans provided classes
 * for the {@code @ApiService} annotation and uses reflection to extract
 * operations from methods annotated with {@code @ApiOperation} or matching
 * convention-based patterns (getters, setters).
 * <p>
 * Usage:
 * <pre>{@code
 * var provider = new AnnotationBasedApiProvider("my-api");
 * var api = provider.build(ServiceClass.class, AnotherService.class);
 * }</pre>
 *
 * @since 0.1.0
 */
public class AnnotationBasedApiProvider implements ApiProvider {

    private final String apiName;
    private final ReflectionApiBuilder builder;

    /**
     * Creates a provider that scans for {@code @ApiService} annotated classes.
     *
     * @param apiName the root API name
     */
    public AnnotationBasedApiProvider(String apiName) {
        this(apiName, new ReflectionApiBuilder());
    }

    /**
     * Creates a provider with a custom builder configuration.
     *
     * @param apiName the root API name
     * @param builder the reflection builder to use
     */
    public AnnotationBasedApiProvider(String apiName, ReflectionApiBuilder builder) {
        this.apiName = apiName;
        this.builder = builder;
    }

    /** Configure the underlying reflection builder */
    public AnnotationBasedApiProvider includeGetters(boolean v) {
        builder.includeGetters(v);
        return this;
    }

    public AnnotationBasedApiProvider includeSetters(boolean v) {
        builder.includeSetters(v);
        return this;
    }

    public AnnotationBasedApiProvider includeAllPublic(boolean v) {
        builder.includeAllPublic(v);
        return this;
    }

    @Override
    public boolean canHandle(Object target) {
        if (target == null) return false;
        if (target instanceof Class<?> clazz) {
            return clazz.isAnnotationPresent(ApiService.class);
        }
        if (target instanceof Object[] arr) {
            return Arrays.stream(arr).anyMatch(this::canHandle);
        }
        if (target instanceof Iterable<?> iterable) {
            for (var item : iterable) {
                if (canHandle(item)) return true;
            }
            return false;
        }
        return false;
    }

    @Override
    public ApiDefinition build(Object target) {
        Class<?>[] types;
        if (target instanceof Class<?> clazz) {
            types = new Class<?>[]{clazz};
        } else if (target instanceof Object[] arr) {
            types = Arrays.stream(arr)
                    .filter(this::canHandle)
                    .map(t -> (Class<?>) t)
                    .toArray(Class<?>[]::new);
        } else if (target instanceof Iterable<?> iterable) {
            var list = new java.util.ArrayList<Class<?>>();
            for (var item : iterable) {
                if (canHandle(item)) list.add((Class<?>) item);
            }
            types = list.toArray(Class<?>[]::new);
        } else {
            return new ApiDefinition(apiName, "1.0.0", null, null, null, null);
        }
        return builder.build(apiName, types);
    }

    @Override
    public String type() {
        return "annotations";
    }
}
