# Phase 1 — Foundation

**RWAMP Phase 1:** Session Meta API, Call Timeout, Statistics, Session State Machine
**Effort:** 6 days (+ 2 days scaffolding)
**Dependencies:** None

---

## 1. Goals

After Phase 1, RWAMP has:
- A working Maven + Gradle project structure
- Core protocol module with messages, session, serialization, transport
- Router module with Broker, Dealer, Realm, RealmManager
- Client module with all four roles
- Session state machine (4 states)
- Session Meta API including kill procedures
- Call timeout enforcement in Dealer
- Basic statistics tracking

---

## 2. Project Scaffolding

### Step 1.1: Repository Setup
| Task | Status |
|------|--------|
| Create `prototype` branch from `master` | ⬜ Pending |
| Create `doc/plan/` directory structure | ⬜ Pending |
| Write plan documents | ⬜ Pending |
| First commit with plan on `prototype` | ⬜ Pending |

### Step 1.2: Root POM + Gradle Settings
| Task | Status |
|------|--------|
| Root `pom.xml` — groupId `ssg`, aggregator with all modules | ⬜ Pending |
| `settings.gradle.kts` — project names, module mapping | ⬜ Pending |
| Root `build.gradle.kts` — Java 25, test conventions | ⬜ Pending |
| Shared dependency management (JUnit 5, AssertJ, Mockito, SLF4J) | ⬜ Pending |
| Verify: `mvn clean compile -DskipTests` | ⬜ Pending |
| Verify: `./gradlew clean classes` | ⬜ Pending |

### Step 1.3: AGENTS.md
| Task | Status |
|------|--------|
| Adapt from lego-flow AGENTS.md for RWAMP | ⬜ Pending |
| Adjust module names, build commands | ⬜ Pending |
| Document CI YAML bug (lego-flow experience) | ⬜ Pending |
| Document dual-build verification protocol | ⬜ Pending |

### Step 1.4: README.md
| Task | Status |
|------|--------|
| Project overview, architecture diagram (Mermaid) | ⬜ Pending |
| Module table with descriptions | ⬜ Pending |
| Quick start instructions | ⬜ Pending |
| Build instructions (Maven + Gradle) | ⬜ Pending |

---

## 3. rwamp-core Module

**Package:** `ssg.rwamp.core`

### Step 3.1: WampMessage (sealed interface)
| Task | Status |
|------|--------|
| `WampMessageType` enum with all 20+ message types and code mapping | ⬜ Pending |
| `WampMessage` sealed interface with `type()` method | ⬜ Pending |
| Session records: `Hello`, `Welcome`, `Abort`, `Goodbye` | ⬜ Pending |
| Auth records: `Challenge`, `Authenticate` | ⬜ Pending |
| Error record: `Error` | ⬜ Pending |
| Pub/Sub records: `Publish`, `Published`, `Subscribe`, `Subscribed`, `Unsubscribe`, `Unsubscribed`, `Event` | ⬜ Pending |
| RPC records: `Call`, `Cancel`, `Result`, `Register`, `Registered`, `Unregister`, `Unregistered`, `Invocation`, `Interrupt`, `Yield` | ⬜ Pending |
| Test: `WampMessageTypeTest` — verify all codes unique | ⬜ Pending |
| Test: `WampMessageTest` — verify all records implement sealed interface | ⬜ Pending |

**Reference:** `lego-flow WampMessage.java` — direct pattern, use same record structure

### Step 3.2: WampSession with State Machine
| Task | Status |
|------|--------|
| `WampSessionState` enum: `OPENING`, `ESTABLISHED`, `CLOSING`, `CLOSED` | ⬜ Pending |
| `WampSession` class with state transitions | ⬜ Pending |
| Session properties: id, realm, authId, authRole, authMethod, state | ⬜ Pending |
| Subscription/registration tracking (ConcurrentHashMap) | ⬜ Pending |
| Lifecycle methods: `establish()`, `close()`, `setState()` | ⬜ Pending |
| State transition validation (illegal transitions throw exception) | ⬜ Pending |
| Test: `WampSessionTest` — state transitions | ⬜ Pending |
| Test: `WampSessionTest` — subscription/registration tracking | ⬜ Pending |

**Reference:** xLib `WAMPSessionState.java` + `WAMPSessionImpl.java` for state machine logic

