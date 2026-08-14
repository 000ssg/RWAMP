# rwamp-feature-virtual — Architecture

## Module Purpose

Create virtual WAMP sessions that represent remote identities (e.g., HTTP users) within the WAMP session model. Virtual sessions participate in session meta procedures and can be targeted by testament publishing.

## Key Abstractions

### VirtualSessionManager

Manages virtual session lifecycle:
1. Allocates a unique session ID (via `AtomicLong`)
2. Creates a `WampSession` with the given auth context
3. Stores the session for lookup

### VirtualSessionApi

Registers `virtual_session.register` and `virtual_session.unregister` as meta procedures. The register handler creates a session and calls `router.sessionJoined()` to make it visible to meta procedures.

## Thread Safety

- `VirtualSessionManager` uses `ConcurrentHashMap` for session storage
- Session ID allocation is atomic via `AtomicLong`

## Extension Points Used

| Extension | From lego-flow | Purpose |
|-----------|---------------|---------|
| `WampRouter.registerMetaProcedure()` | WampRouter | Register virtual session procedures |
| `WampRouter.sessionJoined()` | WampRouter | Make virtual sessions visible |

---

**Last Updated**: 2026-08-14
