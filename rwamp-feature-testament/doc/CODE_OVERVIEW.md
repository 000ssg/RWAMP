# rwamp-feature-testament — Code Overview

> Cross-references: [README](../README.md) | [Architecture](ARCHITECTURE.md) | [Requirements](REQUIREMENTS.md)
> Parent: [Project CODE_OVERVIEW](../../doc/CODE_OVERVIEW.md)

## Source Structure

```
rwamp-feature-testament/src/main/java/ssg/rwamp/feature/testament/
├── TestamentApi.java       — register/unregister procedures, session leave hook
└── TestamentManager.java   — per-session testament storage + publish

rwamp-feature-testament/src/test/java/ssg/rwamp/feature/testament/
├── InMemoryTransport.java  — local copy
└── TestamentApiTest.java   — add, flush, auto-publish on close
```

## Key Classes

| Class | Type | Purpose |
|-------|------|---------|
| `TestamentApi` | Utility (static) | Register procedures, hook session leave |
| `TestamentManager` | Stateful | Store and publish testaments |

## Design Notes

- `TestamentManager.publishAll()` receives the `Broker` from the session leave callback
- Testament records store topic, args, kwargs, scope, and publish options
- `SCOPE_DESTROYED` constant: `"destroyed"`, `SCOPE_CLOSED` constant: `"closed"`
