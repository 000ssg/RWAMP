package ssg.rwamp.api.provider;

import ssg.rwamp.api.provider.builder.ReflectionApiBuilder;
import ssg.rwamp.api.provider.model.ApiDefinition;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ReflectionApiBuilder}.
 */
class ReflectionApiBuilderTest {

    @Test
    void build_simpleClass() {
        var builder = new ReflectionApiBuilder();
        var def = builder.build("test", SampleService.class);

        assertThat(def.name()).isEqualTo("test");
        assertThat(def.groups()).containsKey("users");
        assertThat(def.groups().get("users").description()).isEqualTo("User management API");
    }

    @Test
    void build_noAnnotations() {
        var builder = new ReflectionApiBuilder();
        var def = builder.build("test", UserEntity.class);
        // UserEntity has no @ApiService — still produces a group with simple name
        assertThat(def.groups()).containsKey("UserEntity");
    }

    @Test
    void kebabCase() {
        assertThat(ReflectionApiBuilder.toKebabCase("getUser")).isEqualTo("user");
        assertThat(ReflectionApiBuilder.toKebabCase("isConnected")).isEqualTo("connected");
        assertThat(ReflectionApiBuilder.toKebabCase("createNewOrder")).isEqualTo("create-new-order");
        assertThat(ReflectionApiBuilder.toKebabCase("getId")).isEqualTo("id");
        assertThat(ReflectionApiBuilder.toKebabCase("")).isEqualTo("");
    }

    @Test
    void build_withAllPublic() {
        var builder = new ReflectionApiBuilder().includeAllPublic(true);
        var def = builder.build("test", SampleBean.class);

        var ops = def.groups().get("SampleBean").operations();
        assertThat(ops).containsKeys("name", "count", "active");
    }

    @Test
    void build_typesCollected() {
        var builder = new ReflectionApiBuilder();
        var def = builder.build("test", SampleService.class);

        // Check that parameter/return types are collected
        assertThat(def.types()).isNotEmpty();
    }
}
