# rwamp-feature-rerouting — WAMP Compliance

## WAMP Specification Reference

Cross-realm routing is not part of the WAMP core specification — it is an extension pattern for distributed deployments.

### Implementation Notes

- Uses `wamp.reroute.call` as the meta procedure URI
- Reroute option structure: `{realm: string, procedure: string}`
- Error responses use `{error: string}` for compatibility with WAMP error format

---

**Last Updated**: 2026-08-14
