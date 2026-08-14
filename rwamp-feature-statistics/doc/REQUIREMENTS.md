# rwamp-feature-statistics — Requirements

## Module Timeline

- **Phase**: 1 (Foundation)
- **Tests**: 12
- **Package**: `ssg.rwamp.feature.statistics`

---

## Requirements

1. Implement `wamp.statistics.get` meta procedure returning per-realm counters
2. Provide `StatisticsTransport` decorator for counting messages per type
3. Support optional realm filter in statistics query
4. Snapshot must be thread-safe

---

**Last Updated**: 2026-08-14
