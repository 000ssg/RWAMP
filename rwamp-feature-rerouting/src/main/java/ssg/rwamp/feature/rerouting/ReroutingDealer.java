package ssg.rwamp.feature.rerouting;

import ssg.legoflow.wamp.core.WampMessage;
import ssg.legoflow.wamp.core.WampMessageType;
import ssg.legoflow.wamp.core.realm.Realm;
import ssg.legoflow.wamp.core.realm.RealmManager;
import ssg.legoflow.wamp.core.router.Dealer;
import ssg.legoflow.wamp.core.transport.WampTransport;

import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Wraps a WAMP Dealer to add cross-realm call rerouting.
 * <p>
 * When a CALL includes the {@code reroute} option (a map with {@code realm}
 * and {@code procedure} keys), the dealer forwards the call to the target
 * realm's Dealer and routes the result back to the original caller.
 *
 * @since 0.1.0
 */
public class ReroutingDealer implements WampTransport {

    private final RealmManager realmManager;

    /**
     * Creates a new rerouting dealer backed by the given realm manager.
     *
     * @param realmManager the realm manager to look up target realms
     */
    public ReroutingDealer(RealmManager realmManager) {
        this.realmManager = realmManager;
    }

    /**
     * Handles a call, detecting the reroute option and forwarding if needed.
     *
     * @param call            the call message
     * @param callerTransport the caller's transport
     * @param sessionId       the caller's session ID
     * @return the Invocation sent to the callee, or null if not a reroute
     */
    public WampMessage.Invocation handleCall(WampMessage.Call call,
                                              WampTransport callerTransport,
                                              long sessionId) {
        var reroute = extractRerouteOption(call);
        if (reroute != null) {
            return rerouteCall(call, reroute, callerTransport);
        }
        return null; // not a reroute
    }

    // ── WampTransport (for forwarding results back) ──

    @Override
    public void send(WampMessage msg) {
        // no-op — used as a placeholder in Dealer integration
    }

    @Override
    public WampMessage receive() {
        return null;
    }

    @Override
    public void close() {
        // no-op
    }

    @Override
    public boolean isOpen() {
        return true;
    }

    // ── Internal forwarding ──

    private Map<String, Object> extractRerouteOption(WampMessage.Call call) {
        if (call.options() == null) return null;
        Object reroute = call.options().get("reroute");
        if (reroute instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return null;
    }

    /**
     * Creates the queue used for forwarding results.
     * Protected for testability — tests can override to inject a controlled queue.
     */
    protected BlockingQueue<WampMessage> createForwardQueue() {
        return new LinkedBlockingQueue<>();
    }

    private WampMessage.Invocation rerouteCall(WampMessage.Call call,
                                                Map<String, Object> reroute,
                                                WampTransport callerTransport) {
        String targetRealmName = (String) reroute.get("realm");
        String targetProcedure = (String) reroute.get("procedure");

        Realm target = realmManager.getRealm(targetRealmName).orElse(null);
        if (target == null) {
            callerTransport.send(new WampMessage.Error(
                    WampMessageType.CALL.code(), call.requestId(),
                    Map.of(), "ssg.rerouting.realm_not_found: " + targetRealmName));
            return null;
        }

        Dealer targetDealer = target.getDealer();
        if (!targetDealer.isRegistered(targetProcedure)) {
            callerTransport.send(new WampMessage.Error(
                    WampMessageType.CALL.code(), call.requestId(),
                    Map.of(), "ssg.rerouting.no_procedure: " + targetProcedure));
            return null;
        }

        // Create a forwarding transport to capture the result
        // Protected factory method for testability
        var forwardResultQueue = createForwardQueue();
        var forwardTransport = new QueueTransport(forwardResultQueue);

        // Create the forwarded call with the target procedure
        var forwardedCall = new WampMessage.Call(call.requestId(), call.options(),
                targetProcedure, call.args());

        // Dispatch to target realm's dealer
        var invocation = targetDealer.handleCall(forwardedCall, forwardTransport, 0);
        if (invocation == null) {
            callerTransport.send(new WampMessage.Error(
                    WampMessageType.CALL.code(), call.requestId(),
                    Map.of(), "ssg.rerouting.no_callee"));
            return null;
        }

        // Wait for the result from the target realm (synchronous forwarding)
        try {
            WampMessage response = forwardResultQueue.poll(10, TimeUnit.SECONDS);
            if (response instanceof WampMessage.Result result) {
                callerTransport.send(result);
            } else if (response instanceof WampMessage.Error err) {
                callerTransport.send(err);
            } else {
                callerTransport.send(new WampMessage.Error(
                        WampMessageType.CALL.code(), call.requestId(),
                        Map.of(), "ssg.rerouting.unexpected_response"));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            callerTransport.send(new WampMessage.Error(
                    WampMessageType.CALL.code(), call.requestId(),
                    Map.of(), "ssg.rerouting.timeout"));
        }

        return invocation;
    }

    /**
     * Simple queue-based transport for capturing forwarded results.
     */
    static class QueueTransport implements WampTransport {
        private final BlockingQueue<WampMessage> queue;

        QueueTransport(BlockingQueue<WampMessage> queue) {
            this.queue = queue;
        }

        @Override
        public void send(WampMessage msg) {
            queue.offer(msg);
        }

        @Override
        public WampMessage receive() {
            try {
                return queue.take();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted", e);
            }
        }

        @Override
        public void close() {}

        @Override
        public boolean isOpen() { return true; }
    }
}
