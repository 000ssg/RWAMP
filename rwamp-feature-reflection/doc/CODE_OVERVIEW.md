# rwamp-feature-reflection — Code Overview

> Cross-references: [README](../README.md) | [Architecture](ARCHITECTURE.md) | [Requirements](REQUIREMENTS.md)
> Parent: [Project CODE_OVERVIEW](../../doc/CODE_OVERVIEW.md)

## Source Structure

```
rwamp-feature-reflection/src/main/java/ssg/rwamp/feature/reflection/
├── ReflectionApi.java     — register/unregister 8 introspection procedures
└── ReflectionRegistry.java — procedure/topic/type tracking

rwamp-feature-reflection/src/test/java/ssg/rwamp/feature/reflection/
├── InMemoryTransport.java      — local copy
├── ReflectionApiTest.java      — end-to-end procedure/topic queries
└── ReflectionRegistryTest.java — registry state management
```

## Key Classes

| Class | Type | Purpose |
|-------|------|---------|
| `ReflectionApi` | Utility (static) | Register 8 meta procedures; handler factory |
| `ReflectionRegistry` | Stateful | Track procedures from Dealer, topics from Broker |

## Design Notes

- `createRegistry(router)` factory method extracts Dealer and Broker from router
- Procedure and topic details return `Map<String, Object>` for protocol compatibility
- Topic list returns 3 lists: exact, prefix, wildcard topics
- `defineHandler` stores type/error metadata in `ConcurrentHashMap`
