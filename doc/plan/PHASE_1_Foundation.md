# Phase 1 — Foundation

**Phase:** 1 of 4
**Status:** ✅ Complete
**Effort:** ~4 days

---

## Overview

Phase 1 establishes the RWAMP project infrastructure and implements the first set of
features that depend on the lego-flow extension points implemented in Phase 0.

## Prerequisites

- ✅ lego-flow changes merged (commit `7505aac`)
- ✅ lego-flow-wamp artifact available on GitHub Packages or in local Maven repo

---

## Step 1: Project Scaffolding

**Effort:** 0.5 day — ✅ Complete

### Tasks
- [x] Create root `pom.xml` (Maven aggregator)
  - groupId: `ssg`, artifactId: `rwamp`, version: `0.1.0-SNAPSHOT`
  - Property: `lego-flow.version = 0.2.0-SNAPSHOT`
  - GitHub Packages repo for lego-flow (with content filtering via Maven)
  - Java 25 toolchain, AssertJ 3.27+, JUnit 5.11+
- [x] Create root `build.gradle.kts`
  - groupId: `ssg`, version: `0.1.0-SNAPSHOT`
  - `legoFlowVersion` hardcoded to `0.2.0-SNAPSHOT`
  - GitHub Packages repo with `content { includeGroup("ssg") }` for credential safety
  - Java 25 toolchain, subprojects configuration
- [x] Create `settings.gradle.kts`
  - Include all feature modules
  - Unique project names matching artifact IDs
- [x] Create `gradle.properties`
  - Gradle performance settings
- [x] Verify dependency resolution works for both Maven and Gradle
- [x] Create Gradle wrapper (`gradle wrapper`)

### Key Design Decisions
- Maven POM uses `dependencyManagement` with GitHub Packages repository
- Gradle uses `content { includeGroup("ssg") }` to restrict GitHub Packages to lego-flow artifacts
- `junit-platform-launcher` version is `1.11.4` (not `5.11.4`) — separate versioning
- `legoFlowVersion` is a hardcoded constant in `build.gradle.kts` (consistent with MDB-SQL pattern)
- `InMemoryTransport` is created locally in each module's test sources (lego-flow's is in test scope)

---

## Step 2: Session Meta API (Kill Procedures)

**Effort:** 1.5 days — ✅ Complete
**Module:** `rwamp-feature-session`
**Tests:** 12 tests pass

### Feature Description

Implements WAMP session kill meta procedures as described in the WAMP Advanced Profile:
- `wamp.session.kill` — kill a single session by ID
- `wamp.session.killall` — kill all active sessions (excluding caller)
- `wamp.session.interrupt` — interrupt pending calls for a session

### Architecture

Uses lego-flow extension points:
- `WampRouter.registerMetaProcedure()` for procedure registration
- `SessionTransportTracker` for session→transport mapping (lego-flow doesn't track this)
- Sends GOODBYE to target session's transport on kill

### Files

| File | Purpose |
|------|---------|
| `SessionTransportTracker.java` | Maps session IDs to transports |
| `SessionMetaApi.java` | Kill procedure handlers + registration |
| `SessionTransportTrackerTest.java` | Tracker unit tests (5 tests) |
| `SessionMetaApiTest.java` | Kill procedure tests (7 tests) |
| `InMemoryTransport.java` | Test transport utility |

### Design Notes

- `SessionTransportTracker` is a standalone utility because `WampRouter` and `WampSession`
  do not track session-to-transport mappings
- The application must call `tracker.track(sessionId, transport)` when a session is created
  and `tracker.untrack(sessionId)` when closed
- `kill` sends GOODBYE to the target transport and returns kill details
- `killall` kills all tracked sessions (excluding the caller), returning list of killed IDs
- `interrupt` returns interrupt request details (best-effort, no actual Dealer integration)

---

## Step 3: Statistics

**Effort:** 1 day — ✅ Complete
**Module:** `rwamp-feature-statistics`
**Tests:** 12 tests pass

### Feature Description

Tracks WAMP call and message statistics as counters. Inspired by xLib's `WAMPStatistics`.

- Call counters: total calls, results, errors
- Pub/sub counters: publishes, events, subscribes, registers
- Raw message counters: messages_in, messages_out
- Per-realm granularity via named counter groups

### Files

| File | Purpose |
|------|---------|
| `WampStatistics.java` | Counter groups per realm |
| `WampStatistics.CounterGroup` | Thread-safe atomic counters |
| `StatisticsTransport.java` | Decorator wrapping WampTransport |
| `StatisticsApi.java` | `wamp.statistics.get` meta procedure |
| `WampStatisticsTest.java` | Counter operations (4 tests) |
| `StatisticsTransportTest.java` | Transport decorator (5 tests) |
| `StatisticsApiTest.java` | Meta procedure (3 tests) |

### Design Notes

- `WampStatistics` uses `ConcurrentHashMap` and `AtomicLong` for thread safety
- `StatisticsTransport` wraps any `WampTransport`, counting messages by type
- `StatisticsApi.register()` wires `wamp.statistics.get` to the router
- `snapshot()` returns `Map<String, Object>` for JSON-compatible output
- Supports realm filtering via `call.options()["realm"]`

---

## Progress Tracking

| Step | Status | Details |
|------|--------|---------|
| Project scaffolding | ✅ Complete | POM, Gradle, wrapper, AGENTS.md |
| Session Meta API | ✅ Complete | Kill procedures (12 tests) |
| Statistics | ✅ Complete | Counters + meta procedure (12 tests) |
| Tests | ✅ Complete | 24 tests total, all pass |
| Dual-build verification | ✅ Complete | Maven + Gradle, 24/24 pass |

---

## Phase Completion Criteria

- [x] All feature modules compile with both Maven and Gradle
- [x] All tests pass (Maven: `mvn test` — 24 tests)
- [x] All tests pass (Gradle: `./gradlew test` — 24 tests)
- [x] Dependency on `ssg:lego-flow-wamp` resolves correctly
- [x] Code follows lego-flow design patterns (records, AssertJ, package conventions)
- [x] Phase 1 tracking checkboxes updated to ✅ Complete

---

## Test Summary

| Module | Test Class | Tests |
|--------|-----------|-------|
| `rwamp-feature-session` | `SessionTransportTrackerTest` | 5 |
| `rwamp-feature-session` | `SessionMetaApiTest` | 7 |
| `rwamp-feature-statistics` | `WampStatisticsTest` | 4 |
| `rwamp-feature-statistics` | `StatisticsTransportTest` | 5 |
| `rwamp-feature-statistics` | `StatisticsApiTest` | 3 |
| **Total** | | **24** |
