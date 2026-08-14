package ssg.rwamp.feature.statistics;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread-safe WAMP message and call statistics counters.
 * <p>
 * Tracks counts for WAMP protocol operations: calls, results, errors,
 * publishes, subscriptions, registrations, and raw message flow.
 * Supports per-realm granularity via named counter groups.
 *
 * @since 0.1.0
 */
public class WampStatistics {

    private final Map<String, CounterGroup> groups = new ConcurrentHashMap<>();

    /**
     * Returns the counter group for the given realm name, creating it if absent.
     *
     * @param realm the realm name (or any group identifier)
     * @return the counter group
     */
    public CounterGroup group(String realm) {
        return groups.computeIfAbsent(realm, CounterGroup::new);
    }

    /**
     * Returns a snapshot of all groups as a nested map.
     *
     * @return realm name → counter map
     */
    public Map<String, Object> snapshot() {
        var result = new ConcurrentHashMap<String, Object>();
        for (var entry : groups.entrySet()) {
            result.put(entry.getKey(), entry.getValue().snapshot());
        }
        return Map.copyOf(result);
    }

    /**
     * Thread-safe counters for a single realm or group.
     */
    public static class CounterGroup {

        private final String name;
        private final AtomicLong calls = new AtomicLong();
        private final AtomicLong results = new AtomicLong();
        private final AtomicLong errors = new AtomicLong();
        private final AtomicLong publishes = new AtomicLong();
        private final AtomicLong events = new AtomicLong();
        private final AtomicLong subscribes = new AtomicLong();
        private final AtomicLong registers = new AtomicLong();
        private final AtomicLong messagesIn = new AtomicLong();
        private final AtomicLong messagesOut = new AtomicLong();

        CounterGroup(String name) {
            this.name = name;
        }

        void call() { calls.incrementAndGet(); }
        void result() { results.incrementAndGet(); }
        void error() { errors.incrementAndGet(); }
        void publish() { publishes.incrementAndGet(); }
        void event() { events.incrementAndGet(); }
        void subscribe() { subscribes.incrementAndGet(); }
        void register() { registers.incrementAndGet(); }
        void messageIn() { messagesIn.incrementAndGet(); }
        void messageOut() { messagesOut.incrementAndGet(); }

        /**
         * Returns a snapshot of all counters.
         */
        Map<String, Object> snapshot() {
            var map = new java.util.HashMap<String, Object>();
            map.put("calls", calls.get());
            map.put("results", results.get());
            map.put("errors", errors.get());
            map.put("publishes", publishes.get());
            map.put("events", events.get());
            map.put("subscribes", subscribes.get());
            map.put("registers", registers.get());
            map.put("messages_in", messagesIn.get());
            map.put("messages_out", messagesOut.get());
            return map;
        }

        /**
         * Resets all counters to zero.
         */
        void reset() {
            calls.set(0);
            results.set(0);
            errors.set(0);
            publishes.set(0);
            events.set(0);
            subscribes.set(0);
            registers.set(0);
            messagesIn.set(0);
            messagesOut.set(0);
        }
    }
}
