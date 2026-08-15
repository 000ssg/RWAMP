package ssg.rwamp.api.provider;

import ssg.rwamp.api.provider.model.ApiDefinition;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link CombinedApiProvider}.
 */
class CombinedApiProviderTest {

    @Test
    void mergesMultipleProviders() {
        var combined = new CombinedApiProvider("merged-api", "2.0.0")
                .addProvider(new AnnotationBasedApiProvider("anno-api"))
                .addProvider(new GetterSetterApiProvider("bean-api"));

        assertThat(combined.canHandle(SampleService.class)).isTrue();
        assertThat(combined.canHandle(SampleBean.class)).isTrue();

        var def = combined.build(SampleService.class);
        assertThat(def.name()).isEqualTo("merged-api");
        assertThat(def.version()).isEqualTo("2.0.0");

        // Only annotation provider handles SampleService
        assertThat(def.groups()).containsKey("users");
    }

    @Test
    void mergesSameTarget_fromDifferentProviders() {
        var combined = new CombinedApiProvider("combined-api")
                .addProvider(new AnnotationBasedApiProvider("a-api").includeGetters(true))
                .addProvider(new GetterSetterApiProvider("b-api"));

        var def = combined.build(SampleBean.class);
        var ops = def.groups().get("SampleBean").operations();

        // Both providers contribute to SampleBean
        assertThat(ops).containsKey("get-name");
        assertThat(ops).containsKey("set-name");
    }

    @Test
    void canHandle_noProviderMatches() {
        var combined = new CombinedApiProvider("empty-api")
                .addProvider(new AnnotationBasedApiProvider("a"));

        assertThat(combined.canHandle(SampleBean.class)).isFalse();
    }

    @Test
    void type() {
        var combined = new CombinedApiProvider("test");
        assertThat(combined.type()).isEqualTo("combined");
    }
}
