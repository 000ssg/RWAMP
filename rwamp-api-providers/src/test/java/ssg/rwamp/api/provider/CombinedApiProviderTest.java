package ssg.rwamp.api.provider;

import ssg.rwamp.api.provider.model.ApiDefinition;
import ssg.rwamp.api.provider.model.ApiDataType;
import ssg.rwamp.api.provider.model.ApiGroup;
import ssg.rwamp.api.provider.model.ApiOperation;
import ssg.rwamp.api.provider.model.ApiParameter;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

        assertThat(def.groups()).containsKey("users");
    }

    @Test
    void mergesSameTarget_fromDifferentProviders() {
        var combined = new CombinedApiProvider("combined-api")
                .addProvider(new AnnotationBasedApiProvider("a-api").includeGetters(true))
                .addProvider(new GetterSetterApiProvider("b-api"));

        var def = combined.build(SampleBean.class);
        var ops = def.groups().get("SampleBean").operations();

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

    @Test
    void addProvider_nullIgnored() {
        var combined = new CombinedApiProvider("test");
        var result = combined.addProvider(null);
        assertThat(result).isSameAs(combined);
    }

    @Test
    void addProviders_nullIterableIgnored() {
        var combined = new CombinedApiProvider("test");
        var result = combined.addProviders(null);
        assertThat(result).isSameAs(combined);
    }

    @Test
    void addProviders_fromIterable() {
        var combined = new CombinedApiProvider("test")
                .addProviders(List.of(
                        new AnnotationBasedApiProvider("a"),
                        new GetterSetterApiProvider("b")
                ));

        assertThat(combined.canHandle(SampleService.class)).isTrue();
        assertThat(combined.canHandle(SampleBean.class)).isTrue();
    }

    @Test
    void build_noMatchingProviders_returnsEmpty() {
        var combined = new CombinedApiProvider("empty-api")
                .addProvider(new AnnotationBasedApiProvider("anno"));

        var def = combined.build(SampleBean.class); // SampleBean is not annotated
        assertThat(def.name()).isEqualTo("empty-api");
        assertThat(def.groups()).isEmpty();
    }

    @Test
    void build_mergesGroups_withOverlappingOperations() {
        var def1 = ManualApiProvider.builder("api1")
                .group("shared", "Group 1", g -> g
                        .operation("op1", "Op 1", op -> op.response(ApiDataType.STRING)))
                .build();
        var def2 = ManualApiProvider.builder("api2")
                .group("shared", "Group 2", g -> g
                        .operation("op2", "Op 2", op -> op.response(ApiDataType.INTEGER)))
                .build();

        var combinedProvider = new CombinedApiProvider("final-api", "3.0.0")
                .addProvider(new ApiProvider() {
                    @Override public boolean canHandle(Object t) { return true; }
                    @Override public ApiDefinition build(Object t) { return def1; }
                    @Override public String type() { return "test1"; }
                })
                .addProvider(new ApiProvider() {
                    @Override public boolean canHandle(Object t) { return true; }
                    @Override public ApiDefinition build(Object t) { return def2; }
                    @Override public String type() { return "test2"; }
                });

        var def = combinedProvider.build("any-target");
        assertThat(def.name()).isEqualTo("final-api");
        assertThat(def.version()).isEqualTo("3.0.0");
        assertThat(def.groups()).containsKey("shared");

        var ops = def.groups().get("shared").operations();
        assertThat(ops).containsKeys("op1", "op2");
    }

    @Test
    void build_mergesTags() {
        var apiProvider1 = new ApiProvider() {
            @Override public boolean canHandle(Object t) { return true; }
            @Override public ApiDefinition build(Object t) {
                return new ApiDefinition("t1", "1.0.0", null,
                        Map.of("g1", new ApiGroup("g1", "Desc1",
                                Map.of("op1", ApiOperation.function("op1", List.of(), ApiDataType.STRING)),
                                null, null, List.of("tag1", "tag3"))),
                        null, null);
            }
            @Override public String type() { return "t1"; }
        };

        var apiProvider2 = new ApiProvider() {
            @Override public boolean canHandle(Object t) { return true; }
            @Override public ApiDefinition build(Object t) {
                return new ApiDefinition("t2", "1.0.0", null,
                        Map.of("g1", new ApiGroup("g1", "Desc2",
                                Map.of("op2", ApiOperation.function("op2", List.of(), ApiDataType.INTEGER)),
                                null, null, List.of("tag2", "tag3"))),
                        null, null);
            }
            @Override public String type() { return "t2"; }
        };

        var combined = new CombinedApiProvider("merged-tags")
                .addProvider(apiProvider1)
                .addProvider(apiProvider2);

        var def = combined.build("target");
        var group = def.groups().get("g1");
        assertThat(group.tags()).containsExactlyInAnyOrder("tag1", "tag2", "tag3");
    }

    @Test
    void build_withNullGroupsAndTypes() {
        var apiProvider = new ApiProvider() {
            @Override public boolean canHandle(Object t) { return true; }
            @Override public ApiDefinition build(Object t) {
                return new ApiDefinition("null-def", "1.0.0", null, null, null, null);
            }
            @Override public String type() { return "null-type"; }
        };

        var combined = new CombinedApiProvider("null-test")
                .addProvider(apiProvider);

        var def = combined.build("target");
        assertThat(def.name()).isEqualTo("null-test");
    }

    @Test
    void constructor_nullApiName_throwsException() {
        assertThatThrownBy(() -> new CombinedApiProvider(null));
    }
}
