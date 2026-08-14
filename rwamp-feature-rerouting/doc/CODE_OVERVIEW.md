# rwamp-feature-rerouting — Code Overview

> Cross-references: [README](../README.md) | [Architecture](ARCHITECTURE.md) | [Requirements](REQUIREMENTS.md)
> Parent: [Project CODE_OVERVIEW](../../doc/CODE_OVERVIEW.md)

## Source Structure

```
rwamp-feature-rerouting/src/main/java/ssg/rwamp/feature/rerouting/
├── ReroutingApi.java    — register/unregister reroute meta procedure
└── ReroutingDealer.java — forwarding logic with RealmManager lookup

rwamp-feature-rerouting/src/test/java/ssg/rwamp/feature/rerouting/
├── InMemoryTransport.java       — local copy
├── CallReroutingTest.java       — end-to-end cross-realm calls
└── ReroutingDealerTest.java     — Dealer forwarding logic
```

## Key Classes

| Class | Type | Purpose |
|-------|------|---------|
| `ReroutingApi` | Utility (static) | Register `wamp.reroute.call` meta procedure |
| `ReroutingDealer` | Stateless | Forward calls to target realm's Dealer |

## Design Notes

- `extractRerouteOption()` reads the `reroute` key from CALL options
- Returns `{error: "..."}` map for error cases (missing realm, missing procedure)
- `ReroutingDealer` uses `RealmManager` to resolve realms at call time
