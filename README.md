# RWAMP — Extended WAMP v2 for Java

[![Java](https://img.shields.io/badge/Java-25%2B-orange.svg)](https://www.oracle.com/java/)
[![Maven](https://img.shields.io/badge/Maven-3.9%2B-blue.svg)](https://maven.apache.org/)
[![Gradle](https://img.shields.io/badge/Gradle-9.x-green.svg)](https://gradle.org/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

Production-grade WAMP v2 extension built on **lego-flow's WAMP implementation**.

## Overview

RWAMP extends lego-flow's WAMP core with production-grade features found in established
WAMP implementations (xLib/Autobahn):

| Feature | Module | Status |
|---------|--------|--------|
| Session kill (`wamp.session.kill*`) | `rwamp-feature-session` | ✅ Phase 1 |
| Statistics (`wamp.statistics.get`) | `rwamp-feature-statistics` | ✅ Phase 1 |
| Testaments (`wamp.session.add_testament`) | `rwamp-feature-testament` | ✅ Phase 2 |
| Virtual sessions | `rwamp-feature-virtual` | ✅ Phase 2 |
| Reflection API (`wamp.reflection.*`) | `rwamp-feature-reflection` | ✅ Phase 2 |
| REST over WAMP | `rwamp-rest` | ✅ Phase 3 |
| Call rerouting | `rwamp-feature-rerouting` | ✅ Phase 3 |
| Pattern registration | `rwamp-feature-registration` | ⬜ Phase 4 |

## Architecture

```mermaid
graph TD
    subgraph "lego-flow (dependency)"
        LF["ssg:lego-flow-wamp<br/>WAMP core: messages, session,<br/>broker, dealer, router, auth, transport"]
    end

    subgraph "RWAMP Phase 1 ✅"
        S["rwamp-feature-session<br/>kill, killall, interrupt"]
        ST["rwamp-feature-statistics<br/>counters + transport wrapper"]
    end

    subgraph "RWAMP Phase 2 ✅"
        T["rwamp-feature-testament<br/>schedule events on session close"]
        V["rwamp-feature-virtual<br/>identity mapping for HTTP users"]
        R["rwamp-feature-reflection<br/>introspect procedures, topics"]
    end

    subgraph "RWAMP Phase 3 ✅"
        REST["rwamp-rest<br/>HTTP-to-WAMP bridge"]
        RR["rwamp-feature-rerouting<br/>cross-realm RPC forwarding"]
    end

    subgraph "RWAMP Phase 4 ⬜"
        RG["rwamp-feature-registration<br/>pattern matching"]
    end

    S --> LF
    ST --> LF
    T --> LF
    V --> LF
    R --> LF
    REST --> LF
    REST --> V
    REST --> R
    RR --> LF
    RG --> LF
```

## Building

### Maven

```bash
mvn compile
mvn test
```

### Gradle

```bash
./gradlew compileJava
./gradlew test
```

### Dependencies

Requires `ssg:lego-flow-wamp` from GitHub Packages:
```
https://maven.pkg.github.com/000ssg/lego-flow
```

For local development, install lego-flow first:
```bash
cd /path/to/lego-flow && mvn install -DskipTests
```

Both builds resolve `lego-flow-wamp:0.2.0-SNAPSHOT` from `~/.m2/repository` or
GitHub Packages (with `GITHUB_ACTOR`/`GITHUB_TOKEN`).

## Project Structure

| Module | Package | Purpose | Tests |
|--------|---------|---------|-------|
| `rwamp-feature-session` | `ssg.rwamp.feature.session` | Session kill procedures | 9 |
| `rwamp-feature-statistics` | `ssg.rwamp.feature.statistics` | Statistics counters | 10 |
| `rwamp-feature-testament` | `ssg.rwamp.feature.testament` | Testament scheduling | 6 |
| `rwamp-feature-virtual` | `ssg.rwamp.feature.virtual` | Virtual session manager | 6 |
| `rwamp-feature-reflection` | `ssg.rwamp.feature.reflection` | Introspection API | 7 |
| `rwamp-rest` | `ssg.rwamp.rest` | REST over WAMP bridge | 10 |
| `rwamp-feature-rerouting` | `ssg.rwamp.feature.rerouting` | Cross-realm forwarding | 7 |
| **Total** | | | **55** |

## Quick Start

### Session Kill

```java
var router = new WampRouter();
var tracker = new SessionTransportTracker();
SessionMetaApi.register(router, tracker);

// Track session transport when created
tracker.track(sessionId, transport);

// Call wamp.session.kill from any session
router.route(new WampMessage.Call(requestId, options, "wamp.session.kill",
        List.of(targetSessionId)), callerTransport);
```

### Reflection API

```java
var router = new WampRouter();
var registry = ReflectionApi.createRegistry(router);
ReflectionApi.register(router, registry);

// Query available procedures
router.route(new WampMessage.Call(1, Map.of(), "wamp.reflection.procedure.list", null), transport);
// → Result with list of registered procedure URIs
```

### REST over WAMP

```java
var router = new WampRouter();
var realm = new Realm("realm1");
var virtualManager = new VirtualSessionManager(realm);
var registry = ReflectionApi.createRegistry(router);

VirtualSessionApi.register(router, realm, virtualManager);
ReflectionApi.register(router, registry);

var bridge = new RestWampBridge(router, virtualManager, registry);

// Map HTTP request to WAMP call
var request = new RestRequest("GET", "/com/example/greet",
        List.of("World"), Map.of("authid", "user1"), null);
var response = bridge.handle(request);
// → RestResponse(200, ["Hello, World!"])
```

### Call Rerouting

```java
var realmManager = new RealmManager();
realmManager.createRealm("realm1");
realmManager.createRealm("realm2");

var router = new WampRouter();
ReroutingApi.register(router, realmManager);

// Call with reroute option to forward to another realm
var reroute = new LinkedHashMap<String, Object>();
reroute.put("realm", "realm2");
reroute.put("procedure", "com.example.add");
var options = new LinkedHashMap<String, Object>();
options.put("reroute", reroute);
```

## Development

See [AGENTS.md](AGENTS.md) for development practices and [doc/plan/](doc/plan/) for
the detailed implementation plan.

## License

MIT — see [LICENSE](LICENSE)
