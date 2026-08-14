# rwamp-rest — WAMP Compliance

## WAMP Specification Reference

REST over WAMP is not a WAMP core protocol feature — it is a bridging pattern used for integrating HTTP clients with WAMP routers.

### Implementation Notes

- Path resolution follows the convention of dot-delimited URIs (matching WAMP URI style)
- Virtual sessions provide WAMP identity for stateless HTTP callers
- Auth identity is extracted from query parameters

---

**Last Updated**: 2026-08-14
