# rwamp-api-web-services — Compliance

## RWAMP Project Conventions

| Convention | Status | Notes |
|---|---|---|
| Package prefix `ssg.rwamp` | ✅ | `ssg.rwamp.api.webservices` |
| Java 25 toolchain | ✅ | Configured in root `build.gradle.kts` |
| Dual Gradle/Maven build | ✅ | `build.gradle.kts` + `pom.xml` |
| AssertJ assertions | ✅ | All tests use AssertJ |
| JaCoCo 80%+ coverage | ✅ | 87.9% line coverage |
| Module docs | ✅ | README.md + doc/*.md |
| SLF4J logging | ⚠️ | Uses `java.util.logging` (inherited from xLib pattern); consider migrating to SLF4J |

## Dependency Compliance

| Dependency | Scope | Rationale |
|---|---|---|
| `rwamp-api-providers` | compile | Required for `ApiProvider` interface and model classes |
| `jakarta.ws.rs-api` | provided/optional | Weak dependency — truly optional at runtime |
| `slf4j-api` | compile | Logging (managed by root POM) |
| `junit-jupiter` | test | Test framework (managed by root POM) |
| `assertj-core` | test | Assertion library (managed by root POM) |

## xLib Design Compliance

| xLib Pattern | RWAMP Implementation | Compliance |
|---|---|---|
| String-based annotation names | `setTypeAnnotations(String...)` | ✅ |
| `Class.forName()` loading | `AnnotationScanner.loadAnnotation()` | ✅ |
| `isOperable()` guard | `ApiProvider.isOperable()` default method | ✅ |
| Lazy class loading + caching | `annotationCache` map | ✅ |
| Configurable `ClassLoader` | `AnnotationScanner(ClassLoader)` constructor | ✅ |

## Build Verification

```bash
# Compile main sources
./gradlew :rwamp-api-web-services:compileJava

# Run tests
./gradlew :rwamp-api-web-services:test

# Verify coverage (min 50% per module)
./gradlew :rwamp-api-web-services:jacocoTestCoverageVerification

# Full build with Maven
mvn -pl rwamp-api-web-services clean verify

# Aggregate verification
./gradlew jacocoAggregateVerification
```

## Known Limitations

1. **Thread safety**: `AnnotationScanner` cache is not thread-safe. For concurrent use, create one scanner per thread or use external synchronization.
2. **Logging**: Uses `java.util.logging` instead of SLF4J — matches xLib convention but inconsistent with RWAMP.
3. **Generic type erasure**: Complex generic types (e.g. `Map<String, List<User>>`) are flattened to raw types in `ApiDataType`.

---

**Last Updated**: 2026-08-15
