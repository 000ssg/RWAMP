# Phase 2 — Discovery & Identity

**RWAMP Phase 2:** Reflection API, Testament API, Virtual Sessions
**Effort:** 13 days
**Dependencies:** Phase 1 complete

---

## Dependency Note

All modules in this phase depend on `ssg:lego-flow-wamp` via GitHub Packages.
Follow the pattern established in Phase 1 (see PHASE_1_Foundation.md Step 1).

## 1. Goals

After Phase 2, RWAMP has:
- **Reflection API** — introspect procedures, topics, types, errors via WAMP RPC
- **Testament API** — schedule events on session detach/destroy
- **Virtual Sessions** — create virtual WAMP sessions from HTTP-authenticated users

All features built on top of lego-flow's `Broker`, `Dealer`, `Realm`, and `WampRouter`.

---

## Dependency Note

All modules in this phase depend on `ssg:lego-flow-wamp` via GitHub Packages.
Follow the pattern established in Phase 1 (see PHASE_1_Foundation.md Step 1).

## 2. Reflection API

**Package:** `ssg.rwamp.feature.reflection`

Re-uses lego-flow's `Dealer` and `Broker`. Reflection procedures are registered
as local procedures with the Dealer via meta procedure registration.

### 2.1 Data Model
| Task | Status |
|---

## Dependency Note

All modules in this phase depend on `ssg:lego-flow-wamp` via GitHub Packages.
Follow the pattern established in Phase 1 (see PHASE_1_Foundation.md Step 1).---|--------|
| `ReflectionRegistry` — per-realm registry for types, procedures, errors, topics | ⬜ Pending |
| `ReflectionRegistry` reads from lego-flow's `Broker` (subscriptions) and `Dealer` (registrations) | ⬜ Pending |
| `ReflectionRegistry` scoped by realm name | ⬜ Pending |

### 2.2 Procedures (registered as meta/local procedures)
| Task | Status |
|---

## Dependency Note

All modules in this phase depend on `ssg:lego-flow-wamp` via GitHub Packages.
Follow the pattern established in Phase 1 (see PHASE_1_Foundation.md Step 1).---|--------|
| `wamp.reflection.topic.list` — list topics with subscribers | ⬜ Pending |
| `wamp.reflection.topic.describe` — describe topics (subscriber count, retained events) | ⬜ Pending |
| `wamp.reflection.procedure.list` — list registered procedures | ⬜ Pending |
| `wamp.reflection.procedure.describe` — describe procedures (callee count, invoke policy) | ⬜ Pending |
| `wamp.reflection.error.list` — list known errors | ⬜ Pending |
| `wamp.reflection.type.list` — list type definitions | ⬜ Pending |
| `wamp.reflection.type.describe` — describe types | ⬜ Pending |

### 2.3 Event-Driven Reflection
| Task | Status |
|---

## Dependency Note

All modules in this phase depend on `ssg:lego-flow-wamp` via GitHub Packages.
Follow the pattern established in Phase 1 (see PHASE_1_Foundation.md Step 1).---|--------|
| `wamp.reflect.define` topic handler — register type/procedure/error definitions | ⬜ Pending |
| `wamp.reflect.describe` topic handler — request definitions | ⬜ Pending |
| `wamp.reflect.on_define` event — published when something is defined | ⬜ Pending |
| `wamp.reflect.on_undefine` event — published when something is undefined | ⬜ Pending |
| `ReflectionInterceptor` — intercepts REGISTER/PUBLISH via wrapper, captures metadata | ⬜ Pending |

### 2.4 Tests
| Task | Status |
|---

## Dependency Note

All modules in this phase depend on `ssg:lego-flow-wamp` via GitHub Packages.
Follow the pattern established in Phase 1 (see PHASE_1_Foundation.md Step 1).---|--------|
| `ReflectionAPITest` — topic.list/topic.describe | ⬜ Pending |
| `ReflectionAPITest` — procedure.list/procedure.describe | ⬜ Pending |
| `ReflectionAPITest` — type.list/type.describe | ⬜ Pending |
| `ReflectionEventTest` — define/describe topics | ⬜ Pending |
| `ReflectionAutoCaptureTest` — auto-capture from REGISTER with reflection options | ⬜ Pending |

**Reference:** xLib `WAMP_FP_Reflection.java`

**Key design decision:** Reflection reads live data from lego-flow's Broker and Dealer.
No separate state tracking — queries Broker's subscription maps and Dealer's registration
maps at call time.

---

## Dependency Note

All modules in this phase depend on `ssg:lego-flow-wamp` via GitHub Packages.
Follow the pattern established in Phase 1 (see PHASE_1_Foundation.md Step 1).

## 3. Testament API

**Package:** `ssg.rwamp.feature.testament`

Re-uses lego-flow's `WampSession` and `Broker`. Testament publishing hooks into
session close lifecycle.

### 3.1 Core Implementation
| Task | Status |
|---

## Dependency Note

