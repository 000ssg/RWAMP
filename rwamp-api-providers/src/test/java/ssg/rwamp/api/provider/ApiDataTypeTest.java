package ssg.rwamp.api.provider;

import ssg.rwamp.api.provider.model.ApiDataType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ApiDataType}.
 */
class ApiDataTypeTest {

    @Test
    void predefinedTypes() {
        assertThat(ApiDataType.STRING.name()).isEqualTo("string");
        assertThat(ApiDataType.STRING.javaType()).isEqualTo("java.lang.String");
        assertThat(ApiDataType.STRING.isScalar()).isTrue();

        assertThat(ApiDataType.INTEGER.name()).isEqualTo("integer");
        assertThat(ApiDataType.INTEGER.javaType()).isEqualTo("java.lang.Integer");

        assertThat(ApiDataType.NUMBER.name()).isEqualTo("number");
        assertThat(ApiDataType.BOOLEAN.name()).isEqualTo("boolean");
    }

    @Test
    void fromClass_scalars() {
        assertThat(ApiDataType.fromClass(String.class)).isEqualTo(ApiDataType.STRING);
        assertThat(ApiDataType.fromClass(Integer.class)).isEqualTo(ApiDataType.INTEGER);
        assertThat(ApiDataType.fromClass(int.class)).isEqualTo(ApiDataType.INTEGER);
        assertThat(ApiDataType.fromClass(Long.class)).isEqualTo(ApiDataType.INTEGER);
        assertThat(ApiDataType.fromClass(Double.class)).isEqualTo(ApiDataType.NUMBER);
        assertThat(ApiDataType.fromClass(float.class)).isEqualTo(ApiDataType.NUMBER);
        assertThat(ApiDataType.fromClass(Boolean.class)).isEqualTo(ApiDataType.BOOLEAN);
        assertThat(ApiDataType.fromClass(boolean.class)).isEqualTo(ApiDataType.BOOLEAN);
    }

    @Test
    void fromClass_customType() {
        var type = ApiDataType.fromClass(List.class);
        assertThat(type.name()).isEqualTo("List");
        assertThat(type.javaType()).isEqualTo("java.util.List");
        assertThat(type.nullable()).isTrue();
    }

    @Test
    void fromClass_nullAndObject() {
        assertThat(ApiDataType.fromClass(null)).isEqualTo(ApiDataType.OBJECT);
        assertThat(ApiDataType.fromClass(Object.class)).isEqualTo(ApiDataType.OBJECT);
    }

    @Test
    void isObject_withProperties() {
        var type = new ApiDataType("User", "object", "com.app.User", true,
                Map.of("name", ApiDataType.STRING, "age", ApiDataType.INTEGER));
        assertThat(type.isObject()).isTrue();
        assertThat(type.isScalar()).isFalse();
        assertThat(type.properties()).hasSize(2);
    }

    @Test
    void isObject_nullFormatButWithProperties() {
        var type = new ApiDataType("User", null, "com.app.User", true,
                Map.of("name", ApiDataType.STRING));
        assertThat(type.isObject()).isTrue();
    }
}
