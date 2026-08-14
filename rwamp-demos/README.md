# RWAMP Demos

Comprehensive demonstration of RWAMP features and WAMP v2 usage patterns. Each demo is self-contained, uses in-memory transports for deterministic execution, and includes detailed documentation of the scenario, trade-offs, and resource implications.

## Quick Start

```java
// Run all demos
DemoRunner.runAll();

// Run a single demo
DemoRunner.run("rpc");
DemoRunner.run("rest");
DemoRunner.run("composite");
```

## Demo Index

### Simple Demos

| Demo | File | Scenario | Key Concepts |
|------|------|----------|--------------|
| **Simple RPC** | [`SimpleRpcDemo`](src/main/java/ssg/rwamp/demo/simple/SimpleRpcDemo.java) | One caller, one callee, one procedure | Basic RPC through WampRouter |
| **Simple Pub/Sub** | [`SimplePubSubDemo`](src/main/java/ssg/rwamp/demo/simple/SimplePubSubDemo.java) | One publisher, one subscriber, one topic | Event delivery through Broker |
| **Session Management** | [`SessionManagementDemo`](src/main/java/ssg/rwamp/demo/simple/SessionManagementDemo.java) | Multiple sessions, admin kills one | `wamp.session.kill`, transport tracking |

### Composite Demos

| Demo | File | Scenario | Key Concepts |
|------|------|----------|--------------|
| **Multi-Client RPC** | [`MultiClientRpcDemo`](src/main/java/ssg/rwamp/demo/composite/MultiClientRpcDemo.java) | 3 callees, 3+ callers, shared registration | Invoke policies: single, first, last, roundrobin, random |
| **Multi-Topic Pub/Sub** | [`MultiTopicPubSubDemo`](src/main/java/ssg/rwamp/demo/composite/MultiTopicPubSubDemo.java) | 3 subscribers (exact, prefix, wildcard) | Topic matching policies, event fan-out |
| **Multi-Realm** | [`MultiRealmDemo`](src/main/java/ssg/rwamp/demo/composite/MultiRealmDemo.java) | 2 isolated realms, RPC + pub/sub per realm | Realm-level isolation, namespace separation |
| **Shared Registration** | [`SharedRegistrationDemo`](src/main/java/ssg/rwamp/demo/composite/SharedRegistrationDemo.java) | Same procedure, 3 instances, all policies | Load distribution, registration rejection |

### RWAMP Feature Demos

| Demo | File | Scenario | Key Concepts |
|------|------|----------|--------------|
| **Testament** | [`TestamentDemo`](src/main/java/ssg/rwamp/demo/feature/TestamentDemo.java) | Session registers "last will" event | Auto-publish on session close |
| **Reflection API** | [`ReflectionDemo`](src/main/java/ssg/rwamp/demo/feature/ReflectionDemo.java) | Introspect procedures, topics, types | Runtime API discovery |
| **Statistics** | [`StatisticsDemo`](src/main/java/ssg/rwamp/demo/feature/StatisticsDemo.java) | Track message flow per realm | `StatisticsTransport`, counters |
| **Virtual Sessions** | [`VirtualSessionDemo`](src/main/java/ssg/rwamp/demo/feature/VirtualSessionDemo.java) | Auth mapping without transport | REST bridge identity |
| **REST Bridge** | [`RestBridgeDemo`](src/main/java/ssg/rwamp/demo/feature/RestBridgeDemo.java) | HTTP calls → WAMP procedures | Path-to-URI resolution, session reuse |

### Advanced Demos

| Demo | File | Scenario | Key Concepts |
|------|------|----------|--------------|
| **Multi-Router** | [`MultiRouterDemo`](src/main/java/ssg/rwamp/demo/advanced/MultiRouterDemo.java) | 2 routers, cross-realm forwarding | `ReroutingDealer`, service decomposition |
| **Multi-Version API** | [`MultiVersionApiDemo`](src/main/java/ssg/rwamp/demo/advanced/MultiVersionApiDemo.java) | v1 + v2 procedures, REST + WAMP | API versioning, backward compatibility |
| **Composite Scenario** | [`CompositeScenarioDemo`](src/main/java/ssg/rwamp/demo/advanced/CompositeScenarioDemo.java) | Full multi-tenant topology | All RWAMP features combined |

---

## Detailed Scenarios

### 1. Simple RPC (`SimpleRpcDemo`)

**Scenario:** The most basic WAMP RPC — one caller invokes one procedure on one callee.

**When to use:** Single-instance services, prototyping, testing.

| Aspect | Detail |
|--------|--------|
| **Latency** | Sub-microsecond (in-memory) |
| **Throughput** | Limited by single-threaded processing |
| **Reliability** | Single point of failure |
| **Resource cost** | Minimal (~1KB per session) |

**Trade-offs:**
- ✅ Fastest option for local processing
- ❌ No fault tolerance — callee death means service outage
- ❌ No scaling — single instance handles all calls

---

### 2. Shared Registration / Load Balancing (`SharedRegistrationDemo`)

**Scenario:** Multiple callee instances register the same procedure with different invoke policies.

