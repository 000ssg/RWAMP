package ssg.rwamp.feature.testament;

import ssg.legoflow.wamp.core.WampMessage;
import ssg.legoflow.wamp.core.router.Broker;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Manages testaments — events scheduled for publication when a session
 * is detached or destroyed.
 * <p>
 * Implements the WAMP Advanced Profile testament feature. Testaments are stored
 * per session ID and automatically published when the session lifecycle event fires.
 */
public class TestamentManager {

    /** Testament scope: publish when session is detached (transport lost). */
    public static final String SCOPE_DETACHED = "detached";
    /** Testament scope: publish when session is destroyed (explicit close). */
    public static final String SCOPE_DESTROYED = "destroyed";

    /** session ID -> list of testaments (destroyed scope) */
    private final ConcurrentHashMap<Long, ConcurrentLinkedQueue<TestamentEntry>> destroyedTestaments = new ConcurrentHashMap<>();
    /** session ID -> list of testaments (detached scope) */
    private final ConcurrentHashMap<Long, ConcurrentLinkedQueue<TestamentEntry>> detachedTestaments = new ConcurrentHashMap<>();

    /**
     * Stores a testament for the given session.
     *
     * @param sessionId  the session ID
     * @param topic      the topic to publish to
     * @param args       positional arguments
     * @param kwargs     keyword arguments
     * @param scope      "detached" or "destroyed"
     * @param pubOptions publish options (e.g., exclude_me, retain)
     */
    public void add(long sessionId, String topic, List<Object> args,
                    Map<String, Object> kwargs, String scope, Map<String, Object> pubOptions) {
        var queue = SCOPE_DETACHED.equals(scope) ? detachedTestaments : destroyedTestaments;
        queue.computeIfAbsent(sessionId, k -> new ConcurrentLinkedQueue<>())
                .add(new TestamentEntry(topic, args, kwargs, pubOptions));
    }

    /**
     * Flushes (removes) testaments for the given session and scope.
     *
     * @param sessionId the session ID
     * @param scope     "detached" or "destroyed" (default: destroyed)
     */
    public void flush(long sessionId, String scope) {
        var queue = SCOPE_DETACHED.equals(scope) ? detachedTestaments : destroyedTestaments;
        queue.remove(sessionId);
    }

    /**
     * Gets and clears all testaments for the given session (both scopes).
     * Called when a session is closed.
     *
     * @param sessionId the session ID
     * @return list of all testaments to publish
     */
    public List<TestamentEntry> takeAll(long sessionId) {
        var result = new ArrayList<TestamentEntry>();
        var destroyed = destroyedTestaments.remove(sessionId);
        if (destroyed != null) result.addAll(destroyed);
        var detached = detachedTestaments.remove(sessionId);
        if (detached != null) result.addAll(detached);
        return result;
    }

    /**
     * Publishes all testaments for a session to the broker.
     *
     * @param sessionId the session that left
     * @param broker    the broker to publish events through
     */
    public void publishAll(long sessionId, Broker broker) {
        var testaments = takeAll(sessionId);
        for (var entry : testaments) {
            var options = entry.pubOptions() != null ? entry.pubOptions() : Map.<String, Object>of("exclude_me", false);
            var publish = new WampMessage.Publish(0, options, entry.topic(), entry.args());
            broker.handlePublish(publish, null, sessionId);
        }
    }

    /**
     * A testament entry: topic, arguments, and publish options.
     */
    public record TestamentEntry(String topic, List<Object> args,
                                  Map<String, Object> kwargs, Map<String, Object> pubOptions) {}
}
