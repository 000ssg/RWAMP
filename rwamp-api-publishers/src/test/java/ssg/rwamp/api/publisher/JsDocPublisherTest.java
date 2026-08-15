package ssg.rwamp.api.publisher;

import ssg.rwamp.api.provider.AnnotationBasedApiProvider;
import ssg.rwamp.api.provider.model.*;
import ssg.rwamp.api.publisher.jsdoc.JsDocPublisher;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JsDocPublisherTest {

    @Test
    void publish_basic() {
        var api = new ApiDefinition("docs-api", "1.0.0", "Documentation test",
                Map.of("greet", new ApiGroup("greet", null,
                        Map.of("hello", ApiOperation.function("hello",
                                List.of(ApiParameter.input("name", ApiDataType.STRING)),
                                ApiDataType.STRING
                        )), null, null, null)
                ), null, null);

        var html = (String) new JsDocPublisher().title("API Docs").publish(api);

        assertThat(html).contains("<!DOCTYPE html>");
        assertThat(html).contains("API Docs");
        assertThat(html).contains("greet");
        assertThat(html).contains("hello");
        assertThat(html).contains("<button");
        assertThat(html).contains("Try it");
        assertThat(html).contains("<script>");
    }

    @Test
    void publish_fromProvider() {
        var provider = new AnnotationBasedApiProvider("sample-api");
        var def = provider.build(SampleService.class);

        var html = (String) new JsDocPublisher()
                .title("Sample API")
                .publish(def);

        // SampleService has getUser (→ "user") and listUsers (→ "list-users")
        assertThat(html).contains("users");
        assertThat(html).contains("user");
        assertThat(html).contains("list-users");
    }

    @Test
    void htmlEscaping() {
        var api = new ApiDefinition("api", "1.0.0", "<script>alert('xss')</script>",
                Map.of("x", new ApiGroup("x", null,
                        Map.of("op", ApiOperation.function("op",
                                List.of(ApiParameter.input("q", ApiDataType.STRING)),
                                ApiDataType.STRING
                        )), null, null, null)
                ), null, null);

        var html = (String) new JsDocPublisher().publish(api);
        assertThat(html).doesNotContain("<script>alert");
        assertThat(html).contains("&lt;script&gt;");
    }
}
