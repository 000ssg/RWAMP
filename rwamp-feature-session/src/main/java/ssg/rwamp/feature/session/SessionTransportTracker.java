package ssg.rwamp.feature.session;

import ssg.legoflow.wamp.core.transport.WampTransport;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks active WAMP session to transport mappings.
 * <p>
 * WampRouter maintains session IDs and session objects but does not track
 * the transport associated with each session. This tracker fills that gap,
 * enabling features like session kill that need to send messages to a
 * specific session's transport.
 * <p>
 * The application must call {@link #track(long, WampTransport)} when a session
 * is created and {@link #untrack(long)} when it is closed.
 */
public class SessionTransportTracker {

    private final Map<Long, WampTransport> transports = new ConcurrentHashMap<>();

    /**
     * Records a session's transport.
     *
     * @param sessionId the session identifier
     * @param transport the session's transport
     */
    public void track(long sessionId, WampTransport transport) {
        transports.put(sessionId, transport);
    }

    /**
     * Returns the transport for a session, if tracked.
     *
     * @param sessionId the session identifier
     * @return the transport, or empty if not tracked
     */
    public Optional<WampTransport> getTransport(long sessionId) {
        return Optional.ofNullable(transports.get(sessionId));
    }

    /**
     * Removes a session's transport tracking.
     *
     * @param sessionId the session identifier
     * @return {@code true} if the session was being tracked
     */
    public boolean untrack(long sessionId) {
        return transports.remove(sessionId) != null;
    }

    /**
     * Returns an unmodifiable snapshot of all tracked session-to-transport mappings.
     *
     * @return snapshot map
     */
    public Map<Long, WampTransport> getActiveTransports() {
        return Map.copyOf(transports);
    }

    /**
     * Returns the number of tracked sessions.
     *
     * @return the count
     */
    public int getTrackedCount() {
        return transports.size();
    }
}
