# rwamp-feature-registration — Code Overview

> Cross-references: [README](../README.md) | [Architecture](ARCHITECTURE.md) | [Requirements](REQUIREMENTS.md)
> Parent: [Project CODE_OVERVIEW](../../doc/CODE_OVERVIEW.md)

## Source Structure

```
rwamp-feature-registration/src/main/java/ssg/rwamp/feature/registration/
├── PatternMatcher.java          — exact/prefix/wildcard matching utility
├── PatternRegistry.java         — pattern storage with transport refs
├── RegistrationHandler.java     — process REGISTER/UNREGISTER messages
├── RegistrationInterceptor.java — message filter before router
└── RegistrationMetaApi.java     — list and revoke meta procedures

rwamp-feature-registration/src/test/java/ssg/rwamp/feature/registration/
├── InMemoryTransport.java       — local copy
├── PatternMatcherTest.java      — matching logic tests
├── RegistrationHandlerTest.java — register/unregister message handling
├── RegistrationInterceptorTest.java — end-to-end interception
└── RegistrationMetaApiTest.java — meta procedure handling
```

## Key Classes

| Class | Type | Purpose |
|-------|------|---------|
| `RegistrationInterceptor` | Stateful | Main entry point; intercepts messages |
| `PatternRegistry` | Stateful | Stores patterns, resolves matches |
| `PatternMatcher` | Utility (static) | Match algorithm implementation |
| `RegistrationHandler` | Stateless | Processes REGISTER/UNREGISTER |
| `RegistrationMetaApi` | Utility (static) | Meta procedure registration |

## Design Notes

- `wildcardMatch()` splits by `.`, requires equal segment count, `*` matches any segment
- `RegistrationInterceptor` tracks invocations via `ConcurrentHashMap<invocationId, callerInfo>`
- `routeYield()` and `routeCalleeError()` handle the return path from callee
- `InvocationInfo` record holds caller transport and original call request ID
