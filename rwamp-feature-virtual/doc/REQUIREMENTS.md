# rwamp-feature-virtual — Requirements

## Module Timeline

- **Phase**: 2 (Discovery)
- **Tests**: 6
- **Package**: `ssg.rwamp.feature.virtual`

---

## Requirements

1. Implement `virtual_session.register(authInfo)` — allocate session with auth context
2. Implement `virtual_session.unregister(sessionId)` — remove virtual session
3. Virtual sessions must be visible to session meta procedures (list, kill)
4. Virtual sessions must work with testament publishing
5. Auth context includes authid, authrole, authmethod

---

**Last Updated**: 2026-08-14
