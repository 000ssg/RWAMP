package ssg.rwamp.feature.statistics;

import ssg.legoflow.wamp.core.WampMessage;
import ssg.legoflow.wamp.core.router.WampRouter;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class StatisticsApiTest {

    @Test
    void getStatisticsReturnsSnapshot() {
        var stats = new WampStatistics();
        stats.group("realm1").call();
        stats.group("realm1").call();
        stats.group("realm1").publish();

        var router = new WampRouter();
        StatisticsApi.register(router, stats);

        var pair = InMemoryTransport.createPair();
        router.route(new WampMessage.Call(1L, Map.of(), "wamp.statistics.get",
                List.of()), pair[1]);

        var response = pair[0].tryReceive();
        assertThat(response).isInstanceOf(WampMessage.Result.class);
        var result = (WampMessage.Result) response;
        var snapshot = (Map<String, Object>) result.args().getFirst();

        assertThat(snapshot).containsKey("realm1");
        var realm1Stats = (Map<String, Object>) snapshot.get("realm1");
        assertThat(realm1Stats.get("calls")).isEqualTo(2L);
        assertThat(realm1Stats.get("publishes")).isEqualTo(1L);
    }

    @Test
    void getStatisticsWithRealmFilter() {
        var stats = new WampStatistics();
        stats.group("r1").call();
        stats.group("r2").publish();

        var router = new WampRouter();
        StatisticsApi.register(router, stats);

        var pair = InMemoryTransport.createPair();
        router.route(new WampMessage.Call(1L, Map.of("realm", "r1"),
                "wamp.statistics.get", List.of()), pair[1]);

        var response = pair[0].tryReceive();
        assertThat(response).isInstanceOf(WampMessage.Result.class);
        var result = (WampMessage.Result) response;
        var counters = (Map<String, Object>) result.args().getFirst();

        // Direct counter map for filtered realm
        assertThat(counters.get("calls")).isEqualTo(1L);
        assertThat(counters.get("publishes")).isEqualTo(0L);
    }

    @Test
    void unregisterRemovesProcedure() {
        var stats = new WampStatistics();
        var router = new WampRouter();
        StatisticsApi.register(router, stats);
        StatisticsApi.unregister(router);

        var pair = InMemoryTransport.createPair();
        router.route(new WampMessage.Call(1L, Map.of(), "wamp.statistics.get",
                List.of()), pair[1]);

        var response = pair[0].tryReceive();
        assertThat(response).isInstanceOf(WampMessage.Error.class);
        assertThat(((WampMessage.Error) response).error())
                .isEqualTo("wamp.error.no_such_procedure");
    }
}
