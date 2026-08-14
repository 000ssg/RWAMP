# rwamp-feature-reflection

Runtime introspection for WAMP routers.

## Overview

Introspect registered procedures, subscribed topics, and type definitions at runtime.

| Procedure | Description |
|-----------|-------------|
| `wamp.reflection.procedure.list` | List registered procedure URIs |
| `wamp.reflection.procedure.describe` | Describe procedure details |
| `wamp.reflection.topic.list` | List subscribed topics |
| `wamp.reflection.topic.describe` | Describe topic details |
| `wamp.reflection.type.list` | List type/error definitions |
| `wamp.reflection.type.describe` | Describe type/error details |
| `wamp.reflection.error.list` | List error definitions |
| `wamp.reflect.define` | Define a new type/error |

## Usage

```java
var registry = ReflectionApi.createRegistry(router);
ReflectionApi.register(router, registry);

// Query procedures
router.route(new WampMessage.Call(1, Map.of(), "wamp.reflection.procedure.list", null), transport);
```

## API

| Class | Purpose |
|-------|---------|
| `ReflectionApi` | Register/unregister 8 introspection procedures |
| `ReflectionRegistry` | Tracks procedure/topic/type definitions from Dealer/Broker |

## Cross-references

- [Architecture](doc/ARCHITECTURE.md) | [Requirements](doc/REQUIREMENTS.md) | [Code Overview](doc/CODE_OVERVIEW.md) | [Compliance](doc/COMPLIANCE.md)
- [Root Architecture](../doc/ARCHITECTURE.md) | [Root README](../README.md)
