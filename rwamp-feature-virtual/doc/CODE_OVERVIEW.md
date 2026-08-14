# rwamp-feature-virtual — Code Overview

> Cross-references: [README](../README.md) | [Architecture](ARCHITECTURE.md) | [Requirements](REQUIREMENTS.md)
> Parent: [Project CODE_OVERVIEW](../../doc/CODE_OVERVIEW.md)

## Source Structure

```
rwamp-feature-virtual/src/main/java/ssg/rwamp/feature/virtual/
├── VirtualSessionApi.java      — register/unregister virtual session procedures
└── VirtualSessionManager.java  — session allocation + auth context

rwamp-feature-virtual/src/test/java/ssg/rwamp/feature/virtual/
├── InMemoryTransport.java      — local copy
└── VirtualSessionApiTest.java  — register/unregister flows
```

## Key Classes

| Class | Type | Purpose |
|-------|------|---------|
| `VirtualSessionApi` | Utility (static) | Register virtual session meta procedures |
| `VirtualSessionManager` | Stateful | Allocate IDs, create sessions, store in map |

## Design Notes

- `VirtualSessionManager.register()` returns a `long` session ID
- `VirtualSessionManager.getSession()` returns the `WampSession` or null
- Virtual sessions are added to the router via `sessionJoined()` for visibility
- `VirtualSessionManager` requires a `Realm` reference for session creation