### Step 3.3: Serialization
| Task | Status |
|------|--------|
| `WampSerializer` interface: `encode()`, `decode()` | ⬜ Pending |
| JSON serializer (Jackson) | ⬜ Pending |
| MessagePack serializer | ⬜ Pending |
| CBOR serializer | ⬜ Pending |
| `WampSerializerFactory` — factory by serialization ID | ⬜ Pending |
| Test: `WampSerializerTest` — JSON round-trip | ⬜ Pending |
| Test: `WampSerializerTest` — MessagePack round-trip | ⬜ Pending |
| Test: `WampSerializerTest` — CBOR round-trip | ⬜ Pending |
| Test: `WampSerializerFactoryTest` — factory lookup | ⬜ Pending |

**Reference:** lego-flow `serialization/` package — adapt CBOR and MessagePack

### Step 3.4: Transport Interface
| Task | Status |
|------|--------|
| `WampTransport` interface: `send()`, `close()` | ⬜ Pending |
| `InMemoryTransport` implementation for testing (pair-based) | ⬜ Pending |
| Test: `InMemoryTransportTest` — pair communication | ⬜ Pending |

**Reference:** lego-flow `InMemoryTransport.java`

---

## 4. rwamp-router Module

**Package:** `ssg.rwamp.router`

### Step 4.1: Broker
| Task | Status |
|------|--------|
| `Broker` class with exact, prefix, wildcard subscriptions | ⬜ Pending |
| `handleSubscribe()` with match policy support | ⬜ Pending |
| `handleUnsubscribe()` | ⬜ Pending |
| `handlePublish()` with exclude_me, disclose_me, exclude, eligible, retain | ⬜ Pending |
| Event retention (retained events per topic) | ⬜ Pending |
| Test: `BrokerTest` — basic subscribe/publish | ⬜ Pending |
| Test: `AdvancedBrokerTest` — pattern matching, exclusion, eligibility | ⬜ Pending |
| Test: `EventRetentionTest` — retained event delivery | ⬜ Pending |

**Reference:** lego-flow `Broker.java` — direct adaptation

### Step 4.2: Dealer
| Task | Status |
|------|--------|
| `Dealer` class with registration management | ⬜ Pending |
| `handleRegister()` with invoke policy (single, first, last, roundrobin, random) | ⬜ Pending |
| `handleUnregister()` | ⬜ Pending |
| `handleCall()` with disclose_me, receive_progress | ⬜ Pending |
| `handleYield()` — result delivery | ⬜ Pending |
| `handleCancel()` — skip, kill, killnowait | ⬜ Pending |
| Test: `DealerTest` — basic register/call | ⬜ Pending |
| Test: `AdvancedDealerTest` — shared registrations, progressive results, cancellation | ⬜ Pending |

**Reference:** lego-flow `Dealer.java` — direct adaptation

### Step 4.3: Realm + RealmManager
| Task | Status |
|------|--------|
| `Realm` with embedded Broker, Dealer, session map, session ID counter | ⬜ Pending |
| `RealmManager` with create, get, remove, list realms | ⬜ Pending |
| Test: `RealmTest` — session management | ⬜ Pending |
| Test: `RealmManagerTest` — multi-realm operations | ⬜ Pending |

**Reference:** lego-flow `Realm.java`, `RealmManager.java`

### Step 4.4: Session Meta API (Kill Procedures)
| Task | Status |
|------|--------|
| `wamp.session.count` — session count | ⬜ Pending |
| `wamp.session.list` — list session IDs | ⬜ Pending |
| `wamp.session.get` — get session details | ⬜ Pending |
| `wamp.session.kill` — kill by session ID | ⬜ Pending |
| `wamp.session.kill_by_authid` — kill by auth ID | ⬜ Pending |
| `wamp.session.kill_by_authrole` — kill by auth role | ⬜ Pending |
| `wamp.session.kill_all` — kill all sessions | ⬜ Pending |
| `wamp.session.on_join` — publish on session join | ⬜ Pending |
| `wamp.session.on_leave` — publish on session leave | ⬜ Pending |
| Test: `SessionMetaAPITest` — count, list, get | ⬜ Pending |
| Test: `SessionMetaKillTest` — kill, kill_by_authid, kill_by_authrole, kill_all | ⬜ Pending |

**Reference:** xLib `WAMP_FP_SessionMetaAPI.java` — implement as local procedures in Dealer

