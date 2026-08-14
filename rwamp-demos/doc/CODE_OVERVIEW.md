# rwamp-demos — Code Overview

## Source Structure

### `ssg.rwamp.demo`

| File | Purpose |
|------|---------|
| `DemoRunner.java` | Entry point. Run all demos or a single named demo. |

### `ssg.rwamp.demo.simple`

| File | Lines | Description |
|------|-------|-------------|
| `SimpleRpcDemo.java` | ~90 | One caller, one callee, one procedure through WampRouter |
| `SimplePubSubDemo.java` | ~90 | One subscriber, one publisher, event delivery through Broker |
| `SessionManagementDemo.java` | ~110 | Session tracking, kill, admin session management |

### `ssg.rwamp.demo.composite`

| File | Lines | Description |
|------|-------|-------------|
| `MultiClientRpcDemo.java` | ~140 | Shared registration with roundrobin/first/last/random policies |
| `MultiTopicPubSubDemo.java` | ~150 | Exact, prefix, wildcard subscriptions with event fan-out |
| `MultiRealmDemo.java` | ~160 | Two isolated realms, RPC + pub/sub per realm |
| `SharedRegistrationDemo.java` | ~140 | All invoke policies compared, rejection tracking |

### `ssg.rwamp.demo.feature`

| File | Lines | Description |
|------|-------|-------------|
| `TestamentDemo.java` | ~110 | Last-will events published on session close |
| `ReflectionDemo.java` | ~140 | Procedure/topic/type introspection via meta procedures |
| `StatisticsDemo.java` | ~110 | Message flow counting with StatisticsTransport |
| `VirtualSessionDemo.java` | ~110 | Transport-less sessions for REST bridge identity |
| `RestBridgeDemo.java` | ~140 | HTTP → WAMP call bridging with virtual sessions |

### `ssg.rwamp.demo.advanced`

| File | Lines | Description |
|------|-------|-------------|
| `MultiRouterDemo.java` | ~130 | Cross-realm RPC forwarding between routers |
| `MultiVersionApiDemo.java` | ~130 | v1 + v2 coexistence on WAMP and REST |
| `CompositeScenarioDemo.java` | ~200 | Full multi-tenant topology with all features |

### `ssg.rwamp.demo.infrastructure`

| File | Lines | Description |
|------|-------|-------------|
| `InMemoryTransport.java` | ~70 | Paired blocking-queue WAMP transport for demos |

## Key Design Notes

1. **No external dependencies beyond RWAMP modules** — demos only use `ssg:lego-flow-wamp` (transitive) and `slf4j-api`
2. **InMemoryTransport is local** — lego-flow's version is in test scope and unavailable as transitive dependency
3. **All demos are synchronous** — no threading, no async execution (in-memory transport is blocking)
4. **Each demo is a stateless singleton** — create new instance, call `run()`, get result
5. **Record return types** — each demo returns a typed result for structured access

## Cross-References

- [README.md](../README.md) — comprehensive scenario documentation with pros/cons
- [ARCHITECTURE.md](ARCHITECTURE.md) — module architecture and data flow
- [REQUIREMENTS.md](REQUIREMENTS.md) — original request and design decisions
- [lego-flow demos](https://github.com/000ssg/lego-flow/tree/main/demos) — upstream reference implementation
