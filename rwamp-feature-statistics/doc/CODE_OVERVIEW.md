# rwamp-feature-statistics — Code Overview

> Cross-references: [README](../README.md) | [Architecture](ARCHITECTURE.md) | [Requirements](REQUIREMENTS.md)
> Parent: [Project CODE_OVERVIEW](../../doc/CODE_OVERVIEW.md)

## Source Structure

```
rwamp-feature-statistics/src/main/java/ssg/rwamp/feature/statistics/
├── StatisticsApi.java         — register/unregister statistics meta procedure
├── StatisticsTransport.java   — WampTransport decorator with counting
└── WampStatistics.java        — per-realm counter storage

rwamp-feature-statistics/src/test/java/ssg/rwamp/feature/statistics/
├── InMemoryTransport.java     — local copy
├── StatisticsApiTest.java     — query flows
├── StatisticsTransportTest.java — decorator counting
└── WampStatisticsTest.java    — counter logic
```

## Key Classes

| Class | Type | Purpose |
|-------|------|---------|
| `StatisticsApi` | Utility (static) | Register/unregister the statistics meta procedure |
| `WampStatistics` | Stateful | Per-realm `ProcessorStatistics`-style counters |
| `StatisticsTransport` | Decorator | Wraps any `WampTransport`, counts on send/receive |

## Design Notes

- `WampStatistics.group(realm)` returns a per-realm counter view
- `snapshot()` returns a `Map<String, Object>` with realm→counters hierarchy
- `StatisticsTransport` delegates all transport operations to the wrapped instance after recording
