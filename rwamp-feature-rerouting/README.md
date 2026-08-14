# rwamp-feature-rerouting

Cross-realm RPC forwarding for WAMP.

## Overview

Forwards WAMP calls to procedures registered in other realms. Enables distributed RPC across realm boundaries.

| Procedure | Description |
|-----------|-------------|
| `wamp.reroute.call` | Forward a call to a procedure in another realm |

## Usage

```java
var realmManager = new RealmManager();
realmManager.createRealm("realm1");
realmManager.createRealm("realm2");

var router = new WampRouter();
ReroutingApi.register(router, realmManager);

// Call with reroute option:
var options = Map.of("reroute", Map.of("realm", "realm2", "procedure", "com.example.add"));
router.route(new WampMessage.Call(1, options, "wamp.reroute.call", args), transport);
```

## API

| Class | Purpose |
|-------|---------|
| `ReroutingApi` | Register/unregister reroute meta procedure |
| `ReroutingDealer` | Forwarding logic with RealmManager lookup |

## Cross-references

- [Architecture](doc/ARCHITECTURE.md) | [Requirements](doc/REQUIREMENTS.md) | [Code Overview](doc/CODE_OVERVIEW.md) | [Compliance](doc/COMPLIANCE.md)
- [Root Architecture](../doc/ARCHITECTURE.md) | [Root README](../README.md)
