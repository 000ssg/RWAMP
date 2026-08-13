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

RWAMP depends on lego-flow's WAMP module via **GitHub Packages**:

```
ssg:lego-flow-wamp:0.2.0-SNAPSHOT
```

Both Maven and Gradle builds resolve from:
```
https://maven.pkg.github.com/000ssg/lego-flow
```

Authentication uses `GITHUB_ACTOR` / `GITHUB_TOKEN` environment variables.

### Pre-Approved Build Commands

**Maven:**
```bash
mvn compile -DskipTests
mvn test
```

**Gradle:**
```bash
./gradlew test
```

### Structural Rules

1. **All packages under `ssg.rwamp`** — no conflict with lego-flow's `ssg.legoflow.wamp`
2. **Decorator/wrapper patterns** — extend lego-flow components without subclassing
3. **Each feature in its own module** — clean separation, independent compilation
4. **lego-flow WAMP is a dependency, not a sibling module** — always reference as `ssg:lego-flow-wamp`
5. **Published artifact names must be unique** on GitHub Packages (no `ssg.rwamp` groupId conflicts)
6. **Follow lego-flow's design patterns**: sealed interfaces, records, virtual threads, AssertJ assertions

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

Co-Authored-By: AI assistant
```

#### Commit Workflow:
1. Stage changes: `git add <files>`
2. Commit with detailed message
3. Always include `Co-Authored-By: AI assistant`
4. Update plan tracking checkboxes in `doc/plan/` documents
5. **NEVER run `git push` automatically.** Inform the user and wait for explicit instruction.

### 3. Branch Strategy

| Branch | Purpose |
|--------|---------|
| `master` | Clean main branch |
| `prototype` | All development; implementation commits |

---

## Dependency on lego-flow

### Maven (pom.xml)

```xml
<properties>
    <lego-flow.version>0.2.0-SNAPSHOT</lego-flow.version>
</properties>

<dependencies>
    <dependency>
        <groupId>ssg</groupId>
        <artifactId>lego-flow-wamp</artifactId>
        <version>${lego-flow.version}</version>
    </dependency>
</dependencies>

<repositories>
    <repository>
        <id>github-lego-flow</id>
        <url>https://maven.pkg.github.com/000ssg/lego-flow</url>
    </repository>
</repositories>
```

### Gradle (build.gradle.kts)

```kotlin
repositories {
    maven {
        name = "lego-flow-Packages"
        url = uri("https://maven.pkg.github.com/000ssg/lego-flow")
        credentials {
            username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_ACTOR")
            password = project.findProperty("gpr.key") as String? ?: System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencies {
    implementation("ssg:lego-flow-wamp:${property("legoFlowVersion")}")
}
```

### Dual Build Verification

Both Maven and Gradle builds must pass before reporting changes as verified.

---

## Documentation & Graphics Guidelines

### Mermaid Graphics (REQUIRED)
Always use Mermaid diagrams instead of ASCII graphics. ASCII diagrams are deprecated
and should be replaced with Mermaid equivalents in all documentation files.

---

## Test Strategy

- **Feature tests** — one test class per feature, using `InMemoryTransport` from lego-flow
- **Integration tests** — end-to-end flows using WebSocket transport from lego-flow
- **No duplicate tests for re-used components** — lego-flow tests cover the core
- **AssertJ assertions** — follow lego-flow's testing style

---

## Module Structure

Each RWAMP feature module:
- Has its own `pom.xml` and `build.gradle.kts`
- Depends on `ssg:lego-flow-wamp`
- Uses `ssg.rwamp.feature.<name>` package
- Has dedicated tests

See `doc/plan/PLAN.md` for the complete module dependency graph.
