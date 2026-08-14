# rwamp-rest — Requirements

## Module Timeline

- **Phase**: 3 (Integration)
- **Tests**: 10
- **Package**: `ssg.rwamp.rest`

---

## Requirements

1. Map HTTP paths to WAMP procedure URIs (`/com/example/foo` → `com.example.foo`)
2. Create virtual sessions for HTTP callers with auth identity
3. Reuse virtual sessions per auth identity
4. Route calls through the WAMP router
5. Return HTTP status codes from WAMP results/errors
6. Support both procedure calls (GET/POST) and topic publishes (POST)
7. Support query parameters for auth identity (`?authid=user1`)

---

**Last Updated**: 2026-08-14
