# RWAMP — Detailed Implementation Plan

**Project:** RWAMP — Extended WAMP v2 implementation for Java 25+
**Date:** 2026-08-13
**Status: In Progress (Phase 1, 2, 3 complete)**

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

| Feature | Source Inspiration | Module | Status |
|---------|-------------------|--------|--------|
| Session kill procedures (`wamp.session.kill*`) | xLib `WAMP_FP_SessionMetaAPI` | `rwamp-feature-session` | ✅ Done |
| Testament API (`wamp.session.add_testament`) | xLib `WAMP_FP_TestamentMetaAPI` | `rwamp-feature-testament` | ✅ Phase 2 | |
| Virtual sessions (`virtual_session.register`) | xLib `WAMP_FP_VirtualSession` | `rwamp-feature-virtual` | ✅ Phase 2 | |
| Reflection API (`wamp.reflection.*`) | xLib `WAMP_FP_Reflection` | `rwamp-feature-reflection` | ✅ Phase 2 | |
| Statistics tracking | xLib `WAMPStatistics` | `rwamp-feature-statistics` | ✅ Done |
| REST over WAMP bridge | xLib `REST_WAMP_MethodsProvider` | `rwamp-rest` | ✅ Phase 3 | |
| Call rerouting | xLib `WAMPRPCDealer` rerouting | `rwamp-feature-rerouting` | ✅ Phase 3 | |
| Pattern-based registration | xLib `WAMPRPCDealer` pattern | `rwamp-feature-registration` | ⬜ Phase 4 |

### 2.3 Module Structure

```mermaid
graph TD
    subgraph "lego-flow (GitHub Packages dependency)"
        LF1["ssg:lego-flow-wamp<br/>core: messages, session, broker, dealer, router,<br/>realm, serialization, auth, websocket"]
    end

    subgraph "RWAMP (extension project)"
        RW1["rwamp-feature-session<br/>kill procedures, state machine<br/>✅ Phase 1"]
        RW2["rwamp-feature-testament<br/>testament scheduling + lifecycle hooks<br/>⬜ Phase 2"]
        RW3["rwamp-feature-virtual<br/>virtual session manager<br/>⬜ Phase 2"]
        RW4["rwamp-feature-reflection<br/>procedure/topic/type introspection<br/>⬜ Phase 2"]
        RW5["rwamp-feature-statistics<br/>call + message counters<br/>✅ Phase 1"]
        RW6["rwamp-feature-rerouting<br/>cross-realm call forwarding<br/>⬜ Phase 3"]
        RW7["rwamp-feature-registration<br/>pattern matching + revocation<br/>⬜ Phase 4"]
        RW8["rwamp-rest<br/>REST over WAMP bridge<br/>⬜ Phase 3"]
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

| Package | Purpose | Status |
|---------|---------|--------|
| `ssg.rwamp.feature.session` | Session kill procedures | ✅ Done |
| `ssg.rwamp.feature.testament` | Testament manager, add/flush procedures | ⬜ Phase 2 |
| `ssg.rwamp.feature.virtual` | Virtual session manager | ⬜ Phase 2 |
| `ssg.rwamp.feature.reflection` | Reflection registry, introspection | ⬜ Phase 2 |
| `ssg.rwamp.feature.statistics` | Call and message statistics counters | ✅ Done |
| `ssg.rwamp.feature.rerouting` | Cross-realm call forwarding | ⬜ Phase 3 |
| `ssg.rwamp.feature.registration` | Pattern-based registration, revocation | ⬜ Phase 4 |
| `ssg.rwamp.rest` | REST over WAMP bridge | xLib `REST_WAMP_MethodsProvider` | `rwamp-rest` | ✅ Phase 3 | |

---

## 3. lego-flow Changes (Phase 0 — ✅ Complete)

All changes committed to lego-flow `master` as commit `7505aac` and merged.

### 3.1 SessionState Enum

Replaced `boolean established` in `WampSession` with `SessionState` enum:
PENDING → ESTABLISHED → CLOSING → CLOSED. Backward-compatible `isEstablished()`
preserved. New `getState()` accessor.

### 3.2 Meta Procedure Registration

`WampRouter.registerMetaProcedure(name, handler)` and `unregisterMetaProcedure(name)`.
`BiFunction<WampMessage.Call, WampTransport, List<Object>>` handler signature.
Built-in procedures take precedence (checked via switch before custom handlers).

### 3.3 Call Timeout

`Dealer.setTimeoutExecutor(ScheduledExecutorService)` enables optional call timeout
enforcement. When a Call includes `timeout` option (seconds), the Dealer schedules a
timer. On expiry: `wamp.error.timeout` to caller + INTERRUPT to callee.
Progressive results handling preserved.

### 3.4 Active Sessions Accessor

`Realm.getActiveSessions()` returns unmodifiable `Map<Long, WampSession>` snapshot.
Thread-safe copy, not live-linked to internal registry.

---

## 4. Dependency Management

### 4.1 Maven

- Root POM declares `lego-flow.version` property
- Each module depends on `ssg:lego-flow-wamp:${lego-flow.version}`
- GitHub Packages repo: `https://maven.pkg.github.com/000ssg/lego-flow`
- Credentials from `GITHUB_ACTOR` / `GITHUB_TOKEN` env vars
- `mavenLocal()` for local development override

