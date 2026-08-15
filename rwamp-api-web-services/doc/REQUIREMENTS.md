# rwamp-api-web-services — Requirements

## Functional Requirements

### FR-1: Dynamic Annotation Loading
The provider must load annotation classes at runtime via `Class.forName()` without requiring compile-time dependencies. If an annotation class is absent, the provider must not throw exceptions.

### FR-2: Generic Annotation Provider
The `GenericAnnotationApiProvider` must accept arbitrary annotation class names as strings and scan for matching annotations on classes, methods, and parameters.

### FR-3: JAX-RS Support
The `JaxRsApiProvider` must:
- Auto-detect the available JAX-RS namespace (Jakarta EE or Java EE)
- Scan `@Path`-annotated classes and `@GET`/`@POST`/etc.-annotated methods
- Extract parameter metadata from `@PathParam`, `@QueryParam`, `@HeaderParam`, `@FormParam`, `@CookieParam`
- Extract media types from `@Consumes` and `@Produces`

### FR-4: Graceful Degradation
When annotation classes are not on the classpath:
- `isOperable()` must return `false`
- `canHandle()` must return `false`
- `build()` must return an empty (but valid) `ApiDefinition`
- No `ClassNotFoundException` or `NullPointerException` must be thrown

### FR-5: Parameter Source Inference
The provider must infer parameter source from annotation names:
- `*PathParam*` → PATH
- `*QueryParam*` → QUERY
- `*HeaderParam*` → HEADER
- `*FormParam*` → FORM
- `*CookieParam*` → COOKIE

### FR-6: Weak Dependency
The JAX-RS API dependency must be declared as `provided` (Gradle) and `optional` (Maven), ensuring it's never transitively pulled into dependent projects.

## Non-Functional Requirements

### NFR-1: Performance
- Annotation class loading must be cached to avoid repeated `Class.forName()` calls
- Caching must use the `ClassLoader` as part of the cache key

### NFR-2: Compatibility
- Must support both Jakarta EE (9+) `jakarta.ws.rs.*` and Java EE (8-) `javax.ws.rs.*` namespaces
- Must work with Java 25 (project toolchain)

### NFR-3: Code Quality
- Minimum 80% line coverage on test suite
- No unchecked warnings in production code (where avoidable)

## Constraints

- Must follow RWAMP project conventions (package `ssg.rwamp`, Java 25, dual Gradle/Maven build)
- Must not modify the core `ApiProvider` interface contract beyond adding a default `isOperable()` method
- Must not introduce hard dependencies on web-service annotation libraries

---

**Last Updated**: 2026-08-15
