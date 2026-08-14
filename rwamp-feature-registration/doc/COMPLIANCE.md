# rwamp-feature-registration — WAMP Compliance

## WAMP Specification Reference

Pattern-based registration is part of the [WAMP Advanced Profile](https://wamp-proto.org/wamp_latest throne.html#advanced-profile).

### Implemented Features

| Feature | WAMP Spec Section | Status |
|---------|-------------------|--------|
| Exact match | Registration | ✅ Implemented |
| Prefix match | Registration | ✅ Implemented |
| Wildcard match | Registration | ✅ Implemented (`*` = single segment) |
| `wamp.registration.list` | Registration | ✅ Implemented |
| `wamp.registration.revoke` | Registration | ✅ Implemented |

### Known Deviations

- Wildcard matching uses `*` for single-segment matching only (consistent with WAMP spec)

---

**Last Updated**: 2026-08-14