### 4.2 Gradle

- `build.gradle.kts` with hardcoded `legoFlowVersion = "0.2.0-SNAPSHOT"`
- GitHub Packages repo with `content { includeGroup("ssg") }` to avoid auth issues
- `mavenLocal()` for local lego-flow override
- `junitPlatformVersion = "1.11.4"` (separate from junitVersion)

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

| Phase | Document | Status | Tests |
|-------|----------|--------|-------|
| Phase 0 — lego-flow changes | Section 3 above | ✅ Complete | 21 tests (lego-flow) |
| Phase 1 — Foundation | [doc/plan/PHASE_1_Foundation.md](PHASE_1_Foundation.md) | ✅ Complete | 24 tests |
| Phase 2 — Discovery | Phase 2 — Discovery & Identity | [doc/plan/PHASE_2_Discovery.md](PHASE_2_Discovery.md) | ⬜ Pending | — Identity | [doc/plan/PHASE_2_Discovery.md](PHASE_2_Discovery.md) | ✅ Complete | 19 tests | |
| Phase 3 — Integration | [doc/plan/PHASE_3_Integration.md](PHASE_3_Integration.md) | ✅ Complete | 17 tests | |
| Phase 4 — Polish | [doc/plan/PHASE_4_Polish.md](PHASE_4_Polish.md) | ⬜ Pending | — |

---

## 6. Overall Progress Tracking

### Repository Setup
| Step | Status |
|------|--------|
| Create `prototype` branch from `master` | ✅ Done |
| Write plan documents | ✅ Done |
| First commit with plan | ✅ Done |
| Create AGENTS.md | ✅ Done |

### lego-flow Changes (Phase 0)
| Step | Status |
|------|--------|
| SessionState enum in WampSession | ✅ Merged (commit 7505aac) |
| Meta procedure registration in WampRouter | ✅ Merged (commit 7505aac) |
| Call timeout in Dealer | ✅ Merged (commit 7505aac) |
| getActiveSessions() in Realm | ✅ Merged (commit 7505aac) |

### Phase 1 — Foundation
| Milestone | Status |
|-----------|--------|
| Project scaffolding (POM, Gradle, dependency on lego-flow-wamp) | ✅ Done |
| Session Meta API (kill procedures) | ✅ Done |
| Statistics | ✅ Done |
| Phase 1 tests | ✅ Done (19 tests)) |
| Phase 1 dual-build verification | ✅ Done (Maven + Gradle) |

### Phase 2 — Discovery & Identity
| Milestone | Status |
|-----------|--------|
| Reflection API | ✅ Done |
| Testament API | ✅ Done |
| Virtual Sessions | ✅ Done |
| Phase 2 tests | ✅ Done (19 tests) |
| Phase 2 dual-build verification | ✅ Done (Maven + Gradle) |

### Phase 3 — Integration
| Milestone | Status |
|-----------|--------|
| REST over WAMP bridge | ✅ Done |
| Call Rerouting | ✅ Done |
| Phase 3 tests | ✅ Done (17 tests) |
| Phase 3 dual-build verification | ✅ Done (Maven + Gradle) |

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
- **Feature tests** — one test class per feature provider, using `InMemoryTransport`
- **Integration tests** — end-to-end flows using WebSocket transport from lego-flow
- **No duplicate tests for re-used components** — lego-flow tests cover the core

### Test Patterns (from lego-flow)
- `InMemoryTransport.createPair()` — paired transports for isolated testing
- AssertJ assertions — `assertThat(response).isInstanceOf(...)`
- Tests import lego-flow WAMP classes directly
- Each module has its own `InMemoryTransport` in test sources

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
| Phase 1 — Setup + Foundation | 4 |
| Phase 2 — Discovery & Identity | 13 |
| Phase 3 — Integration | 14 |
| Phase 4 — Polish | 5 |
| Buffer (15%) | 6 |
| **Total** | **44.25** |

**~44 working days (6-8 weeks at 1 person)**

### Cumulative Test Counts

| Phase | Tests | Cumulative |
|-------|-------|------------|
| Phase 0 (lego-flow) | 21 | 21 |
| Phase 1 (RWAMP) | 19 | 40 | |
| Phase 2 (RWAMP) | 19 | 59 |+ |
| Phase 3 (RWAMP) | 17 | 76 |+ |
| Phase 4 (planned) | 8+ | 90+ |
