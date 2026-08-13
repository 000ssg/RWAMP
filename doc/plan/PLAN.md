# RWAMP — Detailed Implementation Plan

**Project:** RWAMP — Extended WAMP v2 implementation for Java 25+
**Date:** 2026-08-13
**Status: In Progress (Phase 0 completed — lego-flow changes merged)**

---

## 1. Project Overview

RWAMP is a **WAMP v2 extension project built on top of lego-flow's WAMP implementation**.
It re-uses lego-flow's core WAMP components (messages, session, broker, dealer, router,
serialization, auth, WebSocket transport) and adds the missing production-grade features
found in xLib's WAMP implementation.

### Key Architectural Decision

**lego-flow's WAMP is the foundation, not xLib.** RWAMP depends on `ssg:lego-flow-wamp`
and extends it. No WAMP-specific structures (messages, session, broker, dealer, serialization)
should be re-created — they are re-used from lego-flow. xLib serves only as a **reference
for feature design**, not as a code source.

Where lego-flow's WAMP needed changes to support new features, those changes were implemented
upstream in lego-flow (see Section 3).

### Goals

- **Re-use lego-flow WAMP fully** — messages, session, broker, dealer, router, realm,
  serialization, auth, WebSocket transport
- **Add xLib features missing in lego-flow** — session kill, testaments, virtual sessions,
  reflection API, call timeout, call rerouting, statistics, REST bridge
- **Avoid duplicating WAMP structures** — no parallel WampMessage, Broker, Dealer, etc.
- **Follow MDB-SQL dependency patterns** — GitHub Packages, dual-build, unique artifact names

---

## 2. Architecture

### 2.1 What RWAMP Re-uses from lego-flow

| Component | lego-flow Class | RWAMP Usage |
|-----------|----------------|-------------|
| Messages | `WampMessage` (sealed interface + records) | Re-used as-is |
| Session | `WampSession` + `SessionState` | Uses getState() for lifecycle |
| Broker | `Broker` | Re-used as-is |
| Dealer | `Dealer` + setTimeoutExecutor() | Uses timeout feature |
| Router | `WampRouter` + registerMetaProcedure() | Uses meta procedure hooks |
| Realm | `Realm` + getActiveSessions() | Uses session snapshot |
| Client roles | `Caller`, `Callee`, `Publisher`, `Subscriber` | Re-used as-is |
| Serialization | JSON, MessagePack, CBOR serializers | Re-used as-is |
| Transport | `WampTransport` interface, `InMemoryTransport` | Re-used as-is |
| Auth | `CraAuth`, `TicketAuth`, `CryptosignAuth` | Re-used as-is |
| WebSocket | `WebSocketWampService`, `WampWebSocketHandler` | Re-used as-is |

### 2.2 What RWAMP Adds (New Code)

| Feature | Source Inspiration | Module |
|---------|-------------------|--------|
| Session kill procedures (`wamp.session.kill*`) | xLib `WAMP_FP_SessionMetaAPI` | `rwamp-feature-session` |
| Testament API (`wamp.session.add_testament`) | xLib `WAMP_FP_TestamentMetaAPI` | `rwamp-feature-testament` |
| Virtual sessions (`virtual_session.register`) | xLib `WAMP_FP_VirtualSession` | `rwamp-feature-virtual` |
| Reflection API (`wamp.reflection.*`) | xLib `WAMP_FP_Reflection` | `rwamp-feature-reflection` |
| Statistics tracking | xLib `WAMPStatistics` | `rwamp-feature-statistics` |
| REST over WAMP bridge | xLib `REST_WAMP_MethodsProvider` | `rwamp-rest` |
| Call rerouting | xLib `WAMPRPCDealer` rerouting | `rwamp-feature-rerouting` |
| Pattern-based registration | xLib `WAMPRPCDealer` pattern | `rwamp-feature-registration` |

### 2.3 Module Structure

