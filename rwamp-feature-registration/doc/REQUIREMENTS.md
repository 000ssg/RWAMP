# rwamp-feature-registration — Requirements

## Module Timeline

- **Phase**: 4 (Polish)
- **Tests**: 31
- **Package**: `ssg.rwamp.feature.registration`

---

## Requirements

1. Support exact match registration (`match: "exact"`)
2. Support prefix match registration (`match: "prefix"`)
3. Support wildcard match registration (`match: "wildcard"`, `*` = one segment)
4. Intercept REGISTER messages and store patterns
5. Intercept UNREGISTER messages and remove patterns
6. Intercept CALL messages and dispatch to matching calleep
7. Route Yield/Error from callee back to caller
8. Implement `wamp.registration.list` meta procedure
9. Implement `wamp.registration.revoke` meta procedure

---

**Last Updated**: 2026-08-14
