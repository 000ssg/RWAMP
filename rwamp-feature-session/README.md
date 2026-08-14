# rwamp-feature-session

Session kill meta procedures for WAMP routers.

## Overview

Implements WAMP Advanced Profile session meta procedures:

| Procedure | Description |
|-----------|-------------|
| `wamp.session.kill` | Kill a single session by ID |
| `wamp.session.killall` | Kill sessions matching authid/authrole |
| `wamp.session.interrupt` | Interrupt pending calls for a session |

## Usage

```java
var tracker = new SessionTransportTracker();
SessionMetaApi.register(router, tracker);

// Track session transport when created
tracker.track(sessionId, transport);
```

## API

| Class | Purpose |
|-------|---------|
| `SessionMetaApi` | Register/unregister kill procedures on WampRouter |
| `SessionTransportTracker` | ConcurrentHashMap-backed session→transport map |

## Cross-references

- [Architecture](doc/ARCHITECTURE.md) | [Requirements](doc/REQUIREMENTS.md) | [Code Overview](doc/CODE_OVERVIEW.md) | [Compliance](doc/COMPLIANCE.md)
- [Root Architecture](../doc/ARCHITECTURE.md) | [Root README](../README.md)
