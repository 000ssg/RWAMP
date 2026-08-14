# rwamp-feature-statistics

Statistics tracking for WAMP routers.

## Overview

Provides WAMP statistics meta procedure and a transport wrapper that counts messages per type.

| Procedure | Description |
|-----------|-------------|
| `wamp.statistics.get` | Return a snapshot of per-realm counters |

## Usage

```java
var stats = new WampStatistics();
StatisticsApi.register(router, stats);

// Wrap a transport for counting
var counting = new StatisticsTransport(transport, stats, "realm1");
```

## API

| Class | Purpose |
|-------|---------|
| `StatisticsApi` | Register/unregister `wamp.statistics.get` meta procedure |
| `WampStatistics` | Per-realm counters with snapshot support |
| `StatisticsTransport` | Decorator wrapping any `WampTransport` with counting |

## Cross-references

- [Architecture](doc/ARCHITECTURE.md) | [Requirements](doc/REQUIREMENTS.md) | [Code Overview](doc/CODE_OVERVIEW.md) | [Compliance](doc/COMPLIANCE.md)
- [Root Architecture](../doc/ARCHITECTURE.md) | [Root README](../README.md)
