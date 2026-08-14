# rwamp-feature-session — Architecture

## Module Purpose

Implements WAMP Advanced Profile session meta procedures: kill, killall, and interrupt. Allows any WAMP client to request session lifecycle management from the router.

## Key Abstractions

### SessionTransportTracker
A thread-safe map from session ID to `WampTransport`. The WAMP router does not track this mapping, so the tracker fills the gap. Applications must call `track()` on session creation and `untrack()` on session close.

```mermaid
classDiagram
    class SessionTransportTracker {
        +track(sessionId, transport)
        +getTransport(sessionId) Optional
        +untrack(sessionId) boolean
        +getActiveTransports() Map
        +getTrackedCount() int
    }
```

### SessionMetaApi
Static utility class that creates and registers meta procedure handlers. Each handler captures a reference to the `SessionTransportTracker` and implements the WAMP protocol for the respective procedure.

## Data Flow

```mermaid
sequenceDiagram
    participant Client
    participant Transport
    participant Router
    participant MetaHandler
    participant Tracker
    participant TargetTransport
    
    Client->>Transport: CALL wamp.session.kill([sessionId])
    Transport->>Router: route()
    Router->>MetaHandler: meta procedure handler
    MetaHandler->>Tracker: getTransport(targetId)
    Tracker-->>MetaHandler: Optional<Transport>
    MetaHandler->>TargetTransport: send(GOODBYE)
    MetaHandler->>Tracker: untrack(targetId)
    MetaHandler-->>Router: Result
    Router-->>Client: RESULT
```

## Thread Safety

- `SessionTransportTracker` uses `ConcurrentHashMap` internally
- `getActiveTransports()` returns an immutable snapshot via `Map.copyOf()`
- Meta procedure handlers run in the router's single-threaded message loop

## Extension Points Used

| Extension | From lego-flow | Purpose |
|-----------|---------------|---------|
| `WampRouter.registerMetaProcedure()` | WampRouter | Register kill handlers |
| `WampRouter.unregisterMetaProcedure()` | WampRouter | Clean unregistration |

---

**Last Updated**: 2026-08-14
