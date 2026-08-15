# rwamp-api-web-services — Code Overview

## Source Files

### Main Sources (`src/main/java`)

| File | Lines | Purpose |
|---|---|---|
| `AnnotationScanner.java` | ~150 | Dynamic annotation class loading, value extraction, caching |
| `GenericAnnotationApiProvider.java` | ~400 | Generic provider: configurable annotation names, scanning, model building |
| `JaxRsApiProvider.java` | ~170 | JAX-RS-specific provider with namespace auto-detection |
| `WsMethod.java` | ~100 | Web-service method model (path, HTTP method, media types) |
| `WsParameter.java` | ~80 | Web-service parameter model (source: PATH, QUERY, HEADER, etc.) |

### Test Sources (`src/test/java`)

| File | Purpose |
|---|---|
| `AnnotationScannerTest.java` | 18 tests: loading, matching, value extraction, caching |
| `GenericAnnotationApiProviderTest.java` | 15 tests: operability, scanning, parameter extraction, edge cases |
| `JaxRsApiProviderTest.java` | 9 tests: namespace detection, delegation, graceful degradation |
| `WsModelTest.java` | 6 tests: WsMethod/WsParameter construction, equality |
| `TestResource.java` | Sample annotated resource (simulates JAX-RS) |
| `TestOrderResource.java` | Second sample resource for multi-resource testing |
| `NoAnnotationResource.java` | Unannotated class for negative testing |
| `annotations/*.java` | 7 test annotation definitions (TestPath, TestGet, etc.) |

## Class Relationships

```
AnnotationScanner
    ↑ uses
GenericAnnotationApiProvider implements ApiProvider
    ↑ delegates
JaxRsApiProvider implements ApiProvider
    ↑ uses
WsMethod, WsParameter (model)
```

## Coverage

- Line coverage: 87.9% (target: 80%)
- Branch coverage: adequate for all inference paths
- Test count: 56 tests across 4 test classes

## Test Strategy

1. **Unit tests** for each class in isolation
2. **Mock-based tests** using custom `AnnotationScanner` overrides to simulate namespace detection
3. **Integration tests** using test annotations that mimic JAX-RS patterns
4. **Negative tests** for absent annotations, null inputs, and unannotated classes

---

**Last Updated**: 2026-08-15
