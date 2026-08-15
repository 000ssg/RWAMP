# rwamp-api-publishers

Multi-format API publishing module for RWAMP.

## Overview

This module takes structured API definitions (from `rwamp-api-providers`) and publishes them in various formats:

- **OpenAPI 3.1.x** — JSON and YAML specifications for REST APIs
- **WAMP Procedures** — Registration on a WAMP router with reflection metadata
- **REST HTTP** — Full HTTP endpoint exposure via REST-WAMP bridge
- **Interactive HTML** — Self-contained documentation pages

## Publisher Implementations

### OpenApiPublisher

Generates OpenAPI 3.1.x specs from API definitions:

```java
var publisher = new OpenApiPublisher()
    .serverUrl("https://api.example.com")
    .contact("API Team", "api@example.com")
    .license("MIT");

String json = publisher.publishAsJson(apiDefinition);
String yaml = publisher.publishAsYaml(apiDefinition);
```

Supports security schemes (bearer, API key, basic auth), path resolution from annotations, JSON Schema generation, and error response mapping.

### WampApiPublisher

Registers API operations as WAMP procedures:

```java
var publisher = new WampApiPublisher(router, "com.app")
    .handler((op, call, transport) -> {
        // Handle the call and return a result
        return execute(op.name(), call.args());
    });

Map<String, String> results = publisher.publish(apiDefinition);
// {"get-user": "com.app.users.get-user", ...}
```

Creates both Dealer registrations (visible in `getRegisteredProcedures()`) and reflection metadata for runtime introspection.

### RestPublisher

Exposes API operations as HTTP endpoints via the REST-WAMP bridge:

```java
var result = new RestPublisher(router, sessionManager, "com.app")
    .basePath("/api")
    .publish(apiDefinition);

// Handle HTTP requests
RestResponse response = result.bridge().handle(
    new RestRequest("GET", "/api/users/123", Map.of()));
```

### JsDocPublisher

Generates interactive HTML documentation:

```java
var html = new JsDocPublisher()
    .title("My API Documentation")
    .publish(apiDefinition);

// Save to file
Files.writeString(Path.of("api-docs.html"), html);
```

Produces a self-contained HTML file with collapsible groups, parameter inputs, and "Try it" buttons.

## Unified Publishing

The `UnifiedApiPublisher` orchestrates multiple publishers in one call:

```java
var result = new UnifiedApiPublisher("com.app.api")
    .provider(new AnnotationBasedApiProvider("my-api"))
    .openApiPublisher(p -> p
        .serverUrl("https://api.example.com")
        .license("MIT"))
    .wampPublisher(router, "com.app")
    .restPublisher(router, sessionManager, "com.app")
        .basePath("/api")
    .jsDocPublisher()
    .publish(UserService.class, OrderService.class);

// Access all outputs
String openApiJson = result.openApiJson();
String openApiYaml = result.openApiYaml();
Map<String, String> wampUris = result.wampResults();
RestWampBridge restBridge = result.restBridge();
String htmlDocs = result.jsDocHtml();
```

## Architecture

```
[ApiDefinition]
    │
    ├──→ OpenApiPublisher ──→ JSON / YAML
    ├──→ WampApiPublisher ──→ WAMP procedures + reflection metadata
    ├──→ RestPublisher ──────→ HTTP endpoints (via REST-WAMP bridge)
    └──→ JsDocPublisher ────→ Interactive HTML docs
```

## Dependencies

- `rwamp-api-providers` — API definition model and providers
- `rwamp-feature-reflection` — WAMP reflection metadata
- `rwamp-feature-virtual` — Virtual session management for REST
- `rwamp-rest` — REST-WAMP bridge
- `lego-flow-wamp` — WAMP core

No external JSON/YAML serialization libraries — uses built-in serializers.
