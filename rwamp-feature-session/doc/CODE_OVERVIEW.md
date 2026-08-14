# rwamp-feature-session — Code Overview

> Cross-references: [README](../README.md) | [Architecture](ARCHITECTURE.md) | [Requirements](REQUIREMENTS.md)
> Parent: [Project CODE_OVERVIEW](../../doc/CODE_OVERVIEW.md)

## Source Structure

```
rwamp-feature-session/src/main/java/ssg/rwamp/feature/session/
├── SessionMetaApi.java          — register/unregister kill procedures
└── SessionTransportTracker.java — session→transport ConcurrentHashMap

rwamp-feature-session/src/test/java/ssg/rwamp/feature/session/
├── InMemoryTransport.java       — local copy of lego-flow's InMemoryTransport
├── SessionMetaApiTest.java      — kill, killall, interrupt end-to-end
└── SessionTransportTrackerTest.java — track/untrack/snapshot
```

## Key Classes

| Class | Type | Purpose |
|-------|------|---------|
| `SessionMetaApi` | Utility (static) | Register/unregister meta procedures; creates handlers |
| `SessionTransportTracker` | Stateful | Thread-safe session→transport mapping |

## Design Notes

- `SessionMetaApi` is a sealed utility class (private constructor)
- Handlers capture the `SessionTransportTracker` reference via closure
- `createKillHandler()`, `createKillAllHandler()`, `createInterruptHandler()` are package-private for testing
- `extractCallerSession()` helper reads `_caller_session` from CALL options