### Step 4.5: Call Timeout
| Task | Status |
|------|--------|
| `ScheduledExecutorService` for timeout tracking in Dealer | ⬜ Pending |
| `timeout` option from CALL options | ⬜ Pending |
| Send INTERRUPT to callee on timeout | ⬜ Pending |
| Return ERROR to caller on timeout | ⬜ Pending |
| Cancel timeout on normal yield | ⬜ Pending |
| Test: `CallTimeoutTest` — timeout triggers INTERRUPT | ⬜ Pending |
| Test: `CallTimeoutTest` — no timeout when callee responds in time | ⬜ Pending |

**Reference:** xLib `WAMPRPCDealer.java` timeout handling

### Step 4.6: Statistics
| Task | Status |
|------|--------|
| `WampStatistics` — aggregate statistics holder | ⬜ Pending |
| `CallStatistics` — call count, success, error, timeout, avg latency | ⬜ Pending |
| `MessageStatistics` — per-message-type counters | ⬜ Pending |
| Hook statistics into Broker (publish counts) | ⬜ Pending |
| Hook statistics into Dealer (call counts, latency) | ⬜ Pending |
| Test: `CallStatisticsTest` — verify counters | ⬜ Pending |
| Test: `MessageStatisticsTest` — verify per-type counters | ⬜ Pending |

**Reference:** xLib `WAMPStatistics.java`, `WAMPCallStatistics.java`, `WAMPMessageStatistics.java`

---

## 5. rwamp-client Module

**Package:** `ssg.rwamp.client`

### Step 5.1: Caller
| Task | Status |
|------|--------|
| `Caller` with `call()` returning `CompletableFuture` | ⬜ Pending |
| `call()` with options (disclose_me, receive_progress, timeout) | ⬜ Pending |
| `cancel()` — cancel pending calls | ⬜ Pending |
| Result/error handling via pending calls map | ⬜ Pending |
| Test: `CallerTest` — basic call, result, error | ⬜ Pending |
| Test: `CallerTest` — progressive results | ⬜ Pending |

**Reference:** lego-flow `Caller.java`

### Step 5.2: Callee
| Task | Status |
|------|--------|
| `Callee` with `register()` returning `CompletableFuture` | ⬜ Pending |
| `unregister()` | ⬜ Pending |
| Invocation handler (functional interface or callback) | ⬜ Pending |
| Yield result / error | ⬜ Pending |
| Test: `CalleeTest` — register, invoke, yield | ⬜ Pending |

**Reference:** lego-flow `Callee.java`

### Step 5.3: Publisher
| Task | Status |
|------|--------|
| `Publisher` with `publish()` | ⬜ Pending |
| Options: exclude_me, disclose_me, retain | ⬜ Pending |
| Test: `PublisherTest` — basic publish | ⬜ Pending |

**Reference:** lego-flow `Publisher.java`

### Step 5.4: Subscriber
| Task | Status |
|------|--------|
| `Subscriber` with `subscribe()` returning subscription ID | ⬜ Pending |
| `unsubscribe()` | ⬜ Pending |
| Event handler (Consumer) | ⬜ Pending |
| Test: `SubscriberTest` — subscribe, receive events | ⬜ Pending |

**Reference:** lego-flow `Subscriber.java`

### Step 5.5: WampClient
| Task | Status |
|------|--------|
| `WampClient` — entry point combining all roles + transport + session lifecycle | ⬜ Pending |
| Session lifecycle: `connect()`, `join()`, `leave()`, `close()` | ⬜ Pending |
| Auth method configuration | ⬜ Pending |
| Test: `WampClientTest` — full lifecycle | ⬜ Pending |

---

## 6. Phase 1 Completion Checklist

- [ ] All modules compile with Maven (`mvn clean compile`)
- [ ] All modules compile with Gradle (`./gradlew clean classes`)
- [ ] All tests pass with Maven (`mvn clean test`)
- [ ] All tests pass with Gradle (`./gradlew clean test --rerun-tasks`)
- [ ] README.md reflects Phase 1 features
- [ ] AGENTS.md build commands verified
- [ ] Architecture diagram updated in README.md

---

## 7. Test Count Targets

| Module | Target Tests |
|--------|-------------|
| rwamp-core | 25+ |
| rwamp-router | 35+ |
| rwamp-client | 20+ |
| **Phase 1 Total** | **80+** |

---

## 8. Dependencies & Build Verification

| Check | Status |
|-------|--------|
| No module references `ssg.legoflow` groupId | ⬜ Pending |
| All modules declare JUnit + SLF4J test dependencies | ⬜ Pending |
| `mvn clean test` passes | ⬜ Pending |
| `./gradlew clean test --rerun-tasks` passes | ⬜ Pending |
