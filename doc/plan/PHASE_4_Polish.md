# Phase 4 — Polish

**RWAMP Phase 4:** Pattern-based Registration, Registration Revocation, Final Documentation
**Effort:** 5 days
**Dependencies:** Phase 3 complete

---

## 1. Goals

After Phase 4, RWAMP has:
- **Pattern-based procedure registration** — wildcard/prefix matching for procedure names
- **Registration revocation** — router can revoke registrations from callees
- **Registration meta API** — introspect registrations
- **Final documentation** — README, ARCHITECTURE.md, REQUIREMENTS.md
- **Full dual-build verification**

---

## 2. Pattern-Based Registration

**Package:** `ssg.rwamp.feature.registration`

Extends lego-flow's `Dealer` with pattern matching. Implemented as a wrapper
that intercepts REGISTER and CALL messages.

### 2.1 Implementation
| Task | Status |
|------|--------|
| `PatternRegistrationHandler` — intercepts REGISTER with `match` option | ⬜ Pending |
| `match` option support: `"exact"` (default), `"prefix"`, `"wildcard"` | ⬜ Pending |
| Pattern matching for procedure URIs (compatible with Broker's wildcard matching) | ⬜ Pending |
| `PatternDealer` — wraps lego-flow's Dealer, routes CALL to matching pattern registrations | ⬜ Pending |
| Pattern-to-registration mapping stored in RWAMP, not in lego-flow | ⬜ Pending |

### 2.2 Tests
| Task | Status |
|------|--------|
| `PatternRegistrationTest` — exact match (delegates to lego-flow Dealer) | ⬜ Pending |
| `PatternRegistrationTest` — prefix match | ⬜ Pending |
| `PatternRegistrationTest` — wildcard match | ⬜ Pending |
| `PatternRegistrationTest` — multiple pattern registrations, correct selection | ⬜ Pending |

**Reference:** xLib `WAMPRPCDealer.java` — pattern matching in registrations

**Key design decision:** Pattern registrations are tracked in RWAMP's own registry.
When a CALL arrives, `PatternDealer` checks its pattern registry first; if no pattern
matches, it delegates to lego-flow's Dealer (exact match).

---

## 3. Registration Revocation

**Package:** `ssg.rwamp.feature.registration`

### 3.1 Implementation
| Task | Status |
|------|--------|
| `wamp.registration.revoke` procedure — revoke specific registration | ⬜ Pending |
| Revoke sends INTERRUPT to callee (via lego-flow's `WampTransport`) | ⬜ Pending |
| `wamp.registration.get` procedure — get registration details | ⬜ Pending |
| `wamp.registration.list` procedure — list all registrations in realm | ⬜ Pending |
| Integration with Reflection API — registration changes reflected | ⬜ Pending |

### 3.2 Tests
| Task | Status |
|------|--------|
| `RegistrationMetaTest` — revoke and verify callee affected | ⬜ Pending |
| `RegistrationMetaTest` — get registration details | ⬜ Pending |
| `RegistrationMetaTest` — list registrations | ⬜ Pending |

**Reference:** xLib `WAMPRPCDealer.java` — revocation handling

**Key design decision:** Revocation reads registration data from lego-flow's Dealer and
sends INTERRUPT via the transport. The registration is removed via `handleUnregister()`
on the underlying Dealer.

---

## 4. Documentation

### 4.1 README.md
| Task | Status |
|------|--------|
| Final README — "RWAMP extends lego-flow's WAMP" | ⬜ Pending |
| Architecture diagram (Mermaid) — show lego-flow as dependency, RWAMP as extensions | ⬜ Pending |
| Feature table — WAMP Advanced Profile coverage | ⬜ Pending |
| Dependency on `ssg:lego-flow-wamp` clearly documented | ⬜ Pending |

### 4.2 doc/ARCHITECTURE.md
| Task | Status |
|------|--------|
| "Extension over lego-flow" — how RWAMP wraps/augments lego-flow components | ⬜ Pending |
| Decorator/wrapper pattern — StatisticsRouter, PatternDealer, etc. | ⬜ Pending |
| Meta procedure registration — how new procedures are wired into WampRouter | ⬜ Pending |
| Data flow diagrams (Mermaid sequence diagrams) | ⬜ Pending |
| Thread safety model (lego-flow uses virtual threads) | ⬜ Pending |

### 4.3 doc/REQUIREMENTS.md
| Task | Status |
|------|--------|
| Per-phase requirement documentation | ⬜ Pending |
| Feature design decisions — why wrapper vs. upstream change | ⬜ Pending |
| Test coverage summary | ⬜ Pending |

---

## 5. Phase 4 Completion Checklist

- [ ] All modules compile with Maven
- [ ] All modules compile with Gradle
- [ ] All tests pass (full suite)
- [ ] README.md complete and polished
- [ ] doc/ARCHITECTURE.md complete
- [ ] doc/REQUIREMENTS.md complete
- [ ] Architecture diagram in README.md
- [ ] No WAMP core classes duplicated from lego-flow

---

## 6. Test Count Targets

| Feature | Target Tests |
|---------|-------------|
| Pattern Registration | 4+ |
| Registration Meta | 4+ |
| **Phase 4 Total** | **8+** |
| **Cumulative (P1-P4)** | **63+** |
