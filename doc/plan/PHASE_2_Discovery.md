# Phase 2 — Discovery & Identity

**RWAMP Phase 2:** Reflection API, Testament API, Virtual Sessions
**Effort:** 13 days
**Dependencies:** Phase 1 complete

---

## 1. Goals

After Phase 2, RWAMP has:
- **Reflection API** — introspect procedures, topics, types, errors via WAMP RPC
- **Testament API** — schedule events on session detach/destroy
- **Virtual Sessions** — create virtual WAMP sessions from HTTP-authenticated users

---

## 2. Reflection API

**Package:** `ssg.rwamp.router.feature.reflection`

### 2.1 Data Model
| Task | Status |
|------|--------|
| `ReflectionRegistry` class — per-realm registry for types, procedures, errors, topics | ⬜ Pending |
| `ReflectionEntry` record — name, category, metadata map | ⬜ Pending |
| Registry scoped by realm (stored in RealmManager or per-realm) | ⬜ Pending |

### 2.2 Procedures
| Task | Status |
|------|--------|
| `wamp.reflection.topic.list` — list topics with subscribers | ⬜ Pending |
| `wamp.reflection.topic.describe` — describe topics (subscriber count, retained events) | ⬜ Pending |
| `wamp.reflection.procedure.list` — list registered procedures | ⬜ Pending |
| `wamp.reflection.procedure.describe` — describe procedures (callee count, invoke policy) | ⬜ Pending |
| `wamp.reflection.error.list` — list known errors | ⬜ Pending |
| `wamp.reflection.type.list` — list type definitions | ⬜ Pending |
| `wamp.reflection.type.describe` — describe types | ⬜ Pending |

### 2.3 Event-Driven Reflection
| Task | Status |
|------|--------|
| `wamp.reflect.define` topic — register type/procedure/error definitions | ⬜ Pending |
| `wamp.reflect.describe` topic — request definitions | ⬜ Pending |
| `wamp.reflect.on_define` event — published when something is defined | ⬜ Pending |
| `wamp.reflect.on_undefine` event — published when something is undefined | ⬜ Pending |
| Automatic reflection on REGISTER/PUBLISH (capture procedure signatures from options) | ⬜ Pending |

### 2.4 Reflection Meta (self-description)
| Task | Status |
|------|--------|
| Each reflection procedure provides its own `getReflectionMeta()` (parameter schema) | ⬜ Pending |
| Reflection procedures are discoverable via `wamp.reflection.procedure.list` | ⬜ Pending |

### 2.5 Tests
| Task | Status |
|------|--------|
| `ReflectionAPITest` — topic.list/topic.describe | ⬜ Pending |
| `ReflectionAPITest` — procedure.list/procedure.describe | ⬜ Pending |
| `ReflectionAPITest` — type.list/type.describe | ⬜ Pending |
| `ReflectionEventTest` — define/describe topics | ⬜ Pending |
| `ReflectionAutoCaptureTest` — auto-capture from REGISTER with reflection options | ⬜ Pending |

**Reference:** xLib `WAMP_FP_Reflection.java`

---

## 3. Testament API

**Package:** `ssg.rwamp.router.feature.testament`

### 3.1 Core Implementation
| Task | Status |
|------|--------|
| `TestamentManager` class — stores testaments per session | ⬜ Pending |
| Testament data: topic, args, kwargs, publish_options | ⬜ Pending |
| Testament scope: `detached` (session disconnect) vs `destroyed` (session finalized) | ⬜ Pending |
| `wamp.session.add_testament` procedure | ⬜ Pending |
| `wamp.session.flush_testament` procedure | ⬜ Pending |
| Testament auto-publish on session close (detached scope) | ⬜ Pending |
| Testament auto-publish on session destroy (destroyed scope) | ⬜ Pending |
| Integration with session lifecycle (called from `WampSession.close()`) | ⬜ Pending |

### 3.2 Tests
| Task | Status |
|------|--------|
| `TestamentTest` — add_testament, verify stored | ⬜ Pending |
| `TestamentTest` — flush_testament, verify removed | ⬜ Pending |
| `TestamentTest` — scope destroyed, publish on session close | ⬜ Pending |
| `TestamentTest` — scope detached, publish on session disconnect | ⬜ Pending |
| `TestamentTest` — publish_options honored on auto-publish | ⬜ Pending |

**Reference:** xLib `WAMP_FP_TestamentMetaAPI.java`

---

## 4. Virtual Sessions

**Package:** `ssg.rwamp.router.feature.virtualsession`

### 4.1 Core Implementation
| Task | Status |
|------|--------|
| `VirtualSessionManager` class — manages virtual sessions per realm | ⬜ Pending |
| `VirtualSession` — lightweight session without transport (auth identity only) | ⬜ Pending |
| `virtual_session.register` procedure — register a virtual session with auth info | ⬜ Pending |
| `virtual_session.unregister` procedure — unregister a virtual session | ⬜ Pending |
| Publish `wamp.session.on_join` meta event for virtual session | ⬜ Pending |
| Publish `wamp.session.on_leave` meta event for virtual session | ⬜ Pending |
| Virtual sessions appear in `wamp.session.list` and `wamp.session.count` | ⬜ Pending |
| Virtual sessions can be killed via `wamp.session.kill` | ⬜ Pending |

### 4.2 Tests
| Task | Status |
|------|--------|
| `VirtualSessionTest` — register virtual session | ⬜ Pending |
| `VirtualSessionTest` — virtual session appears in session meta | ⬜ Pending |
| `VirtualSessionTest` — virtual session on_join event | ⬜ Pending |
| `VirtualSessionTest` — unregister + on_leave event | ⬜ Pending |
| `VirtualSessionTest` — kill virtual session via session.meta.kill | ⬜ Pending |

**Reference:** xLib `WAMP_FP_VirtualSession.java`

---

## 5. Phase 2 Completion Checklist

- [ ] All modules compile with Maven
- [ ] All modules compile with Gradle
- [ ] All tests pass (Phase 1 + Phase 2 combined)
- [ ] Reflection API self-describes via `wamp.reflection.procedure.describe`
- [ ] Testaments publish correctly on session lifecycle events
- [ ] Virtual sessions integrate with session meta API
- [ ] README.md updated with Phase 2 features
- [ ] Architecture diagram updated

---

## 6. Test Count Targets

| Feature | Target Tests |
|---------|-------------|
| Reflection API | 15+ |
| Testament API | 8+ |
| Virtual Sessions | 8+ |
| **Phase 2 Total** | **31+** |
| **Cumulative (P1+P2)** | **110+** |
