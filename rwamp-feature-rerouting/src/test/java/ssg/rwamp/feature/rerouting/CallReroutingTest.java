package ssg.rwamp.feature.rerouting;

import ssg.legoflow.wamp.core.WampMessage;
import ssg.legoflow.wamp.core.WampMessageType;
import ssg.legoflow.wamp.core.realm.RealmManager;
import ssg.legoflow.wamp.core.router.WampRouter;
import ssg.legoflow.wamp.core.transport.WampTransport;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CallReroutingTest {

    private RealmManager realmManager;
    private WampRouter router;

    @BeforeEach
    void setUp() {
        realmManager = new RealmManager();
        realmManager.createRealm("realm1");
        realmManager.createRealm("realm2");
        router = new WampRouter();
        ReroutingApi.register(router, realmManager);
    }

    @Test
    void rerouteApi_missingRerouteOption_returnsError() {
        var call = new WampMessage.Call(1, Map.of(), "wamp.reroute.call", List.of());
        var result = route(call);

        assertThat(result).isInstanceOf(WampMessage.Result.class);
        var body = (Map<String, Object>) ((WampMessage.Result) result).args().get(0);
        assertThat(body).containsEntry("error", "missing reroute option");
    }

    @Test
    void rerouteApi_realmNotFound_returnsError() {
        var reroute = new LinkedHashMap<String, Object>();
        reroute.put("realm", "nonexistent");
        reroute.put("procedure", "com.example.add");
        var options = new LinkedHashMap<String, Object>();
        options.put("reroute", reroute);

        var call = new WampMessage.Call(1, options, "wamp.reroute.call", List.of());
        var result = route(call);

        assertThat(result).isInstanceOf(WampMessage.Result.class);
        var body = (Map<String, Object>) ((WampMessage.Result) result).args().get(0);
        assertThat((String) body.get("error")).contains("realm_not_found");
    }

    @Test
    void rerouteApi_procedureNotRegistered_returnsError() {
        var reroute = new LinkedHashMap<String, Object>();
        reroute.put("realm", "realm1");
        reroute.put("procedure", "com.example.nonexistent");
        var options = new LinkedHashMap<String, Object>();
        options.put("reroute", reroute);

        var call = new WampMessage.Call(1, options, "wamp.reroute.call", List.of());
        var result = route(call);

        assertThat(result).isInstanceOf(WampMessage.Result.class);
        var body = (Map<String, Object>) ((WampMessage.Result) result).args().get(0);
        assertThat((String) body.get("error")).contains("no_such_procedure");
    }

    @Test
    void rerouteDealer_missingRealm_returnsError() {
        var dealer = new ReroutingDealer(realmManager);
        var callerPair = InMemoryTransport.createPair();

        var reroute = new LinkedHashMap<String, Object>();
        reroute.put("realm", "nonexistent");
        reroute.put("procedure", "com.example.add");
        var options = new LinkedHashMap<String, Object>();
        options.put("reroute", reroute);

        var call = new WampMessage.Call(1, options, "com.example.forward", List.of());
        dealer.handleCall(call, callerPair[0], 100);

        var response = callerPair[1].poll();
        assertThat(response).isInstanceOf(WampMessage.Error.class);
        var error = (WampMessage.Error) response;
        assertThat(error.requestType()).isEqualTo(WampMessageType.CALL.code());
        assertThat(error.error()).contains("realm_not_found");
    }

    @Test
    void rerouteDealer_procedureNotFound_returnsError() {
        var dealer = new ReroutingDealer(realmManager);
        var callerPair = InMemoryTransport.createPair();

        var reroute = new LinkedHashMap<String, Object>();
        reroute.put("realm", "realm1");
        reroute.put("procedure", "com.example.nonexistent");
        var options = new LinkedHashMap<String, Object>();
        options.put("reroute", reroute);

        var call = new WampMessage.Call(1, options, "com.example.forward", List.of());
        dealer.handleCall(call, callerPair[0], 100);

        var response = callerPair[1].poll();
        assertThat(response).isInstanceOf(WampMessage.Error.class);
        assertThat(((WampMessage.Error) response).error()).contains("no_procedure");
    }

    @Test
    void realmManager_createsAndFindsRealms() {
        assertThat(realmManager.getRealmCount()).isEqualTo(2);
        assertThat(realmManager.getRealm("realm1")).isPresent();
        assertThat(realmManager.getRealm("realm2")).isPresent();
        assertThat(realmManager.getRealm("realm3")).isEmpty();
    }

    @Test
    void realmManager_removeRealm() {
        realmManager.removeRealm("realm1");
        assertThat(realmManager.getRealmCount()).isEqualTo(1);
        assertThat(realmManager.getRealm("realm1")).isEmpty();
    }

    private WampMessage route(WampMessage.Call call) {
        var transport = new ResultTransport();
        router.route(call, transport, 0);
        return transport.captured();
    }

    private static class ResultTransport implements WampTransport {
        private WampMessage captured;

        @Override public void send(WampMessage msg) { this.captured = msg; }
        @Override public WampMessage receive() { return null; }
        @Override public void close() {}
        @Override public boolean isOpen() { return true; }

        public WampMessage captured() { return captured; }
    }
}
