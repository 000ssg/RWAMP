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

**Package:** `ssg.rwamp.router.Dealer` (extension)

### 2.1 Implementation
| Task | Status |
|------|--------|
| `match` option in REGISTER — `"exact"`, `"prefix"`, `"wildcard"` | ⬜ Pending |
| Prefix matching for procedure URIs (e.g., `com.example.*`) | ⬜ Pending |
| Wildcard matching for procedure URIs (e.g., `com..bar`) | ⬜ Pending |
| Dealer uses pattern matching when routing CALL to registrations | ⬜ Pending |
| `RegistrationEntry` extended with match policy | ⬜ Pending |
| Procedure pattern matching compatible with Broker wildcard matching | ⬜ Pending |

### 2.2 Tests
| Task | Status |
|------|--------|
| `PatternRegistrationTest` — exact match (existing) | ⬜ Pending |
| `PatternRegistrationTest` — prefix match | ⬜ Pending |
| `PatternRegistrationTest` — wildcard match | ⬜ Pending |
| `PatternRegistrationTest` — multiple pattern registrations, correct selection | ⬜ Pending |

**Reference:** xLib `WAMPRPCDealer.java` — pattern matching in registrations

---

## 3. Registration Revocation

**Package:** `ssg.rwamp.router.feature.registration`

### 3.1 Implementation
| Task | Status |
|------|--------|
| `wamp.registration.revoke` procedure — revoke specific registration | ⬜ Pending |
| Revoke sends INTERRUPT to callee with `mode: killnowait` | ⬜ Pending |
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

---

## 4. Documentation

### 4.1 README.md
| Task | Status |
|------|--------|
| Final README with all features listed | ⬜ Pending |
| Architecture diagram (Mermaid) — full module overview | ⬜ Pending |
| Feature table with WAMP Advanced Profile coverage | ⬜ Pending |
| Quick start with WebSocket example | ⬜ Pending |
| REST bridge usage example | ⬜ Pending |

### 4.2 doc/ARCHITECTURE.md
| Task | Status |
|------|--------|
| Module descriptions | ⬜ Pending |
| Data flow diagrams (Mermaid sequence diagrams) | ⬜ Pending |
| Feature provider architecture | ⬜ Pending |
| Thread safety model | ⬜ Pending |

### 4.3 doc/REQUIREMENTS.md
| Task | Status |
|------|--------|
| Per-phase requirement documentation | ⬜ Pending |
| Feature design decisions | ⬜ Pending |
| Test coverage summary | ⬜ Pending |

### 4.4 doc/FEATURES.md
| Task | Status |
|------|--------|
| Complete feature matrix (WAMP Advanced Profile features) | ⬜ Pending |
| Per-feature description with WAMP spec reference | ⬜ Pending |
| Example usage for each feature | ⬜ Pending |

---

## 5. rwamp-auth Module

**Package:** `ssg.rwamp.auth`

### 5.1 Auth Providers
| Task | Status |
|------|--------|
| `AnyAuthProvider` — accepts any authentication | ⬜ Pending |
| `CraAuthProvider` — Challenge-Response Authentication | ⬜ Pending |
| `TicketAuthProvider` — Ticket-based authentication | ⬜ Pending |
| `CryptosignAuthProvider` — Ed25519 public-key authentication | ⬜ Pending |
| Integration with WampClient (auth method configuration) | ⬜ Pending |

### 5.2 Tests
| Task | Status |
|------|--------|
| `AnyAuthTest` — accepts any | ⬜ Pending |
| `CraAuthTest` — challenge/response cycle | ⬜ Pending |
| `TicketAuthTest` — ticket verification | ⬜ Pending |
| `CryptosignAuthTest` — Ed25519 sign/verify | ⬜ Pending |

**Reference:** lego-flow `CryptosignAuth.java`, `TicketAuth.java`, `WampCraAuth.java`

---

## 6. Phase 4 Completion Checklist

- [ ] All modules compile with Maven
- [ ] All modules compile with Gradle
- [ ] All tests pass (full suite)
- [ ] README.md complete and polished
- [ ] doc/ARCHITECTURE.md complete
- [ ] doc/REQUIREMENTS.md complete
- [ ] doc/FEATURES.md complete
- [ ] Final architecture diagram in README.md
- [ ] Final cost estimate documented

---

## 7. Test Count Targets

| Feature | Target Tests |
|---------|-------------|
| Pattern Registration | 5+ |
| Registration Meta | 4+ |
| Auth Providers | 6+ |
| **Phase 4 Total** | **15+** |
| **Cumulative (P1-P4)** | **150+** |
