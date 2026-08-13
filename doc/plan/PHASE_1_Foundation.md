# Phase 1 — Foundation

**RWAMP Phase 1:** Project scaffolding, Session Meta API (kill), Call Timeout, Statistics
**Effort:** 5 days (+ 2 days scaffolding)
**Dependencies:** lego-flow WAMP changes (pending approval)

---

## 1. Goals

After Phase 1, RWAMP has:
- A working Maven + Gradle project depending on `ssg:lego-flow-wamp`
- Session Meta API kill procedures (on top of lego-flow's WampRouter)
- Call timeout enforcement (on top of lego-flow's Dealer)
- Basic statistics tracking (on top of lego-flow's Broker + Dealer)
- Comprehensive test coverage for all Phase 1 features

---

## 2. Project Scaffolding

### Step 2.1: Repository Setup
| Task | Status |
|------|--------|
| Create `prototype` branch from `master` | ✅ Done |
| Write plan documents | ✅ Done |
| First commit with plan | ✅ Done |

### Step 2.2: Root POM + Gradle Settings
| Task | Status |
|------|--------|
| Root `pom.xml` — groupId `ssg`, aggregator, **depends on lego-flow-wamp** | ⬜ Pending |
| `settings.gradle.kts` — project names, module mapping | ⬜ Pending |
| Root `build.gradle.kts` — Java 25, test conventions, lego-flow dependency | ⬜ Pending |
| Shared dependency management (JUnit 5, AssertJ, Mockito, SLF4J, lego-flow-wamp) | ⬜ Pending |
| Verify: `mvn clean compile -DskipTests` | ⬜ Pending |
| Verify: `./gradlew clean classes` | ⬜ Pending |

**Note:** `lego-flow-wamp` is the only external WAMP dependency. All RWAMP modules
depend on it transitively through the root POM.

### Step 2.3: AGENTS.md
| Task | Status |
|------|--------|
| Adapt from lego-flow AGENTS.md for RWAMP | ⬜ Pending |
| Document dependency on lego-flow-wamp | ⬜ Pending |
| Document CI YAML bug (from lego-flow experience) | ⬜ Pending |
| Document dual-build verification protocol | ⬜ Pending |

### Step 2.4: README.md
| Task | Status |
|------|--------|
| Project overview — "RWAMP extends lego-flow's WAMP" | ⬜ Pending |
| Architecture diagram (Mermaid) — show lego-flow as dependency | ⬜ Pending |
| Module table with descriptions | ⬜ Pending |
| Build instructions (Maven + Gradle) | ⬜ Pending |

---

## 3. RWAMP Feature Modules (Phase 1)

**All modules depend on `ssg:lego-flow-wamp`.** No WAMP core classes are re-created.

### Step 3.1: rwamp-feature-session — Kill Procedures

**Package:** `ssg.rwamp.feature.session`

Re-uses lego-flow's `WampRouter` and `Realm`. Adds kill procedures as
meta procedure handlers registered with WampRouter.

| Task | Status |
|------|--------|
| `SessionMetaKillHandler` class — implements kill procedures | ⬜ Pending |
| `wamp.session.kill` — kill by session ID, sends GOODBYE | ⬜ Pending |
| `wamp.session.kill_by_authid` — kill all matching authid | ⬜ Pending |
| `wamp.session.kill_by_authrole` — kill all matching authrole | ⬜ Pending |
| `wamp.session.kill_all` — kill all sessions | ⬜ Pending |
| Registration with WampRouter (uses proposed meta procedure registration) | ⬜ Pending |
| **Fallback:** if lego-flow changes not approved, `SessionMetaRouter` wraps `WampRouter.route()` | ⬜ Pending |
| Test: `SessionMetaKillTest` — kill by session ID | ⬜ Pending |
| Test: `SessionMetaKillTest` — kill by authid | ⬜ Pending |
| Test: `SessionMetaKillTest` — kill by authrole | ⬜ Pending |
| Test: `SessionMetaKillTest` — kill_all | ⬜ Pending |
| Test: `SessionMetaKillTest` — no_such_session error | ⬜ Pending |

**Reference:** xLib `WAMP_FP_SessionMetaAPI.java` (procedure logic only)

**Key design decision:** Kill procedures are registered as meta procedure handlers.
If lego-flow's meta procedure registration is not approved, RWAMP provides a
`SessionMetaRouter` subclass that extends `WampRouter.route()` to intercept kill calls.

### Step 3.2: rwamp-feature-statistics — Statistics Tracking

**Package:** `ssg.rwamp.feature.statistics`

Re-uses lego-flow's `Broker` and `Dealer`. Wraps or hooks into message processing
to collect statistics.

| Task | Status |
|------|--------|
| `WampStatistics` — aggregate statistics holder (call + message counters) | ⬜ Pending |
| `CallStatistics` — call count, success, error, timeout, avg latency (record) | ⬜ Pending |
| `MessageStatistics` — per-message-type counters (record) | ⬜ Pending |
| `StatisticsRouter` — wraps WampRouter, counts messages routed | ⬜ Pending |
| `StatisticsBroker` — wraps Broker, counts publishes/subscribes | ⬜ Pending |
| `StatisticsDealer` — wraps Dealer, counts calls/invocations/yields | ⬜ Pending |
| `StatisticsProvider` — convenience class that wraps router + broker + dealer | ⬜ Pending |
| Test: `CallStatisticsTest` — call counts after operations | ⬜ Pending |
| Test: `MessageStatisticsTest` — per-type counters after operations | ⬜ Pending |
| Test: `StatisticsIntegrationTest` — end-to-end with wrapper | ⬜ Pending |

**Reference:** xLib `WAMPStatistics.java`, `WAMPCallStatistics.java`, `WAMPMessageStatistics.java`

**Key design decision:** Statistics uses a decorator/wrapper pattern around lego-flow's
Broker and Dealer. No changes to lego-flow needed — pure RWAMP composition.

### Step 3.3: Call Timeout

**Package:** `ssg.rwamp.feature.timeout`

| Task | Status |
|------|--------|
| `TimeoutDealer` — wraps lego-flow's `Dealer`, adds timeout tracking | ⬜ Pending |
| `timeout` option from CALL options parsed and applied | ⬜ Pending |
| `ScheduledExecutorService`-based timeout scheduling | ⬜ Pending |
| On timeout: send INTERRUPT to callee, return ERROR to caller | ⬜ Pending |
| Cancel timeout on normal yield (handler intercepts YIELD) | ⬜ Pending |
| **Fallback:** if lego-flow Dealer timeout change approved, `TimeoutDealer` delegates | ⬜ Pending |
| Test: `CallTimeoutTest` — timeout triggers INTERRUPT | ⬜ Pending |
| Test: `CallTimeoutTest` — no timeout when callee responds in time | ⬜ Pending |
| Test: `CallTimeoutTest` — timeout option absent → no timeout | ⬜ Pending |

**Reference:** xLib `WAMPRPCDealer.java` timeout handling

**Key design decision:** Timeout is implemented as a `TimeoutDealer` wrapper that
intercepts CALL and YIELD messages. If lego-flow's Dealer timeout change is approved,
the wrapper delegates directly.

---

## 4. Phase 1 Completion Checklist

- [ ] All modules compile with Maven (`mvn clean compile`)
- [ ] All modules compile with Gradle (`./gradlew clean classes`)
- [ ] All tests pass with Maven (`mvn clean test`)
- [ ] All tests pass with Gradle (`./gradlew clean test --rerun-tasks`)
- [ ] Kill procedures work correctly on top of lego-flow's WampRouter
- [ ] Statistics correctly track calls and messages
- [ ] Call timeout enforces deadline, sends INTERRUPT on expiry
- [ ] README.md reflects Phase 1 features
- [ ] No WAMP core classes duplicated from lego-flow

---

## 5. Test Count Targets

| Module | Target Tests |
|--------|-------------|
| rwamp-feature-session | 8+ |
| rwamp-feature-statistics | 5+ |
| rwamp-feature-timeout | 5+ |
| **Phase 1 Total** | **18+** |

---

## 6. Build Verification

| Check | Status |
|-------|--------|
| `lego-flow-wamp` dependency resolved | ⬜ Pending |
| No duplicate WAMP message/session/broker classes | ⬜ Pending |
| All RWAMP classes under `ssg.rwamp` package | ⬜ Pending |
| `mvn clean test` passes | ⬜ Pending |
| `./gradlew clean test --rerun-tasks` passes | ⬜ Pending |
