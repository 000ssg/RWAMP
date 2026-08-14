# rwamp-demos — Architecture

## Purpose

The `rwamp-demos` module provides executable usage examples for all RWAMP features. Each demo is a self-contained Java class that demonstrates a specific scenario, including the message flow, trade-offs, and resource implications.

## Module Structure

```
rwamp-demos/
├── src/main/java/ssg/rwamp/demo/
│   ├── DemoRunner.java              — entry point, runs all or single demo
│   ├── simple/
│   │   ├── SimpleRpcDemo.java       — basic RPC through WampRouter
│   │   ├── SimplePubSubDemo.java    — basic pub/sub through Broker
│   │   └── SessionManagementDemo.java — session kill via meta procedures
│   ├── composite/
│   │   ├── MultiClientRpcDemo.java  — shared registration, load balancing
│   │   ├── MultiTopicPubSubDemo.java — exact/prefix/wildcard subscriptions
│   │   ├── MultiRealmDemo.java      — cross-realm isolation
│   │   └── SharedRegistrationDemo.java — invoke policy comparison
│   ├── feature/
│   │   ├── TestamentDemo.java       — last-will events on session close
│   │   ├── ReflectionDemo.java      — runtime introspection
│   │   ├── StatisticsDemo.java      — message flow counters
│   │   ├── VirtualSessionDemo.java  — transport-less sessions
│   │   └── RestBridgeDemo.java      — HTTP → WAMP bridging
│   ├── advanced/
│   │   ├── MultiRouterDemo.java     — cross-realm rerouting
│   │   ├── MultiVersionApiDemo.java — API versioning (v1 + v2)
│   │   └── CompositeScenarioDemo.java — full multi-tenant topology
│   └── infrastructure/
│       └── InMemoryTransport.java   — paired in-memory WAMP transport
├── build.gradle.kts
├── README.md
└── doc/
    ├── ARCHITECTURE.md (this file)
    ├── REQUIREMENTS.md
    └── CODE_OVERVIEW.md
```

## Key Abstractions

### InMemoryTransport
A paired blocking-queue transport for deterministic demos. Not published — local copy of lego-flow's test transport.

### DemoRunner
Centralized entry point. Supports running all demos or a single named demo.

### Demo Result Records
Each demo class returns a typed `record` result containing the scenario's outcomes, enabling programmatic verification and comparison.

## Data Flow

```
Demo Class
    │
    ├── Creates InMemoryTransport pairs
    ├── Configures WampRouter + RWAMP features
    ├── Sends WampMessages through transports
    ├── Routes through Broker/Dealer/ReroutingDealer
    └── Collects results from receiver transports
```

## Design Decisions

1. **InMemoryTransport over real transports** — Demos must be deterministic and fast. Real WebSocket/HTTP transports add latency and complexity.

2. **One demo per class** — Each class demonstrates exactly one pattern. No shared state between demos.

3. **record return types** — Each demo returns structured results for programmatic access and documentation.

4. **main() in each demo** — Every demo class has a standalone main method for individual execution.

5. **Not published** — This module is excluded from Maven publishing and JaCoCo coverage.

## Dependencies

This module depends on all RWAMP feature modules:
- `rwamp-feature-session` (SessionMetaApi, SessionTransportTracker)
- `rwamp-feature-statistics` (StatisticsApi, StatisticsTransport, WampStatistics)
- `rwamp-feature-testament` (TestamentApi, TestamentManager)
- `rwamp-feature-reflection` (ReflectionApi, ReflectionRegistry)
- `rwamp-feature-virtual` (VirtualSessionApi, VirtualSessionManager)
- `rwamp-feature-rerouting` (ReroutingApi, ReroutingDealer)
- `rwamp-rest` (RestWampBridge, RestRequest, RestResponse)
- `rwamp-feature-registration` (PatternRegistry, RegistrationHandler)

Through transitive dependencies, all demos have access to lego-flow's WAMP core.

## Thread Safety

All demos run single-threaded. RWAMP features are thread-safe but demos don't exercise concurrency. For production scenarios, use virtual threads or executors as demonstrated in lego-flow's WebSocket demos.
