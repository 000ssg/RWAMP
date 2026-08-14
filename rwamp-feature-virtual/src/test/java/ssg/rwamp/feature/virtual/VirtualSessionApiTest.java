package ssg.rwamp.feature.virtual;

import ssg.legoflow.wamp.core.WampMessage;
import ssg.legoflow.wamp.core.WampSession;
import ssg.legoflow.wamp.core.realm.Realm;
import ssg.legoflow.wamp.core.router.WampRouter;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class VirtualSessionApiTest {

    @Test
    void registerVirtualSession() {
        var realm = new Realm("test");
        var router = new WampRouter();
        var manager = new VirtualSessionManager(realm);
        VirtualSessionApi.register(router, realm, manager);

        var pair = InMemoryTransport.createPair();
        router.route(new WampMessage.Call(1L, Map.of(),
                "virtual_session.register",
                List.of(Map.of("authid", "user-1", "authrole", "admin", "authmethod", "ticket"))),
                pair[1]);

        var result = pair[0].tryReceive();
        assertThat(result).isInstanceOf(WampMessage.Result.class);
        var info = (Map<String, Object>) ((WampMessage.Result) result).args().get(0);
        assertThat(info).containsEntry("authid", "user-1");
        assertThat(info).containsEntry("authrole", "admin");
        assertThat(info).containsEntry("authmethod", "ticket");
        long virtualId = (long) info.get("session");

        // Virtual session exists in realm
        var session = realm.getSession(virtualId);
        assertThat(session).isNotNull();
        assertThat(session.getAuthId()).isEqualTo("user-1");

        // Virtual session appears in router's active sessions
        assertThat(router.getActiveSessionIds()).contains(virtualId);
    }

    @Test
    void virtualSessionCountedInMeta() {
        var realm = new Realm("test");
        var router = new WampRouter();
        var manager = new VirtualSessionManager(realm);
        VirtualSessionApi.register(router, realm, manager);

        var pair = InMemoryTransport.createPair();

        // Register virtual session
        router.route(new WampMessage.Call(1L, Map.of(),
                "virtual_session.register",
                List.of(Map.of("authid", "user-1"))), pair[1]);
        pair[0].tryReceive();

        // Check session count
        router.route(new WampMessage.Call(2L, Map.of(), "wamp.session.count", List.of()), pair[1]);
        var countResult = pair[0].tryReceive();
        var count = (int) ((WampMessage.Result) countResult).args().get(0);
        assertThat(count).isEqualTo(1);

        // Check session list
        router.route(new WampMessage.Call(3L, Map.of(), "wamp.session.list", List.of()), pair[1]);
        var listResult = pair[0].tryReceive();
        var sessions = (List<?>) ((WampMessage.Result) listResult).args().get(0);
        assertThat(sessions).hasSize(1);
    }

    @Test
    void unregisterVirtualSession() {
        var realm = new Realm("test");
        var router = new WampRouter();
        var manager = new VirtualSessionManager(realm);
        VirtualSessionApi.register(router, realm, manager);

        var pair = InMemoryTransport.createPair();

        // Register
        router.route(new WampMessage.Call(1L, Map.of(),
                "virtual_session.register",
                List.of(Map.of("authid", "user-1"))), pair[1]);
        var registerResult = pair[0].tryReceive();
        var virtualId = (long) ((Map<?, ?>) ((WampMessage.Result) registerResult).args().get(0)).get("session");

        // Unregister
        router.route(new WampMessage.Call(2L, Map.of(),
                "virtual_session.unregister",
                List.of(virtualId)), pair[1]);
        pair[0].tryReceive();

        // Session removed from realm
        assertThat(realm.getSession(virtualId)).isNull();
        assertThat(manager.getCount()).isZero();
    }

    @Test
    void virtualSessionCanBeKilled() {
        var realm = new Realm("test");
        var router = new WampRouter();
        var manager = new VirtualSessionManager(realm);
        VirtualSessionApi.register(router, realm, manager);

        var pair = InMemoryTransport.createPair();

        // Register
        router.route(new WampMessage.Call(1L, Map.of(),
                "virtual_session.register",
                List.of(Map.of("authid", "user-1"))), pair[1]);
        var registerResult = pair[0].tryReceive();
        var virtualId = (long) ((Map<?, ?>) ((WampMessage.Result) registerResult).args().get(0)).get("session");

        // Check session gets
        router.route(new WampMessage.Call(2L, Map.of(),
                "wamp.session.get",
                List.of(virtualId)), pair[1]);
        var getResult = pair[0].tryReceive();
        var info = (Map<String, Object>) ((WampMessage.Result) getResult).args().get(0);
        assertThat(info).containsEntry("session", virtualId);
    }

    @Test
    void multipleVirtualSessions() {
        var realm = new Realm("test");
        var router = new WampRouter();
        var manager = new VirtualSessionManager(realm);
        VirtualSessionApi.register(router, realm, manager);

        var pair = InMemoryTransport.createPair();

        // Register two virtual sessions
        router.route(new WampMessage.Call(1L, Map.of(),
                "virtual_session.register",
                List.of(Map.of("authid", "user-1", "authrole", "admin"))), pair[1]);
        var id1 = (long) ((Map<?, ?>) ((WampMessage.Result) pair[0].tryReceive()).args().get(0)).get("session");

        router.route(new WampMessage.Call(2L, Map.of(),
                "virtual_session.register",
                List.of(Map.of("authid", "user-2", "authrole", "viewer"))), pair[1]);
        var id2 = (long) ((Map<?, ?>) ((WampMessage.Result) pair[0].tryReceive()).args().get(0)).get("session");

        assertThat(id1).isNotEqualTo(id2);
        assertThat(manager.getCount()).isEqualTo(2);
        assertThat(router.getActiveSessionCount()).isEqualTo(2);
    }

    @Test
    void unregisterRemovesProcedures() {
        var realm = new Realm("test");
        var router = new WampRouter();
        var manager = new VirtualSessionManager(realm);
        VirtualSessionApi.register(router, realm, manager);
        VirtualSessionApi.unregister(router);

        var pair = InMemoryTransport.createPair();
        router.route(new WampMessage.Call(1L, Map.of(),
                "virtual_session.register",
                List.of(Map.of())), pair[1]);

        var response = pair[0].tryReceive();
        assertThat(response).isInstanceOf(WampMessage.Error.class);
        assertThat(((WampMessage.Error) response).error()).isEqualTo("wamp.error.no_such_procedure");
    }
}
