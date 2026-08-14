# rwamp-feature-session — Requirements

## Module Timeline

- **Phase**: 1 (Foundation)
- **Tests**: 14
- **Package**: `ssg.rwamp.feature.session`

---

## Requirements

1. Implement `wamp.session.kill(sessionId, reason)` — send GOODBYE to target session
2. Implement `wamp.session.killall(authid?, authrole?)` — kill matching sessions
3. Implement `wamp.session.interrupt(sessionId, mode)` — request pending call cancellation
4. Track session-to-transport mappings externally (not in the router)
5. Follow lego-flow's test patterns: `InMemoryTransport`, AssertJ

---

**Last Updated**: 2026-08-14
