package ssg.rwamp.feature.reflection;

import ssg.legoflow.wamp.core.WampMessage;
import ssg.legoflow.wamp.core.router.Broker;
import ssg.legoflow.wamp.core.router.Dealer;
import ssg.legoflow.wamp.core.transport.WampTransport;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import static org.assertj.core.api.Assertions.assertThat;

class ReflectionRegistryTest {

    private static class QueueTransport implements WampTransport {
        private final BlockingQueue<WampMessage> queue = new LinkedBlockingQueue<>();

        @Override public void send(WampMessage msg) { queue.offer(msg); }
        @Override public WampMessage receive() { throw new UnsupportedOperationException(); }
        @Override public void close() {}
        @Override public boolean isOpen() { return true; }

        WampMessage poll() { return queue.poll(); }
    }

    @Test
    void constructor_and_emptyProcedures() {
        var dealer = new Dealer();
        var broker = new Broker();
        var registry = new ReflectionRegistry(dealer, broker);

        assertThat(registry.getProcedures()).isEmpty();
        assertThat(registry.getTopics()).isEmpty();
        assertThat(registry.getPrefixTopics()).isEmpty();
        assertThat(registry.getWildcardTopics()).isEmpty();
        assertThat(registry.getDefinitions()).isEmpty();
    }

    @Test
    void getProcedureDetails_afterRegistration() {
        var dealer = new Dealer();
        var broker = new Broker();
        var registry = new ReflectionRegistry(dealer, broker);

        var transport = new QueueTransport();
        dealer.handleRegister(new WampMessage.Register(1L, Map.of(), "com.example.proc1"),
                transport);
        transport.poll(); // consume Registered

        var details = registry.getProcedureDetails();
        assertThat(details).containsKey("com.example.proc1");
        assertThat(details.get("com.example.proc1")).containsEntry("callees", 1);
    }

    @Test
    void getTopicDetails_afterSubscription() {
        var dealer = new Dealer();
        var broker = new Broker();
        var registry = new ReflectionRegistry(dealer, broker);

        var transport = new QueueTransport();
        broker.handleSubscribe(new WampMessage.Subscribe(1L, Map.of(), "com.example.topic"),
                transport, 100L);
        transport.poll(); // consume Subscribed

        var details = registry.getTopicDetails();
        assertThat(details).containsKey("com.example.topic");
        assertThat(details.get("com.example.topic")).containsEntry("subscribers", 1);
    }

    @Test
    void define_and_undefine() {
        var dealer = new Dealer();
        var broker = new Broker();
        var registry = new ReflectionRegistry(dealer, broker);

        registry.define("com.example.MyError", Map.of("kind", "error", "description", "Test error"));
        var defs = registry.getDefinitions();
        assertThat(defs).containsKey("com.example.MyError");
        assertThat(defs.get("com.example.MyError")).containsEntry("kind", "error");

        registry.undefine("com.example.MyError");
        assertThat(registry.getDefinitions()).doesNotContainKey("com.example.MyError");
    }

    @Test
    void getDefinitions_returnsUnmodifiableCopy() {
        var dealer = new Dealer();
        var broker = new Broker();
        var registry = new ReflectionRegistry(dealer, broker);

        registry.define("com.example.Type1", Map.of("kind", "type"));
        var defs = registry.getDefinitions();
        assertThat(defs).hasSize(1);

        // Second call returns the same content
        var defs2 = registry.getDefinitions();
        assertThat(defs2).containsExactlyEntriesOf(defs);
    }
}
