# Phase 1 — Foundation

**Phase:** 1 of 4
**Status:** ⬜ Pending
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

**Effort:** 0.5 day

### Tasks
- [ ] Create root `pom.xml` (Maven aggregator)
  - groupId: `ssg`, artifactId: `rwamp`, version: `0.1.0-SNAPSHOT`
  - Property: `lego-flow.version = 0.2.0-SNAPSHOT`
  - GitHub Packages repo for lego-flow
  - Java 25 toolchain, AssertJ 3.27+, JUnit 5.11+
- [ ] Create root `build.gradle.kts`
  - groupId: `ssg`, version: `0.1.0-SNAPSHOT`
  - `legoFlowVersion` property
  - GitHub Packages repo with credentials from env vars
  - Java 25 toolchain, subprojects configuration
- [ ] Create `settings.gradle.kts`
  - Include all feature modules
  - Unique project names matching artifact IDs
- [ ] Create `gradle.properties`
  - `legoFlowVersion=0.2.0-SNAPSHOT`
  - Test dependency versions
- [ ] Verify dependency resolution works for both Maven and Gradle

### Reference (from MDB-SQL)

Maven root POM follows `MDB-SQL/pom.xml` pattern:
- `dependencyManagement` section with `ssg:lego-flow-wamp` versioned
- GitHub Packages repository with authentication
- Common test dependencies (JUnit, AssertJ, SLF4J)

Gradle root build.gradle.kts follows `MDB-SQL/build.gradle.kts` pattern:
- `subprojects` block with shared configuration
- GitHub Packages repo with `GITHUB_ACTOR`/`GITHUB_TOKEN` credentials
- `mavenLocal()` for local lego-flow override

---

## Step 2: Session Meta API (Kill Procedures)

**Effort:** 1.5 days
**Module:** `rwamp-feature-session`

### Feature Description

Implements WAMP session kill meta procedures as described in the WAMP Advanced Profile:
- `wamp.session.kill` — kill a session by ID
- `wamp.session.killall` — kill sessions matching criteria
- `wamp.session.interrupt` — interrupt pending calls for a session

Uses lego-flow extension points:
- `WampRouter.registerMetaProcedure()` for procedure registration
- `Realm.getActiveSessions()` for session iteration
- `WampSession.getState()` for state-based filtering

### Implementation

- [ ] `SessionMetaApi.java` — kill procedure handlers
  - Uses `registerMetaProcedure("wamp.session.kill", ...)`
  - Accesses realm's `getActiveSessions()` to find target
  - Sends GOODBYE to target session
  - Returns kill confirmation details
- [ ] `SessionKillHandler.java` — kill logic with reason handling
  - Processes `wamp.session.kill` with session ID and reason
  - Processes `wamp.session.killall` with authid/authrole filters
  - Uses SessionState to skip CLOSING/CLOSED sessions
- [ ] Register procedures on router during initialization

### Tests

- [ ] `SessionMetaApiTest.java` — kill single session
- [ ] `SessionKillAllTest.java` — kill by authid/authrole
- [ ] `SessionKillIntegrationTest.java` — end-to-end kill flow

---

## Step 3: Statistics

**Effort:** 1 day
**Module:** `rwamp-feature-statistics`

### Feature Description

Tracks WAMP call and message statistics as counters. Inspired by xLib's `WAMPStatistics`.

- Call counters: total, success, timeout, error
- Message counters: publish, subscribe, register
- Per-realm and per-session granularity

### Implementation

- [ ] `WampStatistics.java` — statistics counters (ConcurrentHashMap-based)
- [ ] `StatisticsTransport.java` — decorator wrapping `WampTransport` to count messages
- [ ] `wamp.statistics.get` meta procedure via `registerMetaProcedure()`

### Tests

- [ ] `WampStatisticsTest.java` — counter operations
- [ ] `StatisticsTransportTest.java` — message counting
- [ ] `StatisticsMetaProcedureTest.java` — statistics query

---

## Progress Tracking

| Step | Status | Details |
|------|--------|---------|
| Project scaffolding | ⬜ Pending | POM, Gradle, AGENTS.md |
| Session Meta API | ⬜ Pending | Kill procedures |
| Statistics | ⬜ Pending | Counters + meta procedure |
| Tests | ⬜ Pending | All feature tests |
| Dual-build verification | ⬜ Pending | Maven + Gradle test |

---

## Phase Completion Criteria

- [ ] All feature modules compile with both Maven and Gradle
- [ ] All tests pass (Maven: `mvn test`)
- [ ] All tests pass (Gradle: `./gradlew test`)
- [ ] Dependency on `ssg:lego-flow-wamp` resolves correctly
- [ ] Code follows lego-flow design patterns (sealed interfaces, records, AssertJ)
- [ ] Phase 1 tracking checkboxes updated to ✅ Complete
