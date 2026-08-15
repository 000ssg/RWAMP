package ssg.rwamp.api.provider;

import ssg.rwamp.api.provider.model.*;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ApiDefinition} and {@link ApiGroup}.
 */
class ApiDefinitionTest {

    @Test
    void emptyDefinition() {
        var def = new ApiDefinition("test-api", "1.0.0", "Test", null, null, null);
        assertThat(def.name()).isEqualTo("test-api");
        assertThat(def.version()).isEqualTo("1.0.0");
        assertThat(def.isEmpty()).isTrue();
        assertThat(def.allOperations()).isEmpty();
    }

    @Test
    void definitionWithGroups() {
        var ops = Map.of("get-user", ApiOperation.function("get-user",
                List.of(ApiParameter.input("id", ApiDataType.INTEGER)), ApiDataType.STRING));
        var group = new ApiGroup("users", "User operations", ops, null, null, List.of("crud"));
        var def = new ApiDefinition("my-api", "2.0.0", "My API",
                Map.of("users", group), null, null);

        assertThat(def.isEmpty()).isFalse();
        assertThat(def.allOperations()).hasSize(1);
        assertThat(def.findOperation("get-user")).isNotNull();
        assertThat(def.findOperation("get-user").name()).isEqualTo("get-user");
        assertThat(def.findOperation("nonexistent")).isNull();
    }

    @Test
    void groupFqn() {
        var group = new ApiGroup("users", null, null, null, null, null);
        assertThat(group.fqn()).isEqualTo("users");
        assertThat(group.fqn("api")).isEqualTo("api.users");
        assertThat(group.fqn("v1", "api")).isEqualTo("v1.api.users");
    }

    @Test
    void groupAllOperations_nested() {
        var innerOps = Map.of("inner-op", ApiOperation.procedure("inner-op", List.of()));
        var innerGroup = new ApiGroup("inner", null, innerOps, null, null, null);

        var outerOps = Map.of("outer-op", ApiOperation.procedure("outer-op", List.of()));
        var outerGroup = new ApiGroup("outer", null, outerOps, null,
                List.of(innerGroup), null);

        assertThat(outerGroup.allOperations()).hasSize(2);
    }

    @Test
    void groupIsEmpty() {
        var group = new ApiGroup("empty", null, null, null, null, null);
        assertThat(group.isEmpty()).isTrue();

        var ops = Map.of("op", ApiOperation.procedure("op", List.of()));
        var nonEmpty = new ApiGroup("non-empty", null, ops, null, null, null);
        assertThat(nonEmpty.isEmpty()).isFalse();
    }
}
