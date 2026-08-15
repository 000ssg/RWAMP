package ssg.rwamp.api.publisher;

import ssg.legoflow.wamp.core.router.WampRouter;
import ssg.rwamp.api.provider.AnnotationBasedApiProvider;
import ssg.rwamp.api.publisher.openapi.OpenApiPublisher;
import ssg.rwamp.api.publisher.jsdoc.JsDocPublisher;
import org.junit.jupiter.api.Test;

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
        assertThat(result.wampResults()).isNull(); // no WAMP configured
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
}
