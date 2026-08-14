# rwamp-feature-rerouting — Requirements

## Module Timeline

- **Phase**: 3 (Integration)
- **Tests**: 7
- **Package**: `ssg.rwamp.feature.rerouting`

---

## Requirements

1. Implement `wamp.reroute.call` meta procedure
2. Extract `reroute` option from CALL (realm name + procedure URI)
3. Look up target realm via RealmManager
4. Validate procedure registration in target realm
5. Forward call to target Dealer
6. Return appropriate errors for missing realm or procedure

---

**Last Updated**: 2026-08-14
