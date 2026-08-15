package ssg.rwamp.api.provider;

import ssg.rwamp.api.provider.model.ApiDefinition;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link GetterSetterApiProvider}.
 */
class GetterSetterApiProviderTest {

    @Test
    void canHandle() {
        var provider = new GetterSetterApiProvider("test");
        assertThat(provider.canHandle(SampleBean.class)).isTrue();
        assertThat(provider.canHandle(null)).isFalse();
    }

    @Test
    void build_getterSetterPairs() {
        var provider = new GetterSetterApiProvider("bean-api");
        var def = provider.build(SampleBean.class);

        assertThat(def.name()).isEqualTo("bean-api");
        assertThat(def.groups()).containsKey("SampleBean");

        var ops = def.groups().get("SampleBean").operations();

        // Getters: name, count, active
        assertThat(ops).containsKey("get-name");
        assertThat(ops).containsKey("get-count");
        assertThat(ops).containsKey("get-active");

        // Setters: name, count, active
        assertThat(ops).containsKey("set-name");
        assertThat(ops).containsKey("set-count");
        assertThat(ops).containsKey("set-active");
    }

    @Test
    void build_operationDetails() {
        var provider = new GetterSetterApiProvider("bean-api");
        var def = provider.build(SampleBean.class);

        var ops = def.groups().get("SampleBean").operations();

        // Getter: no params, has response
        var getName = ops.get("get-name");
        assertThat(getName.parameters()).isEmpty();
        assertThat(getName.response()).isNotNull();

        // Setter: one param, no response
        var setName = ops.get("set-name");
        assertThat(setName.parameters()).hasSize(1);
        assertThat(setName.response()).isNull();
    }

    @Test
    void build_classWithoutGetters() {
        var provider = new GetterSetterApiProvider("empty-api");
        var def = provider.build(UserEntity.class);
        // UserEntity has public fields but no getters
        assertThat(def.groups().get("UserEntity").operations()).isEmpty();
    }

    @Test
    void type() {
        var provider = new GetterSetterApiProvider("test");
        assertThat(provider.type()).isEqualTo("getter-setter");
    }
}