**Invoke policies compared:**

| Policy | Distribution | Fault Tolerance | Resource Usage | Best For |
|--------|-------------|-----------------|----------------|----------|
| **single** | 1 instance only | None | Lowest | Singleton services |
| **first** | Always first registered | Standby available | Moderate | Primary + backup |
| **last** | Always most recent | Standby available | Moderate | Rolling deploys |
| **roundrobin** | Cyclic distribution | Partial (next instance) | Highest | Stateless services |
| **random** | Random selection | Partial (next call) | High | Even load spread |

**Trade-offs:**
- ✅ Horizontal scalability — add instances for capacity
- ❌ No health checking — dead instances still receive calls
- ❌ Stateful procedures need sticky sessions (not supported)
- ⚠️ "single" policy rejects duplicate registration (error returned)

---

### 3. Multi-Realm Isolation (`MultiRealmDemo`)

**Scenario:** Two separate realms with isolated procedures and topics.

**When to use:** Multi-tenant systems, security boundaries, service mesh segmentation.

| Aspect | Detail |
|--------|--------|
| **Isolation** | Full — separate Broker and Dealer per realm |
| **Resource cost** | ~100KB per realm (Broker + Dealer state) |
| **Cross-realm** | Requires explicit rerouting (`ReroutingDealer`) |

**Trade-offs:**
- ✅ Strong tenant isolation — no accidental cross-talk
- ✅ Per-realm resource management
- ❌ No automatic cross-realm discovery
- ❌ Same procedure must be registered in each realm
- ❌ Operational complexity — managing N realms × N procedures

---

### 4. Pattern-Based Subscriptions (`MultiTopicPubSubDemo`)

**Scenario:** Three subscribers with different matching policies (exact, prefix, wildcard).

| Match Policy | Pattern | Matches | Misses |
|-------------|---------|---------|--------|
| **exact** | `events.user.login` | `events.user.login` | `events.user.logout` |
| **prefix** | `events.user.` | `events.user.*` | `events.order.*` |
| **wildcard** | `events.*.created` | `events.user.created` | `events.user.login` |

**Trade-offs:**
- ✅ Flexible event routing without code changes
- ❌ Prefix matching is broad — may receive unwanted events
- ❌ Wildcard has fixed segment count — `com..bar` matches `com.X.bar` only
- ❌ No server-side filtering — subscriber receives all matching events

---

### 5. Testament / Last Will (`TestamentDemo`)

**Scenario:** A session schedules an event to be published when it closes.

**When to use:** Online/offline presence, resource cleanup, session audit logging.

| Aspect | Detail |
|--------|--------|
| **Trigger** | Any session close (graceful or crash) |
| **Scope** | `destroyed` (explicit close) or `detached` (transport lost) |
| **Latency** | Synchronous during teardown (~1μs) |

**Trade-offs:**
- ✅ Guaranteed "last words" — event always fires on close
- ✅ Decouples cleanup from session lifecycle
- ❌ No distinction between graceful and crash close
- ❌ No retry — if broker is down, event is lost
- ❌ Ordering — testament events may interleave with other close events

---

### 6. REST Bridge (`RestBridgeDemo`)

**Scenario:** HTTP clients call WAMP procedures through the REST bridge. Same procedure is callable via WAMP and HTTP.

**Path resolution:** `/com/example/greet` → `com.example.greet`

| Aspect | WAMP (Caller) | REST (Bridge) |
|--------|---------------|---------------|
| **Transport** | WebSocket / InMemory | HTTP (simulated) |
| **Session** | Persistent | Virtual per auth identity |
| **Communication** | Bidirectional | Request/response only |
| **Streaming** | Progressive results supported | Not supported |
| **Auth** | Full WAMP auth | Simple authid mapping |

**Trade-offs:**
- ✅ Unified API — same backend serves both protocols
- ✅ HTTP clients get WAMP capabilities without WebSocket
- ❌ Virtual sessions accumulate — need cleanup strategy
- ❌ No pub/sub subscription for HTTP clients
- ❌ Path convention (`/` → `.`) may collide with real URIs

---

### 7. Multi-Router / Cross-Realm Rerouting (`MultiRouterDemo`)

**Scenario:** Calls forwarded between realms managed by different routers.

```
Client → Router-A (realm.alpha) → [reroute] → Router-B (realm.beta) → Callee
```

**When to use:** Microservices, service mesh, geographic distribution.

| Aspect | Detail |
|--------|--------|
| **Latency** | +1-10ms per cross-router hop |
| **Reliability** | Depends on inter-router link |
| **Timeout** | Caller timeout must account for forwarding delay |

**Trade-offs:**
- ✅ Service decomposition — each router owns a domain
- ✅ Fault isolation — one router failure is contained
- ✅ Geographic distribution — routers in different DCs
- ❌ Added latency — each hop adds round-trip delay
- ❌ No automatic router discovery — client knows topology
- ❌ Debugging complexity — tracing across routers is hard

---

### 8. Multi-Version API (`MultiVersionApiDemo`)

**Scenario:** Two API versions (v1, v2) coexist on the same router, accessible via both WAMP and REST.