All modules in this phase depend on `ssg:lego-flow-wamp` via GitHub Packages.
Follow the pattern established in Phase 1 (see PHASE_1_Foundation.md Step 1).---|--------|
| `TestamentManager` — stores testaments per session (keyed by session ID) | ⬜ Pending |
| Testament data: topic, args, kwargs, publish_options | ⬜ Pending |
| Testament scope: `detached` vs `destroyed` | ⬜ Pending |
| `wamp.session.add_testament` procedure (registered as meta procedure) | ⬜ Pending |
| `wamp.session.flush_testament` procedure (registered as meta procedure) | ⬜ Pending |
| Testament auto-publish on session close (calls Broker.handlePublish) | ⬜ Pending |
| `TestamentSessionListener` — hooks into `sessionLeft()` to trigger testament publish | ⬜ Pending |

### 3.2 Tests
| Task | Status |
|---

## Dependency Note

All modules in this phase depend on `ssg:lego-flow-wamp` via GitHub Packages.
Follow the pattern established in Phase 1 (see PHASE_1_Foundation.md Step 1).---|--------|
| `TestamentTest` — add_testament, verify stored | ⬜ Pending |
| `TestamentTest` — flush_testament, verify removed | ⬜ Pending |
| `TestamentTest` — scope destroyed, publish on session close | ⬜ Pending |
| `TestamentTest` — scope detached, publish on session disconnect | ⬜ Pending |
| `TestamentTest` — publish_options honored on auto-publish | ⬜ Pending |

**Reference:** xLib `WAMP_FP_TestamentMetaAPI.java`

**Key design decision:** Testament uses a `TestamentSessionListener` callback that is
invoked when `WampRouter.sessionLeft()` is called. If proposed meta procedure registration
is approved, add/flush are registered meta procedures. Otherwise, they are handled by
a wrapper router.

---

## Dependency Note

All modules in this phase depend on `ssg:lego-flow-wamp` via GitHub Packages.
Follow the pattern established in Phase 1 (see PHASE_1_Foundation.md Step 1).

## 4. Virtual Sessions

**Package:** `ssg.rwamp.feature.virtual`

Re-uses lego-flow's `WampSession`, `Realm`, and `WampRouter`. Virtual sessions
are lightweight — no transport, just identity.

### 4.1 Core Implementation
| Task | Status |
|---

## Dependency Note

All modules in this phase depend on `ssg:lego-flow-wamp` via GitHub Packages.
Follow the pattern established in Phase 1 (see PHASE_1_Foundation.md Step 1).---|--------|
| `VirtualSessionManager` — manages virtual sessions per realm | ⬜ Pending |
| `VirtualSession` — auth identity (authid, authrole, authmethod) + session ID | ⬜ Pending |
| `virtual_session.register` procedure — allocate session ID, set auth, call `sessionJoined()` | ⬜ Pending |
| `virtual_session.unregister` procedure — remove session, call `sessionLeft()` | ⬜ Pending |
| Virtual sessions appear in `wamp.session.list` and `wamp.session.count` (via Realm) | ⬜ Pending |
| Virtual sessions can be killed via `wamp.session.kill` (from Phase 1) | ⬜ Pending |

### 4.2 Tests
| Task | Status |
|---

## Dependency Note

All modules in this phase depend on `ssg:lego-flow-wamp` via GitHub Packages.
Follow the pattern established in Phase 1 (see PHASE_1_Foundation.md Step 1).---|--------|
| `VirtualSessionTest` — register virtual session | ⬜ Pending |
| `VirtualSessionTest` — virtual session appears in session meta (list/count/get) | ⬜ Pending |
| `VirtualSessionTest` — on_join event published for virtual session | ⬜ Pending |
| `VirtualSessionTest` — unregister + on_leave event | ⬜ Pending |
| `VirtualSessionTest` — kill virtual session via session.meta.kill | ⬜ Pending |

**Reference:** xLib `WAMP_FP_VirtualSession.java`

**Key design decision:** Virtual sessions are `WampSession` instances added to the
`Realm.sessions` map (via `Realm.getActiveSessions()` accessor). No transport is
associated — they exist purely for identity mapping.

---

## Dependency Note

All modules in this phase depend on `ssg:lego-flow-wamp` via GitHub Packages.
Follow the pattern established in Phase 1 (see PHASE_1_Foundation.md Step 1).

## 5. Phase 2 Completion Checklist

- [ ] All modules compile with Maven
- [ ] All modules compile with Gradle
- [ ] All tests pass (Phase 1 + Phase 2 combined)
- [ ] Reflection API returns live data from lego-flow Broker/Dealer
- [ ] Testaments publish correctly on session lifecycle events
- [ ] Virtual sessions integrate with session meta API
- [ ] README.md updated with Phase 2 features
- [ ] No WAMP core classes duplicated from lego-flow

---

## Dependency Note

All modules in this phase depend on `ssg:lego-flow-wamp` via GitHub Packages.
Follow the pattern established in Phase 1 (see PHASE_1_Foundation.md Step 1).

## 6. Test Count Targets

| Feature | Target Tests |
|---

## Dependency Note

All modules in this phase depend on `ssg:lego-flow-wamp` via GitHub Packages.
Follow the pattern established in Phase 1 (see PHASE_1_Foundation.md Step 1).------|-------------|
| Reflection API | 10+ |
| Testament API | 5+ |
| Virtual Sessions | 6+ |
| **Phase 2 Total** | **21+** |
| **Cumulative (P1+P2)** | **39+** |
