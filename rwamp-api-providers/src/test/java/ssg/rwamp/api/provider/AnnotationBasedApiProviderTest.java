package ssg.rwamp.api.provider;

import ssg.rwamp.api.provider.model.ApiDefinition;
import ssg.rwamp.api.provider.builder.ReflectionApiBuilder;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AnnotationBasedApiProvider}.
 */
class AnnotationBasedApiProviderTest {

    @Test
    void canHandle_annotatedClass() {
        var provider = new AnnotationBasedApiProvider("test");
        assertThat(provider.canHandle(SampleService.class)).isTrue();
        assertThat(provider.canHandle(UserEntity.class)).isFalse();
        assertThat(provider.canHandle(null)).isFalse();
    }

    @Test
    void canHandle_arrayOfClasses() {
        var provider = new AnnotationBasedApiProvider("test");
        assertThat(provider.canHandle(new Class<?>[]{SampleService.class})).isTrue();
        assertThat(provider.canHandle(new Class<?>[]{UserEntity.class})).isFalse();
        assertThat(provider.canHandle(new Class<?>[]{SampleService.class, UserEntity.class})).isTrue();
    }

    @Test
    void canHandle_iterableOfClasses() {
        var provider = new AnnotationBasedApiProvider("test");
        assertThat(provider.canHandle(List.of(SampleService.class))).isTrue();
        assertThat(provider.canHandle(List.of(UserEntity.class))).isFalse();
        assertThat(provider.canHandle(List.of(SampleService.class, UserEntity.class))).isTrue();
        assertThat(provider.canHandle(List.<Class<?>>of())).isFalse();
    }

    @Test
    void canHandle_unknownType() {
        var provider = new AnnotationBasedApiProvider("test");
        assertThat(provider.canHandle("not a class")).isFalse();
        assertThat(provider.canHandle(42)).isFalse();
    }

    @Test
    void build_annotatedClass() {
        var provider = new AnnotationBasedApiProvider("sample-api");
        var def = provider.build(SampleService.class);

        assertThat(def.name()).isEqualTo("sample-api");
        assertThat(def.groups()).containsKey("users");

        var group = def.groups().get("users");
        assertThat(group.description()).isEqualTo("User management API");
        assertThat(group.tags()).containsExactly("admin", "crud");

        var ops = group.operations();
        assertThat(ops).containsKeys("user", "list-users", "create-user");
        assertThat(ops.get("user").summary()).isEqualTo("Get user by ID");
        assertThat(ops.get("user").parameters()).hasSize(1);
        assertThat(ops.get("user").parameters().get(0).name()).isEqualTo("userId");
    }

    @Test
    void build_withGettersAndSetters() {
        var provider = new AnnotationBasedApiProvider("sample-api")
                .includeGetters(true)
                .includeSetters(true);
        var def = provider.build(SampleService.class);

        var group = def.groups().get("users");
        var ops = group.operations();

        assertThat(ops).containsKey("status");
        assertThat(ops).containsKey("description");

        assertThat(ops).doesNotContainKeys("internal-method", "ignored-method");
    }

    @Test
    void build_withCustomBuilder() {
        var builder = new ReflectionApiBuilder();
        builder.includeGetters(true);
        builder.includeSetters(true);

        var provider = new AnnotationBasedApiProvider("custom-api", builder);
        var def = provider.build(SampleService.class);

        var group = def.groups().get("users");
        var ops = group.operations();
        assertThat(ops).containsKey("status");
        assertThat(ops).containsKey("description");
    }

    @Test
    void build_includeAllPublic() {
        var provider = new AnnotationBasedApiProvider("all-public-api")
                .includeAllPublic(true);
        var def = provider.build(SampleService.class);

        var group = def.groups().get("users");
        assertThat(group).isNotNull();
    }

    @Test
    void build_multipleClasses() {
        var provider = new AnnotationBasedApiProvider("multi-api");
        var def = provider.build(new Class<?>[]{SampleService.class, UserEntity.class});

        assertThat(def.groups()).hasSize(1);
        assertThat(def.groups()).containsKey("users");
    }

    @Test
    void build_fromIterable() {
        var provider = new AnnotationBasedApiProvider("iterable-api");
        var def = provider.build(List.of(SampleService.class, UserEntity.class));

        assertThat(def.groups()).containsKey("users");
    }

    @Test
    void build_unknownTarget_returnsEmptyDefinition() {
        var provider = new AnnotationBasedApiProvider("empty-api");
        var def = provider.build("not a valid target");

        assertThat(def.name()).isEqualTo("empty-api");
        assertThat(def.groups()).isEmpty();
    }

    @Test
    void type() {
        var provider = new AnnotationBasedApiProvider("test");
        assertThat(provider.type()).isEqualTo("annotations");
    }
}
