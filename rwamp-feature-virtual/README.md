# rwamp-feature-virtual

Virtual session management for WAMP.

## Overview

Create virtual WAMP sessions for identity mapping — typically used for HTTP users who need WAMP identity without a WebSocket connection.

| Procedure | Description |
|-----------|-------------|
| `virtual_session.register` | Allocate a virtual session with auth context |
| `virtual_session.unregister` | Remove a virtual session |

## Usage

```java
var realm = new Realm("realm1");
var manager = new VirtualSessionManager(realm);
VirtualSessionApi.register(router, realm, manager);
```

## API

| Class | Purpose |
|-------|---------|
| `VirtualSessionApi` | Register/unregister virtual session procedures |
| `VirtualSessionManager` | Allocate virtual sessions, manage auth context |

## Cross-references

- [Architecture](doc/ARCHITECTURE.md) | [Requirements](doc/REQUIREMENTS.md) | [Code Overview](doc/CODE_OVERVIEW.md) | [Compliance](doc/COMPLIANCE.md)
- [Root Architecture](../doc/ARCHITECTURE.md) | [Root README](../README.md)
