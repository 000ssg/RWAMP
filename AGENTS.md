# RWAMP Development Guide

This document describes the development practices, patterns, and conventions used in the RWAMP project.

## Project Overview

**RWAMP** is a WAMP v2 extension project built on top of **lego-flow's WAMP implementation**.
It re-uses lego-flow's core WAMP components (messages, session, broker, dealer, router,
serialization, auth, WebSocket transport) and adds production-grade features found in
xLib's WAMP implementation (session kill, testaments, virtual sessions, reflection API,
call timeout, statistics, REST bridge, etc.).

### Key Architectural Principle

**lego-flow's WAMP is the foundation.** RWAMP depends on `ssg:lego-flow-wamp` and extends
it. No WAMP-specific structures should be re-created — they are re-used from lego-flow.
xLib serves only as a reference for feature design.

### Dependency Management

RWAMP depends on lego-flow's WAMP module via **GitHub Packages** or **local Maven**:

```
ssg:lego-flow-wamp:0.2.0-SNAPSHOT
```

Both Maven and Gradle builds resolve from:
```
https://maven.pkg.github.com/000ssg/lego-flow
```

Authentication uses `GITHUB_ACTOR` / `GITHUB_TOKEN` environment variables.

For local development, install lego-flow to Maven local:
```bash
cd /path/to/lego-flow && mvn install -DskipTests
```

### Pre-Approved Build Commands

**Maven:**
```bash
mvn compile -DskipTests
mvn test
```

**Gradle:**
```bash
./gradlew compileJava
./gradlew test
```

### Structural Rules

1. **All packages under `ssg.rwamp`** — no conflict with lego-flow's `ssg.legoflow.wamp`
2. **Decorator/wrapper patterns** — extend lego-flow components without subclassing
3. **Each feature in its own module** — clean separation, independent compilation
4. **lego-flow WAMP is a dependency, not a sibling module** — always reference as `ssg:lego-flow-wamp`
5. **Published artifact names must be unique** on GitHub Packages (no `ssg.rwamp` groupId conflicts)
6. **Follow lego-flow's design patterns**: sealed interfaces, records, virtual threads, AssertJ assertions
7. **InMemoryTransport is local** — lego-flow's InMemoryTransport is in test sources and not
   available as a transitive dependency; create a local copy in each module's test sources
8. **WampMessage.Error is a 4-arg record**: `(requestType, requestId, details, error)` — no args field

---

## Development Practices

### 1. Requirements Documentation

**Primary Rule:** All requirements and design decisions MUST be tracked in `doc/plan/PLAN.md`
and per-phase documents in `doc/plan/PHASE_*.md`.

### 2. Git Commit Practices

#### Commit Message Format:
```
<Title: Brief summary (max 72 chars)>

<Detailed description of changes>

- Bullet points for key changes
```

#### Commit Workflow:
1. Stage changes: `git add <files>`
2. Commit with detailed message
3. Update plan tracking checkboxes in `doc/plan/` documents
4. **NEVER run `git push` automatically.** Inform the user and wait for explicit instruction.

### 3. Branch Strategy

| Branch | Purpose |
|--------|---------|
| `master` | Clean main branch |
| `prototype` | All development; implementation commits |

---

## Dependency on lego-flow

### Maven (pom.xml)

Root POM declares `lego-flow.version` property and `dependencyManagement`.
Each module POM depends on `ssg:lego-flow-wamp` without version (inherited from parent).
GitHub Packages repo in root POM for dependency resolution.

```xml
<properties>
    <lego-flow.version>0.2.0-SNAPSHOT</lego-flow.version>
</properties>

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>ssg</groupId>
            <artifactId>lego-flow-wamp</artifactId>
            <version>${lego-flow.version}</version>
        </dependency>
    </dependencies>
</dependencyManagement>

<repositories>
    <repository>
        <id>github-lego-flow</id>
        <url>https://maven.pkg.github.com/000ssg/lego-flow</url>
    </repository>
    <repository>
        <id>central</id>
        <url>https://repo1.maven.org/maven2/</url>
    </repository>
</repositories>
```

### Gradle (build.gradle.kts)

Root `build.gradle.kts` with subprojects configuration. **Critical:** use
`content { includeGroup("ssg") }` on the GitHub Packages repo to prevent
resolution failures for non-lego artifacts (Gradle tries all repos for every artifact).

```kotlin
subprojects {
    repositories {
        mavenLocal()
        mavenCentral()
        maven {
            name = "lego-flow-Packages"
            url = uri("https://maven.pkg.github.com/000ssg/lego-flow")
            content {
                includeGroup("ssg")
            }
            credentials {
                username = project.findProperty("gpr.user") as String?
                        ?: System.getenv("GITHUB_ACTOR")
                password = project.findProperty("gpr.key") as String?
                        ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }

    dependencies {
        "implementation"("ssg:lego-flow-wamp:0.2.0-SNAPSHOT")
    }
}
```

**Key:** `junitPlatformVersion = "1.11.4"` (not `5.11.4`) — junit-platform uses
separate versioning from junit-jupiter.

### Dual Build Verification

Both Maven and Gradle builds must pass before reporting changes as verified.

---

## Documentation & Graphics Guidelines

### Mermaid Graphics (REQUIRED)
Always use Mermaid diagrams instead of ASCII graphics. ASCII diagrams are deprecated
and should be replaced with Mermaid equivalents in all documentation files.

---

## Test Strategy

- **Feature tests** — one test class per feature, using local `InMemoryTransport`
- **Integration tests** — end-to-end flows using WebSocket transport from lego-flow
- **No duplicate tests for re-used components** — lego-flow tests cover the core
- **AssertJ assertions** — follow lego-flow's testing style
- **InMemoryTransport** — create a local copy in each module's test sources (lego-flow's
  is in test scope and unavailable as a transitive dependency)
- **Separate transport pairs** — when testing router interactions, use separate InMemoryTransport
  pairs for different sessions to avoid message cross-contamination

---

## Module Structure

Each RWAMP feature module:
- Has its own `pom.xml` (no `build.gradle.kts` — root build.gradle.kts configures subprojects)
- Depends on `ssg:lego-flow-wamp`
- Uses `ssg.rwamp.feature.<name>` package
- Has dedicated tests with local `InMemoryTransport`

See `doc/plan/PLAN.md` for the complete module dependency graph.

---

## New Module Template

When adding a new feature module (e.g., `rwamp-feature-testament`):

1. Create directory: `rwamp-feature-<name>/src/main/java/ssg/rwamp/feature/<name>/`
2. Create directory: `rwamp-feature-<name>/src/test/java/ssg/rwamp/feature/<name>/`
3. Add `pom.xml` (following `rwamp-feature-session/pom.xml` pattern)
4. Add to `settings.gradle.kts` include list
5. Copy `InMemoryTransport.java` from an existing module's test sources
6. Implement feature classes and tests
7. Verify: `mvn test` and `./gradlew test`
