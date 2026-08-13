package ssg.rwamp.rest;

import ssg.legoflow.wamp.core.WampMessage;
import ssg.legoflow.wamp.core.WampSession;
import ssg.legoflow.wamp.core.realm.Realm;
import ssg.legoflow.wamp.core.router.WampRouter;
import ssg.rwamp.feature.reflection.ReflectionApi;
import ssg.rwamp.feature.session.SessionMetaApi;
import ssg.rwamp.feature.session.SessionTransportTracker;
import ssg.rwamp.feature.virtual.VirtualSessionApi;
import ssg.rwamp.feature.virtual.VirtualSessionManager;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RestWampBridgeTest {

    private WampRouter router;
    private Realm realm;
    private VirtualSessionManager virtualManager;
    private RestWampBridge bridge;

    @BeforeEach
    void setUp() {
        router = new WampRouter();
        realm = new Realm("realm1");

        // Register session meta API
        var tracker = new SessionTransportTracker();
        SessionMetaApi.register(router, tracker);

        // Register virtual session API
        virtualManager = new VirtualSessionManager(realm);
        VirtualSessionApi.register(router, realm, virtualManager);

        // Register reflection API
        var registry = ReflectionApi.createRegistry(router);
        ReflectionApi.register(router, registry);

        // Register sample meta procedures
        router.registerMetaProcedure("com.example.greet",
                (call, transport) -> {
                    String name = call.args() != null && !call.args().isEmpty()
                            ? call.args().get(0).toString() : "World";
                    return List.of("Hello, " + name + "!");
                });

        router.registerMetaProcedure("com.example.concat",
                (call, transport) -> {
                    if (call.args() == null || call.args().size() < 2) {
                        return List.of("empty");
                    }
                    String a = call.args().get(0).toString();
                    String b = call.args().get(1).toString();
                    return List.of(a + b);
                });

        bridge = new RestWampBridge(router, virtualManager, ReflectionApi.createRegistry(router));
    }

    @Test
    void callMetaProcedure_returnsResult() {
        var request = new RestRequest("GET", "/com/example/greet",
                List.of("Alice"), Map.of("authid", "user1"), null);

        var response = bridge.handle(request);

        assertThat(response.statusCode()).isEqualTo(200);
        var result = (List<Object>) response.body();
        assertThat(result).containsExactly("Hello, Alice!");
    }

    @Test
    void callMetaProcedure_withMultipleArgs() {
        var request = new RestRequest("POST", "/com/example/concat",
                List.of("hello", "world"), Map.of("authid", "user2"), null);

        var response = bridge.handle(request);

        assertThat(response.statusCode()).isEqualTo(200);
        var result = (List<Object>) response.body();
        assertThat(result).containsExactly("helloworld");
    }

    @Test
    void callUnknownPath_returns404() {
        var request = new RestRequest("GET", "",
                null, Map.of("authid", "user1"), null);

        var response = bridge.handle(request);

        assertThat(response.statusCode()).isEqualTo(404);
    }

    @Test
    void virtualSessionReuse_sameAuthId() {
        bridge.handle(new RestRequest("GET", "/com/example/greet",
                List.of("Alice"), Map.of("authid", "user1"), null));
        bridge.handle(new RestRequest("GET", "/com/example/greet",
                List.of("Bob"), Map.of("authid", "user1"), null));

        var sessions = bridge.getTrackedSessions();
        assertThat(sessions).hasSize(1);
        assertThat(sessions.containsKey("user1")).isTrue();
    }

    @Test
    void differentAuthIds_createDifferentSessions() {
        bridge.handle(new RestRequest("GET", "/com/example/greet",
                List.of("A"), Map.of("authid", "user1"), null));
        bridge.handle(new RestRequest("GET", "/com/example/greet",
                List.of("B"), Map.of("authid", "user2"), null));

        var sessions = bridge.getTrackedSessions();
        assertThat(sessions).hasSize(2);
        assertThat(sessions.get("user1")).isNotEqualTo(sessions.get("user2"));
    }

    @Test
    void removeSession_cleansUpVirtualSession() {
        bridge.handle(new RestRequest("GET", "/com/example/greet",
                List.of("A"), Map.of("authid", "user1"), null));

        bridge.removeSession("user1");

        assertThat(bridge.getTrackedSessions()).doesNotContainKey("user1");
        assertThat(virtualManager.getCount()).isZero();
    }

    @Test
    void anonymousUser_usesDefaultSession() {
        var request = new RestRequest("GET", "/com/example/greet",
                List.of("Anon"), null, null);

        var response = bridge.handle(request);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(bridge.getTrackedSessions()).hasSize(1);
        assertThat(bridge.getTrackedSessions().containsKey("anonymous")).isTrue();
    }

    @Test
    void publishToTopic_returnsPublicationId() {
        var request = new RestRequest("POST", "/com/example/news",
                List.of("hello"), Map.of("authid", "publisher"), null);

        var response = bridge.publish(request);

        assertThat(response.statusCode()).isEqualTo(200);
        var body = (Map<String, Object>) response.body();
        assertThat(body).containsKey("publication_id");
        assertThat(body).containsEntry("topic", "com.example.news");
    }

    @Test
    void reflectionApi_accessibleViaBridge() {
        var request = new RestRequest("GET", "/wamp/reflection/procedure/list",
                null, Map.of("authid", "inspector"), null);

        var response = bridge.handle(request);

        assertThat(response.statusCode()).isEqualTo(200);
        var body = (List<Object>) response.body();
        assertThat(body).isNotEmpty();
    }

    @Test
    void sessionMetaList_accessibleViaBridge() {
        var request = new RestRequest("GET", "/wamp/session/list",
                null, Map.of("authid", "admin"), null);

        var response = bridge.handle(request);

        assertThat(response.statusCode()).isEqualTo(200);
    }
}
