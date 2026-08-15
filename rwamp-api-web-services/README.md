# rwamp-api-web-services

Dynamic web-service annotation scanning for RWAMP — xLib-inspired weak dependency pattern.

## Overview

This module extends RWAMP's API discovery framework with providers that scan web-service annotations (JAX-RS, Jakarta EE, or custom annotation sets) using **dynamic class loading** — no compile-time dependency on external annotation libraries.

When annotation libraries are absent from the classpath, the provider gracefully degrades: `isOperable()` returns `false` and the provider is effectively disabled.

## Key Concepts

- **AnnotationScanner** — Utility for dynamic annotation class loading via `Class.forName()` with caching
- **GenericAnnotationApiProvider** — User-configurable provider: pass annotation class names as strings
- **JaxRsApiProvider** — Pre-configured for JAX-RS with auto-detection of Jakarta EE vs Java EE namespace
- **WsMethod / WsParameter** — Web-service-specific model classes carrying HTTP method, path, and parameter source metadata

## Quick Start

### Generic Annotation Provider

```java
var provider = new GenericAnnotationApiProvider("my-api")
    .setTypeAnnotations("jakarta.ws.rs.Path")
    .setMethodAnnotations("jakarta.ws.rs.GET", "jakarta.ws.rs.POST",
                           "jakarta.ws.rs.PUT", "jakarta.ws.rs.DELETE")
    .setParameterAnnotations("jakarta.ws.rs.PathParam", "jakarta.ws.rs.QueryParam",
                              "jakarta.ws.rs.HeaderParam", "jakarta.ws.rs.FormParam")
    .setConsumesAnnotations("jakarta.ws.rs.Consumes")
    .setProducesAnnotations("jakarta.ws.rs.Produces");

if (provider.isOperable()) {
    ApiDefinition api = provider.build(MyResource.class);
    System.out.println("Discovered " + api.allOperations().size() + " operations");
} else {
    System.out.println("JAX-RS annotations not available — provider disabled");
}
```

### JAX-RS Provider (Auto-Detection)

```java
var provider = new JaxRsApiProvider("my-jaxrs-api");

if (provider.isOperable()) {
    System.out.println("Namespace: " + provider.detectedNamespace());
    // jakarta.ws.rs or javax.ws.rs
    ApiDefinition api = provider.build(MyResource.class);
} else {
    System.out.println("JAX-RS not on classpath — gracefully disabled");
}
```

### Graceful Degradation

```java
// Non-existent annotation classes — no NPE, no error
var provider = new GenericAnnotationApiProvider("degraded")
    .setTypeAnnotations("com.nonexistent.FakeAnnotation");

assert !provider.isOperable();  // Provider self-disables
ApiDefinition api = provider.build(SomeClass.class);  // Returns empty definition
```

## Why This Design?

The original xLib design for `AnnotationsBasedMethodsProvider` uses string-based annotation class names resolved at runtime via `Class.forName()`. This enables:

1. **Weak dependencies** — the module compiles without JAX-RS on the classpath
2. **Flexibility** — supports any annotation library, not just JAX-RS
3. **Graceful degradation** — works correctly whether or not the target library is present
4. **Backward compatibility** — both Jakarta EE (9+) and Java EE (8-) namespaces

## Module Dependencies

| Dependency | Scope | Purpose |
|---|---|---|
| `rwamp-api-providers` | compile | Core `ApiProvider` interface and model |
| `jakarta.ws.rs-api` | provided/optional | JAX-RS annotations (weak dependency) |
| `slf4j-api` | compile | Logging |

The JAX-RS dependency is declared as both `provided` (Gradle) and `<optional>true</optional>` (Maven), ensuring it's never transitively pulled in.

## Demo

See [`WebServicesDemo`](../rwamp-demos/src/main/java/ssg/rwamp/demo/feature/WebServicesDemo.java) for a complete example of:
- Generic annotation scanning with custom annotation sets
- JAX-RS namespace auto-detection
- Graceful degradation when annotations are absent
- Handling of unannotated classes

Run: `./gradlew :rwamp-demos:compileJava` then execute `WebServicesDemo.main()`.
