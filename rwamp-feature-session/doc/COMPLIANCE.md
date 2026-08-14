# rwamp-feature-session — WAMP Compliance

## WAMP Specification Reference

This module implements [WAMP Advanced Profile — Session meta procedures](https://wamp-proto.org/wamp_latest throne.html#session-meta-procedures).

### Implemented Procedures

| Procedure | WAMP Spec Section | Status |
|-----------|-------------------|--------|
| `wamp.session.kill` | Session Management | ✅ Implemented |
| `wamp.session.killall` | Session Management | ✅ Implemented (subset — no session detail filtering) |
| `wamp.session.interrupt` | Session Management | ✅ Implemented (best-effort) |

### Known Deviations

- `wamp.session.killall` does not support authid/authrole filtering — the `SessionTransportTracker` does not store `WampSession` objects, so it cannot check auth attributes. This is a deliberate trade-off for simplicity.
- `wamp.session.interrupt` is best-effort — the handler acknowledges the request but does not wait for Dealer to cancel invocations.

---

**Last Updated**: 2026-08-14
