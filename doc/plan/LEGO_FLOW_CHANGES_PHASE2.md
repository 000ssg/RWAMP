# Legoflow Changes Required for Phase 2

**Status:** Proposed — awaiting user approval
**Impact:** Minimal (4 new public methods, no breaking changes)

---

## Architecture Finding

RWAMP discovered that lego-flow's WAMP has two separate architectures:

| Component | `WampRouter` (standalone) | `Realm` + `WebSocketWampService` |
|-----------|---------------------------|-----------------------------------|
| Session management | `WampRouter.activeSessions` (internal) | `Realm.sessions` |
| Broker/Dealer | Internal `Broker` + `Dealer` | `Realm.getBroker()` + `Realm.getDealer()` |
| Session lifecycle | `router.sessionJoined()` / `router.sessionLeft()` | `realm.addSession()` / `realm.removeSession()` |
| Meta events | Fires via internal `broker.handlePublish()` | No meta events fired |
| Used by | In-memory testing, single-router setups | WebSocket connections (production) |

This means features relying on `WampRouter` meta-procedures work in standalone mode,
but features that interact with `Realm`-based sessions need Realm-level changes.

---

## Required Changes

### 1. `Realm` — addVirtualSession()

**What:** Add method to create sessions without a transport (for Virtual Sessions)
**Why:** Virtual Sessions represent HTTP-authenticated users mapped to WAMP identity;
they have no WebSocket transport.

```java
// In Realm.java

/**
 * Adds a virtual session with the given authentication context.
 * Virtual sessions have no transport — they exist purely for identity mapping
 * (e.g., HTTP-authenticated users calling WAMP procedures via REST bridge).
 *
 * @param authId    the authentication identity (may be null)
 * @param authRole  the authentication role (may be null)
 * @param authMethod the auth method used (may be null)
 * @return the assigned session ID
 * @since 0.3.0
 */
public long addVirtualSession(String authId, String authRole, String authMethod) {
    long sessionId = sessionIdCounter.getAndIncrement();
    var session = new WampSession();
    session.establish(sessionId, name);
    if (authId != null) session.setAuthId(authId);
    if (authRole != null) session.setAuthRole(authRole);
    if (authMethod != null) session.setAuthMethod(authMethod);
    sessions.put(sessionId, session);
    return sessionId;
}
```

### 2. `Dealer` — getRegisteredProcedures()

**What:** Return a list of all registered procedure URIs
**Why:** Needed by Reflection API (`wamp.reflection.procedure.list`) to introspect
available procedures at runtime.

```java
// In Dealer.java

/**
 * Returns the set of all registered procedure URIs.
 *
 * @return unmodifiable set of procedure names
 * @since 0.3.0
 */
public Set<String> getRegisteredProcedures() {
    return Set.copyOf(registrations.keySet());
}
```

### 3. `Broker` — getSubscriptionTopics() + topic field in SubscriptionEntry

**What:** Expose topic URIs that have active subscriptions
**Why:** Needed by Reflection API (`wamp.reflection.topic.list`) to introspect
active topics at runtime.

```java
// In Broker.java

/**
 * Returns the set of all topics that have active exact-match subscriptions.
 * Does not include prefix or wildcard subscription patterns.
 *
 * @return unmodifiable set of topic URIs
 * @since 0.3.0
 */
public Set<String> getSubscriptionTopics() {
    return Set.copyOf(topicSubscriptions.keySet());
}

/**
 * Returns the set of all prefix-matched subscription patterns.
 *
 * @return unmodifiable set of prefix patterns
 * @since 0.3.0
 */
public Set<String> getPrefixSubscriptionPatterns() {
    return Set.copyOf(prefixSubscriptions.keySet());
}

/**
 * Returns the set of all wildcard-matched subscription patterns.
 *
 * @return unmodifiable set of wildcard patterns
 * @since 0.3.0
 */
public Set<String> getWildcardSubscriptionPatterns() {
    return Set.copyOf(wildcardSubscriptions.keySet());
}

// Update SubscriptionEntry record to include topic:
record SubscriptionEntry(long subscriptionId, WampTransport transport, long sessionId, String topic) {
    SubscriptionEntry(long subscriptionId, WampTransport transport, long sessionId) {
        this(subscriptionId, transport, sessionId, null);
    }
}
```

### 4. `WampRouter` — session lifecycle hook (optional)

**What:** A callback mechanism for when sessions join/leave, usable by Testament
manager for session lifecycle events in Realm-based deployments.

```java
// In WampRouter.java

/**
 * Registers a callback invoked when a session leaves.
 * Used by extension features (e.g., Testament API) to perform
 * cleanup or publish lifecycle events.
 *
 * @param listener the callback (sessionId -> void)
 * @since 0.3.0
 */
public void addSessionLeaveConsumer(Consumer<Long> listener) {
    sessionLeaveListeners.add(listener);
}
```

**Note:** The Testament API can alternatively subscribe to `wamp.session.on_leave`
meta events (published by `WampRouter.sessionLeft()`). This is preferred for
standalone WampRouter usage. For Realm-based deployments, the callback approach
is needed since `WebSocketWampService` does not call `WampRouter.sessionLeft()`.

---

## Impact Analysis

- **No breaking changes** — all additions, no modifications to existing APIs
- **Backward compatible** — default behaviors unchanged
- **Version bump** — lego-flow `0.3.0` (minor version for new public APIs)
- **RWAMP dependency** — update `legoFlowVersion` to `0.3.0-SNAPSHOT`
