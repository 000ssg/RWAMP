# rwamp-rest — Code Overview

> Cross-references: [README](../README.md) | [Architecture](ARCHITECTURE.md) | [Requirements](REQUIREMENTS.md)
> Parent: [Project CODE_OVERVIEW](../../doc/CODE_OVERVIEW.md)

## Source Structure

```
rwamp-rest/src/main/java/ssg/rwamp/rest/
├── RestRequest.java        — record: method, path, params, body
├── RestResponse.java       — record: statusCode, body; factory methods
└── RestWampBridge.java     — HTTP-to-WAMP bridge

rwamp-rest/src/test/java/ssg/rwamp/rest/
├── InMemoryTransport.java  — local copy
└── RestWampBridgeTest.java — end-to-end REST→WAMP flows
```

## Key Classes

| Class | Type | Purpose |
|-------|------|---------|
| `RestWampBridge` | Stateful | Bridge HTTP to WAMP; manages sessions |
| `RestRequest` | Record | HTTP request model |
| `RestResponse` | Record | HTTP response with status code |

## Design Notes

- `resolveProcedure()` strips leading slash, replaces `/` with `.`
- `getOrCreateVirtualSession()` uses `computeIfAbsent` for thread-safe reuse
- `ResultTransport` (inner class) captures the first message sent to it
- `RestResponse.ok()`, `.notFound()`, `.serverError()` — factory methods
- `publish()` method supports topic publishing in addition to procedure calls
