package ssg.rwamp.feature.statistics;

import ssg.legoflow.wamp.core.WampMessage;
import ssg.legoflow.wamp.core.transport.WampTransport;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class StatisticsTransportTest {

    @Test
    void countsMessagesSent() {
        var stats = new WampStatistics();
        var group = stats.group("test");
        var pair = InMemoryTransport.createPair();
        var transport = new StatisticsTransport(pair[0], group);

        transport.send(new WampMessage.Call(1L, Map.of(), "com.test.proc", List.of("arg")));
        transport.send(new WampMessage.Publish(2L, Map.of(), "com.test.topic", List.of("data")));
        transport.send(new WampMessage.Error(64, 1L, Map.of(), "wamp.error.test"));

        var snapshot = group.snapshot();
        assertThat(snapshot.get("messages_out")).isEqualTo(3L);
        assertThat(snapshot.get("calls")).isEqualTo(1L);
        assertThat(snapshot.get("publishes")).isEqualTo(1L);
        assertThat(snapshot.get("errors")).isEqualTo(1L);
    }

    @Test
    void countsMessagesReceived() {
        var stats = new WampStatistics();
        var group = stats.group("test");

        var pair = InMemoryTransport.createPair();
        var transport = new StatisticsTransport(pair[0], group);

        pair[1].send(new WampMessage.Result(1L, Map.of(), List.of("ok")));
        pair[1].send(new WampMessage.Event(10L, 1L, Map.of(), List.of("data")));

        transport.receive(); // Result
        transport.receive(); // Event

        var snapshot = group.snapshot();
        assertThat(snapshot.get("messages_in")).isEqualTo(2L);
        assertThat(snapshot.get("results")).isEqualTo(1L);
        assertThat(snapshot.get("events")).isEqualTo(1L);
    }

    @Test
    void delegatesToInnerTransport() {
        var stats = new WampStatistics();
        var group = stats.group("test");

        var pair = InMemoryTransport.createPair();
        var transport = new StatisticsTransport(pair[0], group);

        transport.send(new WampMessage.Hello("realm1", Map.of()));

        var received = pair[1].tryReceive();
        assertThat(received).isInstanceOf(WampMessage.Hello.class);
    }

    @Test
    void getDelegateReturnsInner() {
        var stats = new WampStatistics();
        var group = stats.group("test");
        var pair = InMemoryTransport.createPair();
        var transport = new StatisticsTransport(pair[0], group);

        assertThat(transport.getDelegate()).isSameAs(pair[0]);
    }

    @Test
    void isOpenAndClose() {
        var stats = new WampStatistics();
        var group = stats.group("test");
        var pair = InMemoryTransport.createPair();
        var transport = new StatisticsTransport(pair[0], group);

        assertThat(transport.isOpen()).isTrue();
        transport.close();
        assertThat(transport.isOpen()).isFalse();
    }
}
