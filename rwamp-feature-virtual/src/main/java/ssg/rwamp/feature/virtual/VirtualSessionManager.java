package ssg.rwamp.feature.virtual;

import ssg.legoflow.wamp.core.WampSession;
import ssg.legoflow.wamp.core.realm.Realm;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages virtual WAMP sessions — sessions without a transport,
 * created programmatically for identity mapping (e.g., HTTP-authenticated
 * users mapped to WAMP sessions for REST bridge calls).
 * <p>
 * Virtual sessions appear in the realm's active sessions and participate
 * in pub/sub and RPC like regular sessions, but have no transport to
 * receive messages.
 *
 * @since 0.1.0
 */
public class VirtualSessionManager {

    private final Realm realm;
    /** virtual session ID -> session (for tracking which are virtual) */
    private final ConcurrentHashMap<Long, WampSession> virtualSessions = new ConcurrentHashMap<>();

    /**
     * Creates a new manager for the given realm.
     *
     * @param realm the WAMP realm
     */
    public VirtualSessionManager(Realm realm) {
        this.realm = realm;
    }

    /**
     * Creates a virtual session with the given authentication context.
     *
     * @param authId    the authentication identity (may be null)
     * @param authRole  the authentication role (may be null)
     * @param authMethod the auth method (may be null)
     * @return the assigned session ID
     */
    public long register(String authId, String authRole, String authMethod) {
        long sessionId = realm.addVirtualSession(authId, authRole, authMethod);
        var session = realm.getSession(sessionId);
        virtualSessions.put(sessionId, session);
        return sessionId;
    }

    /**
     * Removes a virtual session from the realm.
     *
     * @param sessionId the virtual session to remove
     */
    public void unregister(long sessionId) {
        virtualSessions.remove(sessionId);
        realm.removeSession(sessionId);
    }

    /**
     * Returns the virtual session with the given ID.
     *
     * @param sessionId the session ID
     * @return the session, or null if not found or not virtual
     */
    public WampSession getSession(long sessionId) {
        return virtualSessions.get(sessionId);
    }

    /**
     * Returns all active virtual sessions.
     *
     * @return map of session ID to session
     */
    public Map<Long, WampSession> getVirtualSessions() {
        return Map.copyOf(virtualSessions);
    }

    /**
     * Returns the number of active virtual sessions.
     *
     * @return the count
     */
    public int getCount() {
        return virtualSessions.size();
    }
}
