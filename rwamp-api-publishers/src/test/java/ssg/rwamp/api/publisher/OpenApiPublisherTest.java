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

    @Test
    void type() {
        assertThat(new OpenApiPublisher().type()).isEqualTo("openapi");
    }

    // ── Additional coverage tests ──

    @Test
    void publish_withContactAndLicense() {
        var api = new ApiDefinition("contact-api", "1.0.0", "With contact",
                Map.of("ops", new ApiGroup("ops", null,
                        Map.of("ping", ApiOperation.function("ping", List.of(), ApiDataType.STRING)),
                        null, null, null)
                ), null, null);

        var publisher = new OpenApiPublisher()
                .serverUrl("https://api.test.com")
                .contact("Dev Team", "dev@test.com")
                .license("Apache-2.0");

        var spec = (Map<String, Object>) publisher.publish(api);
        var info = (Map<String, Object>) spec.get("info");
        assertThat(info).containsKey("contact");
        var contact = (Map<String, String>) info.get("contact");
        assertThat(contact).containsEntry("name", "Dev Team");
        assertThat(contact).containsEntry("email", "dev@test.com");
        assertThat(info).containsKey("license");
    }

    @Test
    void publish_withObjectResponseType() {
        var user = new ApiDataType("User", "object", "com.example.User", false,
                Map.of("name", ApiDataType.STRING, "age", ApiDataType.INTEGER));
        var api = new ApiDefinition("object-api", "1.0.0", null,
                Map.of("users", new ApiGroup("users", null,
                        Map.of("get-user", ApiOperation.function("get-user",
                                List.of(ApiParameter.input("id", ApiDataType.INTEGER)), user)),
                        Map.of("User", user), null, null)
                ), Map.of("User", user), null);

        var spec = (Map<String, Object>) new OpenApiPublisher().publish(api);
        var components = (Map<String, Object>) spec.get("components");
        var schemas = (Map<String, Object>) components.get("schemas");
        assertThat(schemas).containsKey("User");
    }

    @Test
    void publish_withArrayType() {
        var arrayType = new ApiDataType("List", "array", "java.util.List", false,
                Map.of("items", ApiDataType.STRING));
        var api = new ApiDefinition("array-api", "1.0.0", null,
                Map.of("ops", new ApiGroup("ops", null,
                        Map.of("list", ApiOperation.function("list", List.of(), arrayType)),
                        null, null, null)
                ), null, null);

        var spec = (Map<String, Object>) new OpenApiPublisher().publish(api);
        assertThat((Map<?, ?>) spec.get("paths")).isNotEmpty();
    }

    @Test
    void publish_withErrors() {
        var errors = List.of(
                ApiError.of("NOT_FOUND", "Resource not found"),
                ApiError.of("BAD_REQUEST", "Invalid input"),
                ApiError.of("UNAUTHORIZED", "Not authenticated"),
                ApiError.of("FORBIDDEN", "Access denied"),
                ApiError.of("CONFLICT", "Conflict"),
                ApiError.of("RATE_LIMITED", "Rate limited"),
                ApiError.of("UNKNOWN", "Unknown error")
        );
        var op = new ApiOperation("get", null, "Get", "get-op",
                List.of(ApiParameter.input("id", ApiDataType.INTEGER)),
                ApiDataType.STRING, errors, null, false, false, null);

        var api = new ApiDefinition("error-api", "1.0.0", null,
                Map.of("ops", new ApiGroup("ops", null, Map.of("get", op), null, null, null)),
                null, null);

        var spec = (Map<String, Object>) new OpenApiPublisher().publish(api);
        var responses = getResponses(spec);
        assertThat(responses).containsKey("200");
        assertThat(responses).containsKey("404");
        assertThat(responses).containsKey("400");
        assertThat(responses).containsKey("401");
        assertThat(responses).containsKey("403");
        assertThat(responses).containsKey("409");
        assertThat(responses).containsKey("429");
        assertThat(responses).containsKey("500");
    }

    @Test
    void publish_hiddenOperation_excluded() {
        var hiddenOp = new ApiOperation("hidden", null, null, "hidden",
                List.of(), ApiDataType.STRING, null, null, false, true, null);
        var visibleOp = ApiOperation.function("visible", List.of(), ApiDataType.STRING);

        var api = new ApiDefinition("hidden-api", "1.0.0", null,
                Map.of("ops", new ApiGroup("ops", null,
                        Map.of("hidden", hiddenOp, "visible", visibleOp),
                        null, null, null)
                ), null, null);

        var spec = (Map<String, Object>) new OpenApiPublisher().publish(api);
        var paths = (Map<String, Object>) spec.get("paths");
        // Hidden operation should not appear
        for (var entry : paths.entrySet()) {
            var pathItem = (Map<String, Object>) entry.getValue();
            // Check that no HTTP method points to the hidden operation
            for (var method : pathItem.values()) {
                var op = (Map<String, Object>) method;
                assertThat(op.get("operationId")).isNotEqualTo("hidden");
            }
        }
    }

    @Test
    void publish_securityDisabled() {
        var api = new ApiDefinition("no-security", "1.0.0", null,
                Map.of("ops", new ApiGroup("ops", null,
                        Map.of("ping", ApiOperation.function("ping", List.of(), ApiDataType.STRING)),
                        null, null, null)
                ), null, null);

        var publisher = new OpenApiPublisher().securityEnabled(false);
        var spec = (Map<String, Object>) publisher.publish(api);
        // When security is disabled, the top-level security field should be absent
        assertThat(spec.containsKey("security")).isFalse();
    }

    @Test
    void publish_securityEnabled_default() {
        var api = new ApiDefinition("secure-api", "1.0.0", null,
                Map.of("ops", new ApiGroup("ops", null,
                        Map.of("ping", ApiOperation.function("ping", List.of(), ApiDataType.STRING)),
                        null, null, null)
                ), null, null);

        var spec = (Map<String, Object>) new OpenApiPublisher().publish(api);
        var components = (Map<String, Object>) spec.get("components");
        assertThat(components).containsKey("securitySchemes");
    }

    @Test
    void publish_withNullableTypes() {
        var nullableStr = new ApiDataType("string", null, "java.lang.String", true, null);
        var api = new ApiDefinition("nullable-api", "1.0.0", null,
                Map.of("ops", new ApiGroup("ops", null,
                        Map.of("get", ApiOperation.function("get",
                                List.of(ApiParameter.input("name", nullableStr)),
                                nullableStr)),
                        null, null, null)
                ), null, null);

        var spec = (Map<String, Object>) new OpenApiPublisher().publish(api);
        assertThat(spec.get("paths")).isNotNull();
    }

    @Test
    void publish_withParameterDefaultsAndExamples() {
        var param = new ApiParameter("limit", ApiDataType.INTEGER, ApiParameterKind.INPUT,
                false, "Max results", 50, List.of(10, 20, 50), Map.of("in", "query"));
        var api = new ApiDefinition("param-api", "1.0.0", null,
                Map.of("ops", new ApiGroup("ops", null,
                        Map.of("list", ApiOperation.function("list",
                                List.of(param), ApiDataType.STRING)),
                        null, null, null)
                ), null, null);

        var spec = (Map<String, Object>) new OpenApiPublisher().publish(api);
        assertThat(spec.get("paths")).isNotNull();
    }

    @Test
    void publish_noResponse_procedure() {
        var proc = ApiOperation.procedure("delete", List.of(ApiParameter.input("id", ApiDataType.INTEGER)));
        var api = new ApiDefinition("proc-api", "1.0.0", null,
                Map.of("ops", new ApiGroup("ops", null, Map.of("delete", proc), null, null, null)),
                null, null);

        var spec = (Map<String, Object>) new OpenApiPublisher().publish(api);
        var responses = getResponses(spec);
        assertThat(responses).containsKey("204");
    }

    @Test
    void publish_extensions() {
        var api = new ApiDefinition("ext-api", "1.0.0", "With extensions",
                Map.of("ops", new ApiGroup("ops", null,
                        Map.of("ping", ApiOperation.function("ping", List.of(), ApiDataType.STRING)),
                        null, null, null)
                ), null,
                Map.of("x-custom", "value", "title", "override", "version", "override"));

        var spec = (Map<String, Object>) new OpenApiPublisher().publish(api);
        var info = (Map<String, Object>) spec.get("info");
        assertThat(info).containsEntry("x-custom", "value");
        assertThat(info.get("title")).isEqualTo("ext-api");
        assertThat(info.get("version")).isEqualTo("1.0.0");
    }

    @Test
    void publishAsJson_specialCharacters() {
        var api = new ApiDefinition("special-api", "1.0.0", null,
                Map.of("ops", new ApiGroup("ops", null,
                        Map.of("test", ApiOperation.function("test",
                                List.of(), ApiDataType.STRING)),
                        null, null, null)
                ), null, null);

        var publisher = new OpenApiPublisher()
                .contact("Dev \"Team\"", "test@example.com");
        var json = publisher.publishAsJson(api);
        assertThat(json).contains("\\\"");
    }

    @Test
    void publishAsYaml_specialStrings() {
        var api = new ApiDefinition("yaml-test", "1.0.0", "Has: colon and #hash",
                Map.of("ops", new ApiGroup("ops", null,
                        Map.of("test", ApiOperation.function("test",
                                List.of(), ApiDataType.STRING)),
                        null, null, null)
                ), null, null);

        var yaml = new OpenApiPublisher().publishAsYaml(api);
        assertThat(yaml).contains("yaml-test");
    }

    @Test
    void publish_parameterInPath() {
        var param = new ApiParameter("id", ApiDataType.INTEGER, ApiParameterKind.INPUT,
                true, null, null, null, Map.of("in", "path"));
        var op = ApiOperation.function("get", List.of(param), ApiDataType.STRING);
        var api = new ApiDefinition("path-api", "1.0.0", null,
                Map.of("ops", new ApiGroup("ops", null, Map.of("get", op), null, null, null)),
                null, null);

        var spec = (Map<String, Object>) new OpenApiPublisher().publish(api);
        assertThat(spec.get("paths")).isNotNull();
    }

    @Test
    void publish_parameterInHeader() {
        var param = new ApiParameter("auth", ApiDataType.STRING, ApiParameterKind.INPUT,
                false, null, null, null, Map.of("in", "header"));
        var api = new ApiDefinition("header-api", "1.0.0", null,
                Map.of("ops", new ApiGroup("ops", null,
                        Map.of("test", ApiOperation.function("test", List.of(param), ApiDataType.STRING)),
                        null, null, null)
                ), null, null);

        var spec = (Map<String, Object>) new OpenApiPublisher().publish(api);
        assertThat(spec.get("paths")).isNotNull();
    }

    @Test
    void publish_parameterInCookie() {
        var param = new ApiParameter("session", ApiDataType.STRING, ApiParameterKind.INPUT,
                false, null, null, null, Map.of("in", "cookie"));
        var api = new ApiDefinition("cookie-api", "1.0.0", null,
                Map.of("ops", new ApiGroup("ops", null,
                        Map.of("test", ApiOperation.function("test", List.of(param), ApiDataType.STRING)),
                        null, null, null)
                ), null, null);

        var spec = (Map<String, Object>) new OpenApiPublisher().publish(api);
        assertThat(spec.get("paths")).isNotNull();
    }

    @Test
    void publish_parameterInUnknown_location() {
        var param = new ApiParameter("x", ApiDataType.STRING, ApiParameterKind.INPUT,
                false, null, null, null, Map.of("in", "unknown"));
        var api = new ApiDefinition("unknown-loc", "1.0.0", null,
                Map.of("ops", new ApiGroup("ops", null,
                        Map.of("test", ApiOperation.function("test", List.of(param), ApiDataType.STRING)),
                        null, null, null)
                ), null, null);

        var spec = (Map<String, Object>) new OpenApiPublisher().publish(api);
        assertThat(spec.get("paths")).isNotNull();
    }

    @Test
    void publish_withGroupTypes() {
        var user = new ApiDataType("User", "object", "com.example.User", false,
                Map.of("name", ApiDataType.STRING));
        var api = new ApiDefinition("types-api", "1.0.0", null,
                Map.of("users", new ApiGroup("users", null,
                        Map.of("get", ApiOperation.function("get", List.of(), ApiDataType.STRING)),
                        Map.of("User", user), null, null)
                ), null, null);

        var spec = (Map<String, Object>) new OpenApiPublisher().publish(api);
        var components = (Map<String, Object>) spec.get("components");
        var schemas = (Map<String, Object>) components.get("schemas");
        assertThat(schemas).containsKey("User");
    }

    @Test
    void publish_withNonScalarParam_asBody() {
        var objType = new ApiDataType("Filter", "object", "com.example.Filter", false,
                Map.of("name", ApiDataType.STRING));
        var param = ApiParameter.input("filter", objType);
        var api = new ApiDefinition("body-api", "1.0.0", null,
                Map.of("ops", new ApiGroup("ops", null,
                        Map.of("search", ApiOperation.function("search", List.of(param), ApiDataType.STRING)),
                        null, null, null)
                ), null, null);

        var spec = (Map<String, Object>) new OpenApiPublisher().publish(api);
        assertThat(spec.get("paths")).isNotNull();
    }

    @Test
    void publish_withNullParamType() {
        var param = new ApiParameter("data", null, ApiParameterKind.INPUT, false, null, null, null, null);
        var api = new ApiDefinition("null-type", "1.0.0", null,
                Map.of("ops", new ApiGroup("ops", null,
                        Map.of("test", ApiOperation.function("test", List.of(param), ApiDataType.STRING)),
                        null, null, null)
                ), null, null);

        var spec = (Map<String, Object>) new OpenApiPublisher().publish(api);
        assertThat(spec.get("paths")).isNotNull();
    }

    private Map<String, Object> getResponses(Map<String, Object> spec) {
        var paths = (Map<String, Object>) spec.get("paths");
        for (var entry : paths.entrySet()) {
            var pathItem = (Map<String, Object>) entry.getValue();
            for (var methodEntry : pathItem.entrySet()) {
                var operation = (Map<String, Object>) methodEntry.getValue();
                return (Map<String, Object>) operation.get("responses");
            }
        }
        return Map.of();
    }
}
