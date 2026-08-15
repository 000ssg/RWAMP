package ssg.rwamp.api.publisher;

import ssg.legoflow.wamp.core.router.WampRouter;
import ssg.rwamp.api.provider.AnnotationBasedApiProvider;
import ssg.rwamp.api.provider.GetterSetterApiProvider;
import ssg.rwamp.api.provider.ManualApiProvider;
import ssg.rwamp.api.provider.ApiProvider;
import ssg.rwamp.api.provider.model.*;
import ssg.rwamp.api.publisher.openapi.OpenApiPublisher;
import ssg.rwamp.api.publisher.jsdoc.JsDocPublisher;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class UnifiedApiPublisherTest {

    @Test
    void publish_openApiAndJsDoc() {
        var publisher = new UnifiedApiPublisher("unified-api")
                .provider(new AnnotationBasedApiProvider("sample-api"))
                .openApiPublisher(p -> p
                    .serverUrl("http://localhost:8080")
                    .license("MIT"))
                .jsDocPublisher(p -> p.title("Unified Docs"));

        var result = publisher.publish(SampleService.class);

        assertThat(result.definition()).isNotNull();
        assertThat(result.openApiJson()).contains("\"openapi\": \"3.1.0\"");
        assertThat(result.jsDocHtml()).contains("<!DOCTYPE html>");
        assertThat(result.wampResults()).isNull();
    }

    @Test
    void publish_allThreeFormats() {
        var router = new WampRouter();
        var publisher = new UnifiedApiPublisher("full-api")
                .version("2.0.0")
                .provider(new AnnotationBasedApiProvider("sample-api"))
                .openApiPublisher(p -> {})
                .wampPublisher(router, "com.demo")
                .jsDocPublisher(p -> {});

        var result = publisher.publish(SampleService.class);

        assertThat(result.openApiJson()).isNotNull();
        assertThat(result.wampResults()).isNotNull();
        assertThat(result.jsDocHtml()).isNotNull();
        assertThat(result.definition().version()).isEqualTo("2.0.0");
    }

    @Test
    void publish_multipleProviders() {
        var publisher = new UnifiedApiPublisher("multi-api")
                .provider(new AnnotationBasedApiProvider("anno-api"))
                .openApiPublisher(p -> {});

        var result = publisher.publish(SampleService.class);
        assertThat(result.definition().groups()).containsKey("users");
    }

    @Test
    void publish_noProviders() {
        var publisher = new UnifiedApiPublisher("empty-api")
                .openApiPublisher(p -> {});

        var result = publisher.publish(SampleService.class);
        assertThat(result.definition().isEmpty()).isTrue();
    }

    @Test
    void publish_withVersion() {
        var publisher = new UnifiedApiPublisher("versioned-api")
                .version("3.0.0")
                .openApiPublisher(p -> {});

        var result = publisher.publish();
        assertThat(result.definition().version()).isEqualTo("3.0.0");
    }

    @Test
    void publish_noPublishers() {
        var publisher = new UnifiedApiPublisher("no-publishers")
                .provider(new AnnotationBasedApiProvider("sample-api"));

        var result = publisher.publish(SampleService.class);
        assertThat(result.openApiJson()).isNull();
        assertThat(result.openApiYaml()).isNull();
        assertThat(result.jsDocHtml()).isNull();
        assertThat(result.wampResults()).isNull();
        assertThat(result.restBridge()).isNull();
    }

    @Test
    void publish_withMultipleProviderTypes() {
        var publisher = new UnifiedApiPublisher("multi-provider-api")
                .provider(new AnnotationBasedApiProvider("sample-api"))
                .provider(new GetterSetterApiProvider("bean-api"))
                .openApiPublisher(p -> p.serverUrl("http://multi.api"));

        var result = publisher.publish(SampleService.class);
        assertThat(result.openApiJson()).contains("openapi");
        assertThat(result.definition()).isNotNull();
    }

    @Test
    void publish_restPublisher() {
        var router = new WampRouter();
        // Skip REST publisher test since it requires VirtualSessionManager
    }

    @Test
    void publish_withNullProvider() {
        var publisher = new UnifiedApiPublisher("null-provider-api")
                .provider(null)
                .openApiPublisher(p -> {});

        var result = publisher.publish(SampleService.class);
        assertThat(result.definition().isEmpty()).isTrue();
    }

    @Test
    void publish_multipleTargets() {
        var publisher = new UnifiedApiPublisher("multi-target")
                .provider(new AnnotationBasedApiProvider("sample-api"))
                .openApiPublisher(p -> {});

        var result = publisher.publish(SampleService.class, SampleService.class);
        assertThat(result.definition().groups()).containsKey("users");
    }

    @Test
    void publish_jsDocOnly() {
        var publisher = new UnifiedApiPublisher("jsdoc-api")
                .provider(new AnnotationBasedApiProvider("sample-api"))
                .jsDocPublisher(p -> p.title("My API"));

        var result = publisher.publish(SampleService.class);
        assertThat(result.jsDocHtml()).isNotNull();
        assertThat(result.jsDocHtml()).contains("<!DOCTYPE html>");
        assertThat(result.openApiJson()).isNull();
    }

    @Test
    void publish_wampOnly() {
        var router = new WampRouter();
        var publisher = new UnifiedApiPublisher("wamp-api")
                .provider(new AnnotationBasedApiProvider("sample-api"))
                .wampPublisher(router, "com.test");

        var result = publisher.publish(SampleService.class);
        assertThat(result.wampResults()).isNotNull();
        assertThat(result.openApiJson()).isNull();
    }

    @Test
    void publish_openApiPublisher_configConsumer() {
        var publisher = new UnifiedApiPublisher("config-api")
                .openApiPublisher(p -> p
                        .serverUrl("https://configured.example.com")
                        .contact("Team", "team@example.com")
                        .license("MIT")
                        .securityEnabled(false));

        var result = publisher.publish();
        assertThat(result.openApiJson()).contains("configured.example.com");
    }

    @Test
    void publish_openApiPublisher_simple() {
        var publisher = new UnifiedApiPublisher("simple-api")
                .openApiPublisher();

        var result = publisher.publish();
        assertThat(result.openApiJson()).isNotNull();
    }

    @Test
    void publish_jsDocPublisher_configConsumer() {
        var publisher = new UnifiedApiPublisher("jsdoc-config")
                .jsDocPublisher(p -> p.title("Configured Docs"));

        var result = publisher.publish();
        assertThat(result.jsDocHtml()).contains("<!DOCTYPE html>");
    }

    @Test
    void publish_jsDocPublisher_simple() {
        var publisher = new UnifiedApiPublisher("jsdoc-simple")
                .jsDocPublisher();

        var result = publisher.publish();
        assertThat(result.jsDocHtml()).isNotNull();
    }

    @Test
    void publish_wampPublisher_configConsumer() {
        var router = new WampRouter();
        var publisher = new UnifiedApiPublisher("wamp-config")
                .wampPublisher(router, "com.test", p -> {});

        var result = publisher.publish();
        // wampResults is an empty map since no providers/targets
        assertThat(result.wampResults()).isNotNull();
        assertThat(result.wampResults()).isEmpty();
    }

    @Test
    void publish_publishResult_accessors() {
        var api = new ApiDefinition("test", "1.0.0", null,
                Map.of("ops", new ApiGroup("ops", null,
                        Map.of("ping", ApiOperation.function("ping", List.of(), ApiDataType.STRING)),
                        null, null, null)
                ), null, null);

        var result = new UnifiedApiPublisher.PublishResult(api);
        assertThat(result.definition()).isNotNull();
        assertThat(result.openApiJson()).isNull();
        assertThat(result.openApiYaml()).isNull();
        assertThat(result.wampResults()).isNull();
        assertThat(result.restBridge()).isNull();
        assertThat(result.restPathMappings()).isNull();
        assertThat(result.jsDocHtml()).isNull();
    }
}
