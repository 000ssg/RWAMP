# rwamp-feature-statistics — WAMP Compliance

## WAMP Specification Reference

`wamp.statistics.get` is not a WAMP core protocol procedure — it is a common extension used in production deployments for observability.

### Implementation Notes

- Returns per-realm counters in a structure compatible with WAMP tooling
- Supports optional `realm` filter via kwargs

---

**Last Updated**: 2026-08-14
