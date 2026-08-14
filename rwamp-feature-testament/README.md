# rwamp-feature-testament

Testament scheduling for WAMP sessions.

## Overview

Schedule events for automatic publication when a session closes. This is the WAMP "last will and testament" feature.

| Procedure | Description |
|-----------|-------------|
| `wamp.session.add_testament` | Schedule a publication for the calling session's close |
| `wamp.session.flush_testament` | Remove scheduled testaments for the calling session |

## Usage

```java
var manager = new TestamentManager();
TestamentApi.register(router, manager);

// The router automatically publishes testaments when sessions close
// because TestamentApi hooks into WampRouter.addSessionLeaveConsumer()
```

## API

| Class | Purpose |
|-------|---------|
| `TestamentApi` | Register/unregister testament procedures + session leave hook |
| `TestamentManager` | Stores testaments per session, publishes on close |

## Cross-references

- [Architecture](doc/ARCHITECTURE.md) | [Requirements](doc/REQUIREMENTS.md) | [Code Overview](doc/CODE_OVERVIEW.md) | [Compliance](doc/COMPLIANCE.md)
- [Root Architecture](../doc/ARCHITECTURE.md) | [Root README](../README.md)
