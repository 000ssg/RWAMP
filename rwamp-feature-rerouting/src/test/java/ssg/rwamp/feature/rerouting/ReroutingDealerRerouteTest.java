package ssg.rwamp.feature.rerouting;

import ssg.legoflow.wamp.core.WampMessage;
import ssg.legoflow.wamp.core.realm.RealmManager;
import ssg.legoflow.wamp.core.transport.WampTransport;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Additional ReroutingDealer tests for branch coverage.
 */
class ReroutingDealerRerouteTest {

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
    void handleCall_reroute_procedureNotFound_returnsError() {
        var pair = InMemoryTransport.createPair();

        var reroute = new LinkedHashMap<String, Object>();
        reroute.put("realm", "realm1");
        reroute.put("procedure", "com.example.nonexistent");
        var options = new LinkedHashMap<String, Object>();
        options.put("reroute", reroute);

        var call = new WampMessage.Call(1, options, "com.example.forward", List.of());
        var result = dealer.handleCall(call, pair[0], 100);

        assertThat(result).isNull();
        var response = pair[1].poll();
        assertThat(response).isInstanceOf(WampMessage.Error.class);
    }

    @Test
    void QueueTransport_send_and_poll() {
        var queue = new LinkedBlockingQueue<WampMessage>();
        var transport = new ReroutingDealer.QueueTransport(queue);

        var msg = new WampMessage.Result(1L, Map.of(), List.of("hello"));
        transport.send(msg);

        var received = queue.poll();
        assertThat(received).isSameAs(msg);
    }

    @Test
    void QueueTransport_receive_returns_message() throws InterruptedException {
        var queue = new LinkedBlockingQueue<WampMessage>();
        var transport = new ReroutingDealer.QueueTransport(queue);

        var expected = new WampMessage.Result(42L, Map.of(), List.of("data"));
        queue.offer(expected);

        var received = transport.receive();
        assertThat(received).isSameAs(expected);
    }

    @Test
    void QueueTransport_receive_interrupted() throws InterruptedException {
        var queue = new LinkedBlockingQueue<WampMessage>();
        var transport = new ReroutingDealer.QueueTransport(queue);

        var testThread = new Thread(() -> {
            try {
                transport.receive();
            } catch (RuntimeException e) {
                // Expected: interrupted
            }
        });
        testThread.start();

        Thread.sleep(100);
        testThread.interrupt();
        testThread.join(2000);

        assertThat(testThread.isAlive()).isFalse();
    }

    @Test
    void QueueTransport_isOpen_and_close() {
        var queue = new LinkedBlockingQueue<WampMessage>();
        var transport = new ReroutingDealer.QueueTransport(queue);

        assertThat(transport.isOpen()).isTrue();
        transport.close();
        assertThat(transport.isOpen()).isTrue();
    }

    @Test
    void ReroutingDealer_WampTransport_methods() {
        assertThat(dealer.isOpen()).isTrue();
        assertThat(dealer.receive()).isNull();
        dealer.send(new WampMessage.Result(1L, Map.of(), List.of()));
        dealer.close();
        assertThat(dealer.isOpen()).isTrue();
    }

    @Test
    void createForwardQueue_returnsLinkedBlockingQueue() {
        var queue = dealer.createForwardQueue();
        assertThat(queue).isInstanceOf(LinkedBlockingQueue.class);
    }
}