```mermaid
graph TD
    subgraph "lego-flow (GitHub Packages dependency)"
        LF1["ssg:lego-flow-wamp<br/>core: messages, session, broker, dealer, router,<br/>realm, serialization, auth, websocket"]
    end

    subgraph "RWAMP (extension project)"
        RW1["rwamp-feature-session<br/>kill procedures, state machine"]
        RW2["rwamp-feature-testament<br/>testament scheduling + lifecycle hooks"]
        RW3["rwamp-feature-virtual<br/>virtual session manager"]
        RW4["rwamp-feature-reflection<br/>procedure/topic/type introspection"]
        RW5["rwamp-feature-statistics<br/>call + message counters"]
        RW6["rwamp-feature-rerouting<br/>cross-realm call forwarding"]
        RW7["rwamp-feature-registration<br/>pattern matching + revocation"]
        RW8["rwamp-rest<br/>REST over WAMP bridge"]
    end

    RW1 --> LF1
    RW2 --> LF1
    RW3 --> LF1
    RW4 --> LF1
    RW5 --> LF1
    RW6 --> LF1
    RW7 --> LF1
    RW8 --> LF1
    RW8 --> RW3
```

### 2.4 Package Structure

All packages under `ssg.rwamp`:

| Package | Purpose |
|---------|---------|
| `ssg.rwamp.feature.session` | Session kill procedures |
| `ssg.rwamp.feature.testament` | Testament manager, add/flush procedures |
| `ssg.rwamp.feature.virtual` | Virtual session manager |
| `ssg.rwamp.feature.reflection` | Reflection registry, introspection |
| `ssg.rwamp.feature.statistics` | Call and message statistics counters |
| `ssg.rwamp.feature.rerouting` | Cross-realm call forwarding |
| `ssg.rwamp.feature.registration` | Pattern-based registration, revocation |
| `ssg.rwamp.rest` | REST over WAMP bridge |

---

## 3. lego-flow Changes (Implemented)

All 4 proposed changes have been implemented and committed to lego-flow.

| # | Change | Status | lego-flow Commit |
|---|--------|--------|-----------------|
| 1 | `SessionState` enum in `WampSession` | ✅ Merged | `7505aac` |
| 2 | `registerMetaProcedure()` in `WampRouter` | ✅ Merged | `7505aac` |
| 3 | `setTimeoutExecutor()` in `Dealer` | ✅ Merged | `7505aac` |
| 4 | `getActiveSessions()` in `Realm` | ✅ Merged | `7505aac` |

### 3.1 SessionState Enum

`WampSession` now uses `SessionState` enum (PENDING → ESTABLISHED → CLOSING → CLOSED)
instead of boolean `established`. Backward-compatible `isEstablished()` preserved.
New `getState()` accessor available.

### 3.2 Meta Procedure Registration

`WampRouter.registerMetaProcedure(name, handler)` and `unregisterMetaProcedure(name)`
allow external code to register custom meta procedures. Built-in procedures take
precedence. Handler signature: `(WampMessage.Call, WampTransport) → List<Object>`.

### 3.3 Call Timeout in Dealer

`Dealer.setTimeoutExecutor(ScheduledExecutorService)` enables optional call timeout
enforcement. When a Call includes `timeout` option (seconds), the Dealer schedules a
timer. On expiry, sends `wamp.error.timeout` to caller and INTERRUPT to callee.
Progressive results handling preserved.

### 3.4 Active Sessions Accessor

`Realm.getActiveSessions()` returns unmodifiable `Map<Long, WampSession>` snapshot.
Thread-safe copy, not live-linked to internal registry.

---

## 4. Dependency Management (following MDB-SQL patterns)

### 4.1 Maven

- Root POM declares `lego-flow.version` property
- Each module depends on `ssg:lego-flow-wamp:${lego-flow.version}`
- GitHub Packages repo: `https://maven.pkg.github.com/000ssg/lego-flow`
- Credentials from `GITHUB_ACTOR` / `GITHUB_TOKEN` env vars

### 4.2 Gradle

- `build.gradle.kts` references `property("legoFlowVersion")`
- GitHub Packages repo with credentials from env vars
- Uses `mavenLocal()` for local development override

### 4.3 Local Development

For local development where lego-flow is not published, install lego-flow to Maven
local first:

```bash
cd /path/to/lego-flow && mvn install -DskipTests
```

Both Maven and Gradle pick up from `~/.m2/repository`.

---

## 5. Phase Plans

Detailed per-phase plans with step-by-step implementation tracking:

