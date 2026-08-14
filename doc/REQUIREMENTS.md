# RWAMP — Requirements Evolution

> Cross-references: [README](../README.md) | [Architecture](ARCHITECTURE.md) | [Code Overview](CODE_OVERVIEW.md)

---

## Module Timeline Overview

- **Start Date**: 2026-08-13
- **Total Tests**: 92
- **Version**: 0.1.0-SNAPSHOT
- **Purpose**: Production-grade WAMP v2 extensions built on lego-flow's WAMP implementation

---

## Table of Contents

- [Module Timeline Overview](#module-timeline-overview)
- **Commits by date (newest → oldest)**
  - [cd19f6b — CI: Gradle-only pipeline + JaCoCo coverage gate](#cd19f6b)
  - [9e3e173 — Phase 4: Pattern Registration & Meta API](#9e3e173)
  - [51dc9b2 — Phase 2 & 3: Reflection, Testament, Virtual, REST, Rerouting](#51dc9b2)
  - [a5d72ee — Phase 1: Foundation](#a5d72ee)

---

## Commit: `cd19f6b` — CI: Gradle-only pipeline + JaCoCo coverage gate (2026-08-14)

### Original Request
> "finalize planned actions, including pipeline for prototype branch... CI should be similar to lego-flow (ubuntu + macos + conditional for windows) and code coverage based on gradle, not like now (maven + gradle). ensure code coverage is at least 80%."

### Reformulated Requirements
1. Rewrite CI pipeline to be Gradle-only (remove Maven)
2. Match lego-flow CI pattern: ubuntu + macos matrix, conditional Windows
3. JaCoCo coverage gate with custom buildSrc tasks (Gradle 9.x has no built-in jacoco plugin)
4. Aggregate coverage threshold at 80%, per-module at 50%
5. Add tests to bring coverage from ~49% to ≥80% aggregate

### Final Design Decisions
- Custom `JacocoAggregateReportTask`, `JacocoCoverageVerificationTask`, `JacocoAggregateVerificationTask` in buildSrc (same approach as lego-flow)
- Per-module threshold at 50% (rerouting module has inherently hard-to-test blocking flow)
- Aggregate threshold at 80%
- CI jobs: build (ubuntu+macos), coverage gate, conditional Windows (opt-in via PR label)
- `skip-coverage` and `run-windows` PR labels for control

### Implementation Details
- `.github/workflows/ci.yml` — complete rewrite
- `buildSrc/` — 3 custom Gradle tasks for JaCoCo
- `build.gradle.kts` — JaCoCo agent wiring, aggregate tasks
- 14 new tests added across 3 test files

### Test Coverage
- `ReflectionRegistryTest.java` — 5 new tests (coverage: 77.0% → 87.4%)
- `ReroutingDealerTest.java` — 6 new tests (coverage: 48.9% → 59.1%)
- `CallReroutingTest.java` — 3 additions (coverage improved)
- Aggregate coverage: **86.9%** (621/715 lines)

### Cost Estimate
| Metric | Value |
|--------|-------|
| Files created/modified | 12 |
| Lines added/removed | +580 / -20 |
| Tests added | 14 (total: 92) |

---

## Commit: `9e3e173` — Phase 4: Pattern Registration & Meta API (2026-08-14)

### Original Request
> "continue with phase 2 and phase 3" (Phase 4 was part of the implementation plan)

### Reformulated Requirements
1. Pattern-based procedure registration (exact, prefix, wildcard matching)
2. Registration revocation meta procedure
3. Interceptor pattern for message filtering before the router

### Final Design Decisions
- `RegistrationInterceptor` sits between transport and router
- `PatternMatcher` supports exact, prefix, and single-segment wildcard matching
- `RegistrationHandler` processes REGISTER/UNREGISTER messages
- `RegistrationMetaApi` exposes `wamp.registration.list`, `wamp.registration.revoke`

### Implementation Details
- `PatternMatcher.java` — matching utility
- `PatternRegistry.java` — pattern storage
- `RegistrationHandler.java` — message processing
- `RegistrationInterceptor.java` — message filtering
- `RegistrationMetaApi.java` — meta procedure registration

### Test Coverage
- `PatternMatcherTest.java` — exact, prefix, wildcard matching
- `RegistrationHandlerTest.java` — register/unregister flows
- `RegistrationInterceptorTest.java` — end-to-end interception
- `RegistrationMetaApiTest.java` — meta procedure handling
- 31 tests in this module

### Cost Estimate
| Metric | Value |
|--------|-------|
| Files created | 10 |
| Tests added | 31 |

---

## Commit: `51dc9b2` — Phase 2 & 3: Reflection, Testament, Virtual, REST, Rerouting (2026-08-14)

### Original Request
> "continue with /Users/sergey.sidorov/work/projects/github/RWAMP phase 2 and phase 3"

### Reformulated Requirements
1. Reflection API — introspect procedures, topics, types
2. Testament API — schedule events on session close
3. Virtual sessions — identity mapping for HTTP users
4. REST over WAMP bridge — HTTP-to-WAMP translation
5. Call rerouting — cross-realm RPC forwarding

### Final Design Decisions
- Reflection: `ReflectionRegistry` mirrors Dealer/Broker state; 8 meta procedures
- Testament: `TestamentManager` stores per-session testaments; auto-publish on session leave
- Virtual: `VirtualSessionManager` allocates IDs and creates sessions with auth context
- REST: `RestWampBridge` composes virtual + reflection; maps paths to procedure URIs
- Rerouting: meta procedure with `RealmManager` lookup; forwards to target Dealer

### Implementation Details
- `rwamp-feature-reflection` — `ReflectionApi`, `ReflectionRegistry`
- `rwamp-feature-testament` — `TestamentApi`, `TestamentManager`
- `rwamp-feature-virtual` — `VirtualSessionApi`, `VirtualSessionManager`
- `rwamp-rest` — `RestWampBridge`, `RestRequest`, `RestResponse`
- `rwamp-feature-rerouting` — `ReroutingApi`, `ReroutingDealer`

### Test Coverage
- 19 tests for Phase 2 modules (reflection: 7, testament: 6, virtual: 6)
- 17 tests for Phase 3 modules (rest: 10, rerouting: 7)

### Cost Estimate
| Metric | Value |
|--------|-------|
| Files created | 25 |
| Tests added | 36 |

---

## Commit: `a5d72ee` — Phase 1: Foundation (2026-08-13)

### Original Request
> "RWAMP implementation plan — Phase 1 through Phase 4"

### Reformulated Requirements
1. Project scaffolding with dual build (Maven + Gradle)
2. Dependency on `ssg:lego-flow-wamp` via GitHub Packages
3. Session kill meta procedures (`wamp.session.kill`, `killall`, `interrupt`)
4. Statistics tracking (`wamp.statistics.get`) with transport counter wrapper

### Final Design Decisions
- Gradle Kotlin DSL as primary, Maven POMs as secondary
- All packages under `ssg.rwamp` (no conflict with `ssg.legoflow.wamp`)
- Each feature in its own module with independent compilation
- InMemoryTransport copied locally in each module's test sources
- AssertJ for assertions

### Implementation Details
- Root `pom.xml` and `build.gradle.kts` with dependency management
- `rwamp-feature-session` — `SessionMetaApi`, `SessionTransportTracker`
- `rwamp-feature-statistics` — `StatisticsApi`, `WampStatistics`, `StatisticsTransport`

### Test Coverage
- `SessionMetaApiTest.java` — kill, killall, interrupt flows (12 tests)
- `SessionTransportTrackerTest.java` — track/untrack (2 tests)
- `StatisticsApiTest.java` — statistics get (4 tests)
- `StatisticsTransportTest.java` — transport wrapping (4 tests)
- `WampStatisticsTest.java` — counter logic (4 tests)
- 24 tests total in Phase 1

### Cost Estimate
| Metric | Value |
|--------|-------|
| Files created | 15 |
| Tests added | 24 |

---

**Last Updated**: 2026-08-14
