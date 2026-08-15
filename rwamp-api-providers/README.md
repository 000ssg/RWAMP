# rwamp-api-providers

Reflection-based API discovery and definition module for RWAMP.

## Overview

This module provides a framework for automatically discovering and describing API operations from Java classes using reflection and annotations. Inspired by xLib's `MethodsProvider` and `ReflectiveMethodsProvider` patterns.

## Key Concepts

- **ApiProvider** — Strategy interface for discovering API definitions from sources (classes, annotations, configurations)
- **ApiDefinition** — Top-level container: name, version, groups, types, metadata
- **ApiGroup** — Named container for operations, types, and sub-groups (e.g. "UserService")
- **ApiOperation** — A callable procedure with parameters, response type, tags, and metadata
- **ApiDataType** — Type descriptor for parameters and responses with JSON Schema support

## Provider Implementations

### AnnotationBasedApiProvider

Scans `@ApiService`-annotated classes and extracts `@Operation`-annotated methods:

```java
@ApiService(name = "users", path = "/api/users", tags = {"admin"})
public class UserService {
    @Operation(summary = "Get user by ID", httpMethods = {HttpMethod.GET})
    public User getUser(@ApiParam(name = "id") Long id) { ... }
}

var provider = new AnnotationBasedApiProvider("user-api");
ApiDefinition api = provider.build(UserService.class);
```

### GetterSetterApiProvider

Derives operations from JavaBean getter/setter pairs:

```java
var provider = new GetterSetterApiProvider("bean-api");
ApiDefinition api = provider.build(MyBean.class);
// Produces: "property" from getProperty()/setProperty()
```

### ManualApiProvider

Programmatic fluent builder for custom definitions:

```java
ApiDefinition api = ManualApiProvider.builder("custom-api")
    .version("1.0.0")
    .description("Manually defined API")
    .group("orders", "Order management", g -> g
        .tag("ecommerce")
        .operation("create-order", "Create a new order", op -> op
            .param("productId", ApiDataType.INTEGER)
            .param("quantity", ApiDataType.INTEGER)
            .response(ApiDataType.STRING)
        )
    )
    .build();
```

### CombinedApiProvider

Aggregates multiple providers and merges their output:

```java
var combined = new CombinedApiProvider("merged-api")
    .addProvider(new AnnotationBasedApiProvider("annotated"))
    .addProvider(new GetterSetterApiProvider("beans"));
```

## Annotations

| Annotation | Target | Purpose |
|---|---|---|
| `@ApiService` | Type | Marks a class as an API service |
| `@Operation` | Method | Declares a method as an exposed API operation |
| `@ApiParam` | Parameter | Describes a method parameter |
| `@ApiAccess` | Type/Method/Param | Role-based access control |
| `@ApiIgnore` | Type/Method/Field/Param | Excludes from API discovery |
| `@ApiProperty` | Field/Method | Describes a bean property |

## Architecture

```
[Java Class] → [ApiProvider] → [ApiDefinition] → [ApiPublisher] → [Output]
                    ↑
            [ReflectionApiBuilder]
                    ↑
            [Annotations / Conventions]
```

## Dependencies

- `lego-flow-wamp` (for WAMP type references)
- `slf4j-api` (logging)

No external JSON/YAML serialization libraries — the module uses only JDK APIs.
