package ssg.rwamp.api.publisher;

import ssg.legoflow.wamp.core.realm.Realm;
import ssg.legoflow.wamp.core.router.WampRouter;
import ssg.rwamp.api.provider.model.*;
import ssg.rwamp.api.publisher.rest.RestPublisher;
import ssg.rwamp.feature.virtual.VirtualSessionManager;
import ssg.rwamp.rest.RestRequest;
import ssg.rwamp.rest.RestResponse;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RestPublisherTest {

    @Test
    void publish_createsBridgeAndPaths() {
        var router = new WampRouter();
        var realm = new Realm("default");
        var sessionManager = new VirtualSessionManager(realm);
        
        var api = new ApiDefinition("test-api", "1.0.0", null,
                Map.of("users", new ApiGroup("users", null,
                        Map.of("get-user", ApiOperation.function("get-user",
                                List.of(ApiParameter.input("id", ApiDataType.INTEGER)),
                                ApiDataType.STRING)),
                        null, null, List.of("admin"))
                ), null, null);

        var result = (RestPublisher.RestPublishResult) new RestPublisher(router, sessionManager, "com.app")
                .basePath("/api")
                .publish(api);

        assertThat(result.bridge()).isNotNull();
        assertThat(result.pathMappings()).containsKey("/api/get-user");
        assertThat(result.pathMappings().get("/api/get-user")).isEqualTo("com.app.users.get-user");
    }

    @Test
    void publish_withoutBasePath() {
        var router = new WampRouter();
        var realm = new Realm("default");
        var sessionManager = new VirtualSessionManager(realm);
        
        var api = new ApiDefinition("test-api", "1.0.0", null,
                Map.of("items", new ApiGroup("items", null,
                        Map.of("list", ApiOperation.function("list", List.of(), ApiDataType.STRING)),
                        null, null, null)
                ), null, null);

        var result = (RestPublisher.RestPublishResult) new RestPublisher(router, sessionManager, "store")
                .publish(api);

        assertThat(result.pathMappings()).containsKey("/list");
    }

    @Test
    void publish_proceduresVisibleInDealer() {
        var router = new WampRouter();
        var realm = new Realm("default");
        var sessionManager = new VirtualSessionManager(realm);
        
        var api = new ApiDefinition("test-api", "1.0.0", null,
                Map.of("test", new ApiGroup("test", null,
                        Map.of("ping", ApiOperation.function("ping", List.of(), ApiDataType.STRING)),
                        null, null, null)
                ), null, null);

        new RestPublisher(router, sessionManager, "app").publish(api);

        assertThat(router.getDealer().getRegisteredProcedures()).contains("app.test.ping");
    }
}
