# rwamp-feature-testament — Requirements

## Module Timeline

- **Phase**: 2 (Discovery)
- **Tests**: 6
- **Package**: `ssg.rwamp.feature.testament`

---

## Requirements

1. Implement `wamp.session.add_testament(topic, args, kwargs)` with scope option
2. Implement `wamp.session.flush_testament(scope)` to remove scheduled testaments
3. Auto-publish testaments on session close (destroyed scope)
4. Support `SCOPE_DESTROYED` and `SCOPE_CLOSED` scopes
5. Testament publishing goes through the Broker (not directly to subscribers)

---

**Last Updated**: 2026-08-14
