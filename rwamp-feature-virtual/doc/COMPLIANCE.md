# rwamp-feature-virtual — WAMP Compliance

## WAMP Specification Reference

Virtual sessions are not a WAMP core protocol feature — they are an extension pattern used for identity mapping (e.g., bridging HTTP to WAMP).

### Implementation Notes

- Virtual sessions participate in `wamp.session` meta procedures
- Virtual sessions are fully compatible with testament publishing
- Auth context follows WAMP conventions (authid, authrole, authmethod)

---

**Last Updated**: 2026-08-14
