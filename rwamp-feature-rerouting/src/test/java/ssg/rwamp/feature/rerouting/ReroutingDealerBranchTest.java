package ssg.rwamp.feature.rerouting;

import ssg.legoflow.wamp.core.WampMessage;
import ssg.legoflow.wamp.core.WampMessageType;
import ssg.legoflow.wamp.core.realm.RealmManager;
import ssg.legoflow.wamp.core.transport.WampTransport;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for uncovered branch paths in ReroutingDealer.rerouteCall().
 *
 * Uses a queue filter to discard Invocation messages (which the Dealer
 * places into the same queue used for results) so that poll() retrieves
 * the Result/Error sent by the simulated callee.
 */
class ReroutingDealerBranchTest {

    private RealmManager realmManager;
    private ReroutingDealer dealer;

    @BeforeEach
    void setUp() {
        realmManager = new RealmManager();
        realmManager.createRealm("realm1");
        realmManager.createRealm("realm2");
        dealer = new ReroutingDealer(realmManager);
    }

    /**
     * Wrapping queue that discards Invocation messages from offer().
     */
    static class NoInvocationQueue implements BlockingQueue<WampMessage> {
        private final BlockingQueue<WampMessage> delegate;

        NoInvocationQueue(BlockingQueue<WampMessage> delegate) {
            this.delegate = delegate;
        }

        @Override
        public boolean offer(WampMessage msg) {
            if (msg instanceof WampMessage.Invocation) return true;
            return delegate.offer(msg);
        }

        @Override
        public boolean offer(WampMessage msg, long timeout, TimeUnit unit) throws InterruptedException {
            if (msg instanceof WampMessage.Invocation) return true;
            return delegate.offer(msg, timeout, unit);
        }

        @Override
        public void put(WampMessage msg) throws InterruptedException {
            if (!(msg instanceof WampMessage.Invocation)) {
                delegate.put(msg);
            }
        }

        @Override
        public WampMessage take() throws InterruptedException {
            return delegate.take();
        }

        @Override
        public WampMessage poll(long timeout, TimeUnit unit) throws InterruptedException {
            return delegate.poll(timeout, unit);
        }

        @Override
        public WampMessage poll() {
            return delegate.poll();
        }

        @Override
        public int drainTo(Collection<? super WampMessage> c) {
            return delegate.drainTo(c);
        }

        @Override
        public int size() {
            return delegate.size();
        }

        // Minimal stubs for remaining Queue methods
        @Override public boolean add(WampMessage e) { return offer(e); }
        @Override public WampMessage remove() { return delegate.remove(); }
        @Override public boolean remove(Object o) { return delegate.remove(o); }
        @Override public WampMessage element() { return delegate.element(); }
        @Override public WampMessage peek() { return delegate.peek(); }
        @Override public boolean isEmpty() { return delegate.isEmpty(); }
        @Override public void clear() { delegate.clear(); }
        @Override public int remainingCapacity() { return delegate.remainingCapacity(); }
        @Override public Iterator<WampMessage> iterator() { return delegate.iterator(); }
        @Override public Object[] toArray() { return delegate.toArray(); }
        @Override public <T> T[] toArray(T[] a) { return delegate.toArray(a); }
        @Override public boolean contains(Object o) { return delegate.contains(o); }
        @Override public boolean containsAll(Collection<?> c) { return delegate.containsAll(c); }
        @Override public boolean addAll(Collection<? extends WampMessage> c) { return delegate.addAll(c); }
        @Override public boolean removeAll(Collection<?> c) { return delegate.removeAll(c); }
        @Override public boolean retainAll(Collection<?> c) { return delegate.retainAll(c); }
        @Override public int drainTo(Collection<? super WampMessage> c, int maxN) { return delegate.drainTo(c, maxN); }
    }

