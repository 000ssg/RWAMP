# rwamp-feature-registration

Pattern-based procedure registration for WAMP.

## Overview

Extends WAMP registration with pattern matching (exact, prefix, wildcard) and revocation support.

| Procedure | Description |
|-----------|-------------|
| `wamp.registration.list` | List pattern registrations |
| `wamp.registration.revoke` | Revoke a registration by ID |

## Usage

```java
var interceptor = new RegistrationInterceptor();
var router = new WampRouter();
interceptor.registerMetaProcedures(router);

// Intercept before routing:
WampMessage response = interceptor.intercept(msg, transport);
if (response != null) {
    transport.send(response);
} else {
    router.route(msg, transport);
}

// Register with prefix pattern
interceptor.intercept(new WampMessage.Register(1,
        Map.of("match", "prefix"), "com.example."), calleeTransport);

// Register with wildcard pattern
interceptor.intercept(new WampMessage.Register(2,
        Map.of("match", "wildcard"), "com.*.bar"), calleeTransport);
```

## Match Types

| Type | Example | Matches |
|------|---------|---------|
| `exact` | `com.example.foo` | Only `com.example.foo` |
| `prefix` | `com.example.` | `com.example.foo`, `com.example.bar.baz` |
| `wildcard` | `com.*.bar` | `com.example.bar` (single segment) |

## API

| Class | Purpose |
|-------|---------|
| `RegistrationInterceptor` | Message filter: intercepts REGISTER/UNREGISTER/CALL |
| `PatternRegistry` | Stores pattern registrations with transport references |
| `PatternMatcher` | Exact, prefix, wildcard matching utility |
| `RegistrationHandler` | Processes REGISTER/UNREGISTER messages |
| `RegistrationMetaApi` | Registers list and revoke meta procedures |

## Cross-references

- [Architecture](doc/ARCHITECTURE.md) | [Requirements](doc/REQUIREMENTS.md) | [Code Overview](doc/CODE_OVERVIEW.md) | [Compliance](doc/COMPLIANCE.md)
- [Root Architecture](../doc/ARCHITECTURE.md) | [Root README](../README.md)
