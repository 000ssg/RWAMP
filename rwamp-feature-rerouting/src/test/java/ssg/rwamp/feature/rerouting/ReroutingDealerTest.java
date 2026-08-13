package ssg.rwamp.feature.rerouting;

import ssg.legoflow.wamp.core.WampMessage;
import ssg.legoflow.wamp.core.WampMessageType;
import ssg.legoflow.wamp.core.realm.RealmManager;
import ssg.legoflow.wamp.core.transport.WampTransport;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ReroutingDealerTest {

    private RealmManager realmManager;
    private ReroutingDealer dealer;

    @BeforeEach
    void setUp() {
        realmManager = new RealmManager();
        realmManager.createRealm("realm1");
        realmManager.createRealm("realm2");
        dealer = new ReroutingDealer(realmManager);
    }

    @Test
    void handleCall_noRerouteOption_returnsNull() {
        var pair = InMemoryTransport.createPair();
        var call = new WampMessage.Call(1, Map.of(), "com.example.normal", List.of());
        assertThat(dealer.handleCall(call, pair[0], 100)).isNull();
    }

    @Test
    void handleCall_nullOptions_returnsNull() {
        var pair = InMemoryTransport.createPair();
        var call = new WampMessage.Call(1, null, "com.example.normal", List.of());
        assertThat(dealer.handleCall(call, pair[0], 100)).isNull();
    }

    @Test
    void handleCall_rerouteOptionNotMap_returnsNull() {
        var pair = InMemoryTransport.createPair();
        var options = new LinkedHashMap<String, Object>();
        options.put("reroute", "not_a_map");
        var call = new WampMessage.Call(1, options, "com.example.forward", List.of());
        assertThat(dealer.handleCall(call, pair[0], 100)).isNull();
    }

    @Test
    void wampTransport_methods() {
        assertThat(dealer.isOpen()).isTrue();
        assertThat(dealer.receive()).isNull();
        dealer.send(new WampMessage.Call(1, Map.of(), "test", List.of()));
        dealer.close();
        assertThat(dealer.isOpen()).isTrue();
    }

    @Test
    void handleCall_missingRealm_returnsError() {
        var pair = InMemoryTransport.createPair();
        var reroute = new LinkedHashMap<String, Object>();
        reroute.put("realm", "nonexistent");
        reroute.put("procedure", "com.example.add");
        var options = new LinkedHashMap<String, Object>();
        options.put("reroute", reroute);
        var call = new WampMessage.Call(1, options, "com.example.forward", List.of());
        dealer.handleCall(call, pair[0], 100);
        var response = pair[1].poll();
        assertThat(response).isInstanceOf(WampMessage.Error.class);
        assertThat(((WampMessage.Error) response).error()).contains("realm_not_found");
    }

    @Test
    void handleCall_procedureNotRegistered_returnsError() {
        var pair = InMemoryTransport.createPair();
        var reroute = new LinkedHashMap<String, Object>();
        reroute.put("realm", "realm1");
        reroute.put("procedure", "com.example.nonexistent");
        var options = new LinkedHashMap<String, Object>();
        options.put("reroute", reroute);
        var call = new WampMessage.Call(1, options, "com.example.forward", List.of());
        dealer.handleCall(call, pair[0], 100);
        var response = pair[1].poll();
        assertThat(response).isInstanceOf(WampMessage.Error.class);
        assertThat(((WampMessage.Error) response).error()).contains("no_procedure");
    }
}