| Phase | Document | Status |
|-------|----------|--------|
| Phase 0 — lego-flow changes | Section 3 above | ✅ Complete |
| Phase 1 — Foundation | [doc/plan/PHASE_1_Foundation.md](PHASE_1_Foundation.md) | ⬜ Pending |
| Phase 2 — Discovery & Identity | [doc/plan/PHASE_2_Discovery.md](PHASE_2_Discovery.md) | ⬜ Pending |
| Phase 3 — Integration | [doc/plan/PHASE_3_Integration.md](PHASE_3_Integration.md) | ⬜ Pending |
| Phase 4 — Polish | [doc/plan/PHASE_4_Polish.md](PHASE_4_Polish.md) | ⬜ Pending |

---

## 6. Overall Progress Tracking

### Repository Setup
| Step | Status |
|------|--------|
| Create `prototype` branch from `master` | ✅ Done |
| Write plan documents | ✅ Done |
| First commit with plan | ✅ Done |
| Create AGENTS.md | ✅ Done |

### lego-flow Changes
| Step | Status |
|------|--------|
| SessionState enum in WampSession | ✅ Merged (commit 7505aac) |
| Meta procedure registration in WampRouter | ✅ Merged (commit 7505aac) |
| Call timeout in Dealer | ✅ Merged (commit 7505aac) |
| getActiveSessions() in Realm | ✅ Merged (commit 7505aac) |

### Phase 1 — Foundation
| Milestone | Status |
|-----------|--------|
| Project scaffolding (POM, Gradle, dependency on lego-flow-wamp) | ⬜ Pending |
| Session Meta API (kill procedures) | ⬜ Pending |
| Statistics | ⬜ Pending |
| Phase 1 tests | ⬜ Pending |
| Phase 1 dual-build verification | ⬜ Pending |

### Phase 2 — Discovery & Identity
| Milestone | Status |
|-----------|--------|
| Reflection API | ⬜ Pending |
| Testament API | ⬜ Pending |
| Virtual Sessions | ⬜ Pending |
| Phase 2 tests | ⬜ Pending |
| Phase 2 dual-build verification | ⬜ Pending |

### Phase 3 — Integration
| Milestone | Status |
|-----------|--------|
| REST over WAMP bridge | ⬜ Pending |
| Call Rerouting | ⬜ Pending |
| Phase 3 tests | ⬜ Pending |
| Phase 3 dual-build verification | ⬜ Pending |

### Phase 4 — Polish
| Milestone | Status |
|-----------|--------|
| Pattern-based registration | ⬜ Pending |
| Registration revocation/meta | ⬜ Pending |
| Phase 4 tests | ⬜ Pending |
| Phase 4 dual-build verification | ⬜ Pending |
| Final documentation | ⬜ Pending |

---

## 7. Test Strategy

### Test Organization
- **Feature tests** — one test class per feature provider, using `InMemoryTransport` from lego-flow
- **Integration tests** — end-to-end flows using WebSocket transport from lego-flow
- **No duplicate tests for re-used components** — lego-flow tests cover the core

### Test Patterns (from lego-flow)
- `InMemoryTransport.createPair()` — paired transports for isolated testing
- AssertJ assertions — `assertThat(response).isInstanceOf(...)`
- Tests import lego-flow WAMP classes directly

---

## 8. Branch Strategy

| Branch | Purpose |
|--------|---------|
| `master` | Clean main branch; initial plan commit |
| `prototype` | All development; implementation commits |

---

## 9. References

- **WAMP_NEXT_STEPS.md** — Original comparison document
- **lego-flow WAMP** — `/Users/sergey.sidorov/work/projects/github/lego-flow/messaging/wamp` (basis)
- **xLib WAMP** — `/Users/sergey.sidorov/work/projects/github/xLib` (feature reference only)
- **lego-flow AGENTS.md** — Development practices adopted
- **MDB-SQL** — `/Users/sergey.sidorov/work/projects/github/MDB-SQL` (dependency pattern reference)

---

## 10. Effort Summary

| Item | Days |
|------|------|
| Phase 0 — lego-flow changes | 2.25 |
| Setup + scaffolding | 2 |
| Phase 1 — Foundation (RWAMP) | 4 |
| Phase 2 — Discovery & Identity | 13 |
| Phase 3 — Integration | 14 |
| Phase 4 — Polish | 5 |
| Buffer (15%) | 6 |
| **Total** | **46.25** |

**~46 working days (6-8 weeks at 1 person)**
