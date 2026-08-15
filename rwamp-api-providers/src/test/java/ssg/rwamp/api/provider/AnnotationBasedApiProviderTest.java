package ssg.rwamp.api.provider;

import ssg.rwamp.api.provider.model.ApiDefinition;
import org.junit.jupiter.api.Test;

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
    void build_annotatedClass() {
        var provider = new AnnotationBasedApiProvider("sample-api");
        var def = provider.build(SampleService.class);

        assertThat(def.name()).isEqualTo("sample-api");
        assertThat(def.groups()).containsKey("users");

        var group = def.groups().get("users");
        assertThat(group.description()).isEqualTo("User management API");
        assertThat(group.tags()).containsExactly("admin", "crud");

        var ops = group.operations();
        // get-user, list-users, create-user, get-status, set-description
        // internalMethod and ignoredMethod are excluded
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

        // get-status (getter) and set-description (setter) should be included
        assertThat(ops).containsKey("status");
        assertThat(ops).containsKey("description");

        // Excluded/ignored methods should not be present
        assertThat(ops).doesNotContainKeys("internal-method", "ignored-method");
    }

    @Test
    void build_multipleClasses() {
        var provider = new AnnotationBasedApiProvider("multi-api");
        var def = provider.build(new Class<?>[]{SampleService.class, UserEntity.class});

        // Only SampleService has @ApiService
        assertThat(def.groups()).hasSize(1);
        assertThat(def.groups()).containsKey("users");
    }

    @Test
    void type() {
        var provider = new AnnotationBasedApiProvider("test");
        assertThat(provider.type()).isEqualTo("annotations");
    }
}