| Version | Procedure URI | REST Path | Response Format |
|---------|--------------|-----------|-----------------|
| **v1** | `com.app.greet.v1` | `/com/app/greet/v1` | Simple: `["Hello, Name!"]` |
| **v2** | `com.app.greet.v2` | `/com/app/greet/v2` | Structured: `{"name":"Name","version":2,...}` |

**When to use:** API evolution, gradual migration, backward compatibility.

**Trade-offs:**
- ✅ Zero-downtime migration — v1 and v2 coexist
- ✅ Gradual client migration — no forced deadline
- ❌ Code duplication — separate handlers per version
- ❌ Router namespace grows with each version
- ❌ Testing matrix: versions × transports × edge cases

---

### 9. Composite Scenario (`CompositeScenarioDemo`)

**Scenario:** Full multi-tenant microservices topology combining all RWAMP features.

```
┌──────────────────────────────────────────┐
│           WampRouter (shared)             │
│                                           │
│  Realm: tenant.alpha                      │
│  ├─ com.tenant.order.create (×2, rr)     │
│  └─ com.tenant.user.lookup (×1, first)   │
│                                           │
│  Realm: tenant.beta                       │
│  └─ com.tenant.reporting.generate (×1)   │
│                                           │
│  Features:                                │
│  ├─ Session tracking + kill               │
│  ├─ Statistics (per-realm counters)       │
│  ├─ Testament (cleanup on close)          │
│  ├─ Reflection API (introspection)        │
│  ├─ Virtual sessions (REST bridge)        │
│  ├─ REST bridge (HTTP → WAMP)             │
│  └─ Cross-realm rerouting                 │
└──────────────────────────────────────────┘
```

**Estimated resource usage:**
- WampRouter: ~5MB
- 2 realms: ~200KB
- 4 callee instances: ~800KB
- Statistics + session tracking: ~2KB
- **Total: ~7-8MB** for the entire topology

---

## Transport Comparison

| Transport | Use Case | Latency | Reliability | Setup |
|-----------|----------|---------|-------------|-------|
| **InMemory** | Demos, tests | <1μs | N/A (local) | Zero |
| **WebSocket** | Browser clients | 1-50ms | Reconnects | Server + client |
| **REST Bridge** | HTTP clients | 5-100ms | HTTP retry | Virtual sessions |

**When to use each:**
- **InMemory:** Testing, prototyping, single-JVM processing
- **WebSocket:** Real-time bidirectional communication (browsers, mobile)
- **REST Bridge:** Simple request/response from HTTP clients, server-to-server

---

## Architecture Patterns

### Pattern 1: Single-Router Multi-Tenant

```
         ┌───────────┐    ┌───────────┐
         │  Tenant A  │    │  Tenant B  │
         │ (realm.a)  │    │ (realm.b)  │
         └─────┬──────┘    └─────┬──────┘
               │                  │
               └────────┬─────────┘
                        │
                   ┌────┴────┐
                   │ Router  │
                   └─────────┘
```
- **Pros:** Shared infrastructure, simple deployment
- **Cons:** Single point of failure, all tenants share resources
- **Use when:** Low tenant count, trust boundary within one org

### Pattern 2: Distributed Routers

```
     ┌─────────┐    ┌─────────┐
     │Router A │    │Router B │
     │realm.a  │◄───│realm.b  │
     └─────────┘    └─────────┘
         │               │
    Tenant A         Tenant B
```
- **Pros:** Fault isolation, independent scaling
- **Cons:** Cross-router latency, topology knowledge required
- **Use when:** Geographic distribution, strict isolation needs

### Pattern 3: Hybrid REST + WAMP

```
   HTTP Client          WebSocket Client
         │                      │
         ▼                      ▼
     ┌─────────┐          ┌─────────┐
     │ REST    │          │  WAMP   │
     │ Bridge  │          │ Caller  │
     └────┬────┘          └────┬────┘
          │                    │
          └────────┬───────────┘
                   │
              ┌────┴────┐
              │ Router  │
              └─────────┘
```
- **Pros:** Protocol flexibility, unified backend
- **Cons:** REST bridge adds complexity, virtual session management
- **Use when:** Serving both browser and HTTP clients

---

## Running Demos

### Gradle

```bash
# Compile demos
./gradlew :rwamp-demos:compileJava

# Run all demos
./gradlew :rwamp-demos:run

# Run a specific demo
java -cp build/classes/java/main:$(./gradlew dependencies --render-mode=plain 2>/dev/null | grep lego-flow-wamp) \
  ssg.rwamp.demo.DemoRunner rpc
```

### Direct Java

```bash
java -cp <classpath> ssg.rwamp.demo.simple.SimpleRpcDemo
```

---

## Cross-References

- [RWAMP Root README](../README.md)
- [Architecture](doc/ARCHITECTURE.md)
- [Requirements](doc/REQUIREMENTS.md)
- [Code Overview](doc/CODE_OVERVIEW.md)
- [lego-flow WAMP demos](https://github.com/000ssg/lego-flow/tree/main/demos) (upstream reference)
