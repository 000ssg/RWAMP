# rwamp-rest

HTTP-to-WAMP bridge.

## Overview

Bridges HTTP requests to WAMP calls. Maps REST paths to WAMP procedure URIs, creates virtual sessions for HTTP callers, and returns HTTP responses from WAMP results.

## Usage

```java
var realm = new Realm("realm1");
var virtualManager = new VirtualSessionManager(realm);
var registry = ReflectionApi.createRegistry(router);

var bridge = new RestWampBridge(router, virtualManager, registry);

// Map HTTP path to WAMP procedure
var request = new RestRequest("GET", "/com/example/greet",
        List.of("World"), Map.of("authid", "user1"), null);
var response = bridge.handle(request);
// → RestResponse(200, ["Hello, World!"])
```

## API

| Class | Purpose |
|-------|---------|
| `RestWampBridge` | Bridges HTTP requests to WAMP calls |
| `RestRequest` | Record: method, path, pathParams, queryParams, body |
| `RestResponse` | Record: statusCode, body |

## Path Resolution

HTTP path `/com/example/greet` → WAMP procedure `com.example.greet`

## Dependencies

- `rwamp-feature-virtual` — for virtual session management
- `rwamp-feature-reflection` — for procedure discovery/validation
- `ssg:lego-flow-wamp` — for core WAMP types

## Cross-references

- [Architecture](doc/ARCHITECTURE.md) | [Requirements](doc/REQUIREMENTS.md) | [Code Overview](doc/CODE_OVERVIEW.md) | [Compliance](doc/COMPLIANCE.md)
- [Root Architecture](../doc/ARCHITECTURE.md) | [Root README](../README.md)