    @Test
    void rerouteCall_resultResponse_forwardedToCaller() throws InterruptedException {
        var controlledQueue = new NoInvocationQueue(new LinkedBlockingQueue<WampMessage>());
        var testDealer = new ReroutingDealer(realmManager) {
            @Override
            protected BlockingQueue<WampMessage> createForwardQueue() {
                return controlledQueue;
            }
        };

        var pair = InMemoryTransport.createPair();
        var targetRealm = realmManager.getRealm("realm2").orElseThrow();

        var resultToSend = new WampMessage.Result(1L, Map.of(), List.of(42));

        var calleeTransport = new WampTransport() {
            @Override
            public void send(WampMessage msg) {
                controlledQueue.offer(resultToSend);
            }
            @Override public WampMessage receive() { return null; }
            @Override public void close() {}
            @Override public boolean isOpen() { return true; }
        };

        targetRealm.getDealer().handleRegister(
                new WampMessage.Register(1L, Map.of(), "com.example.add"),
                calleeTransport);

        var reroute = new LinkedHashMap<String, Object>();
        reroute.put("realm", "realm2");
        reroute.put("procedure", "com.example.add");
        var options = new LinkedHashMap<String, Object>();
        options.put("reroute", reroute);

        var call = new WampMessage.Call(1, options, "com.example.forward", List.of(3, 5));

        var invocation = testDealer.handleCall(call, pair[0], 100);
        assertThat(invocation).isNotNull();

        var response = pair[1].poll();
        assertThat(response).isInstanceOf(WampMessage.Result.class);
        assertThat(((WampMessage.Result) response).requestId()).isEqualTo(1L);
    }

    @Test
    void rerouteCall_errorResponse_forwardedToCaller() throws InterruptedException {
        var controlledQueue = new NoInvocationQueue(new LinkedBlockingQueue<WampMessage>());
        var testDealer = new ReroutingDealer(realmManager) {
            @Override
            protected BlockingQueue<WampMessage> createForwardQueue() {
                return controlledQueue;
            }
        };

        var pair = InMemoryTransport.createPair();
        var targetRealm = realmManager.getRealm("realm2").orElseThrow();

        var errorToSend = new WampMessage.Error(
                WampMessageType.YIELD.code(), 1L, Map.of(), "com.example.error");

        var calleeTransport = new WampTransport() {
            @Override
            public void send(WampMessage msg) {
                controlledQueue.offer(errorToSend);
            }
            @Override public WampMessage receive() { return null; }
            @Override public void close() {}
            @Override public boolean isOpen() { return true; }
        };

        targetRealm.getDealer().handleRegister(
                new WampMessage.Register(1L, Map.of(), "com.example.failing"),
                calleeTransport);

        var reroute = new LinkedHashMap<String, Object>();
        reroute.put("realm", "realm2");
        reroute.put("procedure", "com.example.failing");
        var options = new LinkedHashMap<String, Object>();
        options.put("reroute", reroute);

        var call = new WampMessage.Call(1, options, "com.example.forward", List.of());

        var invocation = testDealer.handleCall(call, pair[0], 100);
        assertThat(invocation).isNotNull();

        var response = pair[1].poll();
        assertThat(response).isInstanceOf(WampMessage.Error.class);
        assertThat(((WampMessage.Error) response).error()).isEqualTo("com.example.error");
    }

