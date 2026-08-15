package ssg.rwamp.api.publisher;

import ssg.rwamp.api.provider.AnnotationBasedApiProvider;
import ssg.rwamp.api.provider.model.*;
import ssg.rwamp.api.publisher.openapi.OpenApiPublisher;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiPublisherTest {

    @Test
    void publish_basic() {
        var api = new ApiDefinition("test-api", "1.0.0", "Test API",
                Map.of("users", new ApiGroup("users", "Users",
                        Map.of("get-user", ApiOperation.function("get-user",
                                List.of(ApiParameter.input("id", ApiDataType.INTEGER)),
                                ApiDataType.STRING
                        )), null, null, List.of("crud"))
                ), null, null);

        var publisher = new OpenApiPublisher()
                .serverUrl("https://api.example.com")
                .license("MIT");

        var spec = (Map<String, Object>) publisher.publish(api);

        assertThat(spec.get("openapi")).isEqualTo("3.1.0");
        assertThat(((Map) spec.get("info")).get("title")).isEqualTo("test-api");
        assertThat(((Map) spec.get("info")).get("version")).isEqualTo("1.0.0");
        assertThat(((Map) spec.get("info")).get("license")).isNotNull();
        assertThat(spec.get("paths")).isInstanceOf(Map.class);
    }

    @Test
    void publishAsJson() {
        var api = new ApiDefinition("my-api", "2.0.0", null,
                Map.of("items", new ApiGroup("items", null,
                        Map.of("list-items", ApiOperation.function("list-items",
                                List.of(), ApiDataType.STRING
                        )), null, null, null)
                ), null, null);

        var json = new OpenApiPublisher().publishAsJson(api);
        assertThat(json).contains("\"openapi\": \"3.1.0\"");
        assertThat(json).contains("\"title\": \"my-api\"");
        assertThat(json).contains("\"version\": \"2.0.0\"");
    }

    @Test
    void publishAsYaml() {
        var api = new ApiDefinition("yaml-api", "1.0.0", "YAML test",
                Map.of("greet", new ApiGroup("greet", null,
                        Map.of("hello", ApiOperation.function("hello",
                                List.of(ApiParameter.input("name", ApiDataType.STRING)),
                                ApiDataType.STRING
                        )), null, null, null)
                ), null, null);

        var yaml = new OpenApiPublisher().publishAsYaml(api);
        assertThat(yaml).contains("openapi: 3.1.0");
        assertThat(yaml).contains("title: yaml-api");
        assertThat(yaml).contains("version: 1.0.0");
        assertThat(yaml).contains("/hello:");
    }

    @Test
    void publish_fromProvider() {
        var provider = new AnnotationBasedApiProvider("sample-api");
        var def = provider.build(SampleService.class);

        var publisher = new OpenApiPublisher()
                .serverUrl("http://localhost:8080");

        var spec = (Map<String, Object>) publisher.publish(def);
        assertThat(spec.get("paths")).isNotNull();
    }

    @Test
    void publish_emptyApi() {
        var api = new ApiDefinition("empty", "1.0.0", null, null, null, null);
        var spec = (Map<String, Object>) new OpenApiPublisher().publish(api);
        assertThat(spec.get("paths")).isNotNull();
        assertThat(spec.get("components")).isNotNull();
    }
}