    @Test
    void rerouteCall_unexpectedResponse_sendsError() throws InterruptedException {
        var controlledQueue = new NoInvocationQueue(new LinkedBlockingQueue<WampMessage>());
        var testDealer = new ReroutingDealer(realmManager) {
            @Override
            protected BlockingQueue<WampMessage> createForwardQueue() {
                return controlledQueue;
            }
        };

        var pair = InMemoryTransport.createPair();
        var targetRealm = realmManager.getRealm("realm2").orElseThrow();

        var calleeTransport = new WampTransport() {
            @Override
            public void send(WampMessage msg) {
                controlledQueue.offer(new WampMessage.Register(1L, Map.of(), "unexpected"));
            }
            @Override public WampMessage receive() { return null; }
            @Override public void close() {}
            @Override public boolean isOpen() { return true; }
        };

        targetRealm.getDealer().handleRegister(
                new WampMessage.Register(1L, Map.of(), "com.example.weird"),
                calleeTransport);

        var reroute = new LinkedHashMap<String, Object>();
        reroute.put("realm", "realm2");
        reroute.put("procedure", "com.example.weird");
        var options = new LinkedHashMap<String, Object>();
        options.put("reroute", reroute);

        var call = new WampMessage.Call(1, options, "com.example.forward", List.of());

        var invocation = testDealer.handleCall(call, pair[0], 100);
        assertThat(invocation).isNotNull();

        var response = pair[1].poll();
        assertThat(response).isInstanceOf(WampMessage.Error.class);
        assertThat(((WampMessage.Error) response).error()).contains("unexpected_response");
    }

    @Test
    void rerouteCall_interrupted_sendsTimeoutError() throws InterruptedException {
        var controlledQueue = new LinkedBlockingQueue<WampMessage>();
        var testDealer = new ReroutingDealer(realmManager) {
            @Override
            protected BlockingQueue<WampMessage> createForwardQueue() {
                return controlledQueue;
            }
        };

        var pair = InMemoryTransport.createPair();
        var targetRealm = realmManager.getRealm("realm2").orElseThrow();

        var calleeTransport = new WampTransport() {
            @Override public void send(WampMessage msg) { /* silent */ }
            @Override public WampMessage receive() { return null; }
            @Override public void close() {}
            @Override public boolean isOpen() { return true; }
        };

        targetRealm.getDealer().handleRegister(
                new WampMessage.Register(1L, Map.of(), "com.example.slow"),
                calleeTransport);

        var reroute = new LinkedHashMap<String, Object>();
        reroute.put("realm", "realm2");
        reroute.put("procedure", "com.example.slow");
        var options = new LinkedHashMap<String, Object>();
        options.put("reroute", reroute);

        var call = new WampMessage.Call(1, options, "com.example.forward", List.of());

        var testThread = new Thread(() -> testDealer.handleCall(call, pair[0], 100));
        testThread.start();

        Thread.sleep(200);
        testThread.interrupt();
        testThread.join(5000);

        assertThat(testThread.isAlive()).isFalse();

        var response = pair[1].poll();
        assertThat(response).isInstanceOf(WampMessage.Error.class);
        assertThat(((WampMessage.Error) response).error()).contains("timeout");
    }

    @Test
    void handleCall_rerouteOptionNotMap_returnsNull() {
        var pair = InMemoryTransport.createPair();
        var options = new LinkedHashMap<String, Object>();
        options.put("reroute", "not-a-map");

        var call = new WampMessage.Call(1, options, "com.example.normal", List.of());
        var result = dealer.handleCall(call, pair[0], 100);
        assertThat(result).isNull();
    }

    @Test
    void handleCall_noRerouteOption_returnsNull() {
        var pair = InMemoryTransport.createPair();
        var call = new WampMessage.Call(1, Map.of(), "com.example.normal", List.of());
        var result = dealer.handleCall(call, pair[0], 100);
        assertThat(result).isNull();
    }

    @Test
    void handleCall_nullOptions_returnsNull() {
        var pair = InMemoryTransport.createPair();
        var call = new WampMessage.Call(1, null, "com.example.normal", List.of());
        var result = dealer.handleCall(call, pair[0], 100);
        assertThat(result).isNull();
    }

    @Test
    void handleCall_rerouteNullInOptions_returnsNull() {
        var pair = InMemoryTransport.createPair();
        var options = new LinkedHashMap<String, Object>();
        options.put("reroute", null);

        var call = new WampMessage.Call(1, options, "com.example.normal", List.of());
        var result = dealer.handleCall(call, pair[0], 100);
        assertThat(result).isNull();
    }
}
