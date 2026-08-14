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

RWAMP version is **0.1.0-SNAPSHOT** (lego-flow stays at 0.2.0-SNAPSHOT).

For local development, install lego-flow to Maven local:
```bash
cd /path/to/lego-flow && mvn install -DskipTests
```

### Pre-Approved Build Commands

**Gradle (primary):**
```bash
./gradlew compileJava
./gradlew test
./gradlew clean test jacocoAggregateVerification --no-daemon
```

**Maven (secondary):**
```bash
mvn compile -DskipTests
mvn test
```

### Structural Rules

1. **All packages under `ssg.rwamp`** — no conflict with lego-flow's `ssg.legoflow.wamp`
2. **Decorator/wrapper patterns** — extend lego-flow components without subclassing
3. **Each feature in its own module** — clean separation, independent compilation
4. **lego-flow WAMP is a dependency, not a sibling module** — always reference as `ssg:lego-flow-wamp`
5. **Published artifact names must be unique** on GitHub Packages
6. **Follow lego-flow's design patterns**: sealed interfaces, records, virtual threads, AssertJ assertions
7. **InMemoryTransport is local** — lego-flow's InMemoryTransport is in test sources and not
   available as a transitive dependency; create a local copy in each module's test sources
8. **WampMessage.Error is a 4-arg record**: `(requestType, requestId, details, error)` — no args field

---

## Development Practices

### 1. Requirements Documentation

**Primary Rule:** All requirements, design decisions, and their evolution MUST be tracked in `doc/REQUIREMENTS.md`.

#### When Adding Features:
1. **Document Original Request**: Add verbatim user request at the start of each commit section
2. **Reformulate Requirements**: List clear, specific technical requirements
3. **Final Design Decisions**: Document what was chosen and why
4. **Implementation Details**: List files changed, features added
5. **Test Coverage**: Document tests added and total test count

#### REQUIREMENTS.md Structure:
```markdown
## Commit: `<hash>` - <Feature Name> (Date)

### Original Request
> "verbatim user request from conversation or discussion transcript"

### Reformulated Requirements
1. Specific technical requirement
2. Another requirement
...

### Final Design Decisions
- Architectural choices with rationale
- Trade-offs considered

### Implementation Details
- Files modified/created
- Key features implemented

### Test Coverage
- New tests added
- Total tests passing

### Cost Estimate
| Metric | Value |
|--------|-------|
| Files created/modified | N |
| Lines added/removed | +A / -B |
| Tests added | N (total: M) |
```

### 2. Architecture Documentation

**ARCHITECTURE.md** (`doc/ARCHITECTURE.md`) documents the **current** set of architectural decisions.

- **Mandatory update** on every commit with architectural changes
- Unlike REQUIREMENTS.md (append-only, historical), ARCHITECTURE.md is **edited in place** to reflect the latest state
- Sections: module purpose, key abstractions, design patterns, data flow, extension points, thread safety model

### 3. Code Overview Documentation

**CODE_OVERVIEW.md** (`doc/CODE_OVERVIEW.md`) documents the source structure and key classes.

- Maintained at the root level and per-module level
- Lists source structure with file-by-file descriptions
- Documents key abstractions and design decisions
- Cross-references README, Architecture, and Requirements

### 4. Compliance Documentation

**COMPLIANCE.md** (`doc/COMPLIANCE.md`) documents WAMP specification compliance per module.

- Lists WAMP spec sections implemented
- Notes known deviations
- Per-module compliance files in each module's `doc/` directory

### 5. Git Commit Practices

#### Commit Message Format:
```
<Title: Brief summary (max 72 chars)>

<Detailed description of changes>

- Bullet points for key changes
- Implementation highlights
- Test additions

Co-Authored-By: AI assistant
```

#### Commit Workflow:
1. Stage changes: `git add <files>`
2. Commit with detailed message using heredoc for proper formatting
3. Always include `Co-Authored-By: AI assistant`
4. **Update doc/REQUIREMENTS.md** with commit documentation
5. **Update doc/ARCHITECTURE.md** if architectural changes were made
6. **Update README.md**: reflect any API changes, new features, updated module structure, version badges, test counts
7. **NEVER run `git push` automatically.** Inform the user and wait for explicit instruction.

> **MANDATORY DOCUMENTATION RULE**: Steps 4–6 are required on every commit with code changes.
> Documentation-first development: requirements are documented before implementation.

---

## Documentation & Graphics Guidelines

### Mermaid Graphics (REQUIRED)
Always use Mermaid diagrams instead of ASCII graphics. ASCII diagrams are deprecated and should be replaced with Mermaid equivalents in all documentation files (README.md, doc/*.md, AGENTS.md).

Use Mermaid `graph TD` or `graph LR` for architecture diagrams. Use Mermaid sequence diagrams for protocol flows. Mermaid is supported natively by GitHub, GitLab, VS Code, and all major markdown renderers.

### Documentation Update Checklist
Before committing changes that affect code structure:
1. Update README.md: Performance section, module table, architecture diagram, test counts
2. Update doc/ARCHITECTURE.md: Update Mermaid diagrams if module structure changed
3. Update doc/REQUIREMENTS.md: Add commit entry with all sections
4. Update doc/CODE_OVERVIEW.md: Update source structure if files changed
5. Update per-module docs: README, ARCHITECTURE, REQUIREMENTS, COMPLIANCE, CODE_OVERVIEW

### Per-Module Documentation Structure
Each RWAMP module must have:
```
<module>/
├── README.md                    — module overview, usage, API table, cross-refs
└── doc/
    ├── ARCHITECTURE.md          — module purpose, abstractions, data flow, thread safety
    ├── REQUIREMENTS.md          — requirements timeline, phase, test count
    ├── CODE_OVERVIEW.md         — source structure, key classes, design notes
    └── COMPLIANCE.md            — WAMP spec compliance, known deviations
```

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
- **JaCoCo coverage** — aggregate target 80%+, per-module minimum 50%

---

## Module Structure

Each RWAMP feature module:
- Has its own `pom.xml` and is configured via root `build.gradle.kts`
- Depends on `ssg:lego-flow-wamp`
- Uses `ssg.rwamp.<category>.<name>` package
- Has dedicated tests with local `InMemoryTransport`
- Has `README.md` and `doc/` directory with ARCHITECTURE, REQUIREMENTS, CODE_OVERVIEW, COMPLIANCE

See [doc/plan/PLAN.md](doc/plan/PLAN.md) for the complete module dependency graph.

---

## New Module Template

When adding a new feature module (e.g., `rwamp-feature-<name>`):

1. Create directory: `rwamp-feature-<name>/src/main/java/ssg/rwamp/feature/<name>/`
2. Create directory: `rwamp-feature-<name>/src/test/java/ssg/rwamp/feature/<name>/`
3. Create directory: `rwamp-feature-<name>/doc/`
4. Add `pom.xml` (following `rwamp-feature-session/pom.xml` pattern)
5. Add to `settings.gradle.kts` include list
6. Copy `InMemoryTransport.java` from an existing module's test sources
7. Create `README.md` with usage, API table, cross-refs
8. Create `doc/ARCHITECTURE.md`, `doc/REQUIREMENTS.md`, `doc/CODE_OVERVIEW.md`, `doc/COMPLIANCE.md`
9. Implement feature classes and tests
10. Verify: `./gradlew test jacocoAggregateVerification --no-daemon`

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
        "implementation"("ssg:lego-flow-wamp:$legoFlowVersion")
    }
}
```

**Key:** `junitPlatformVersion = "1.11.4"` (not `5.11.4`) — junit-platform uses
separate versioning from junit-jupiter.

### Dual Build Verification

Both Maven and Gradle builds must pass before reporting changes as verified.

---

## Branch Strategy

| Branch | Purpose |
|--------|---------|
| `master` | Clean main branch |
| `prototype` | All development; implementation commits |

---

## Versioning

| Project | Version |
|---------|---------|
| lego-flow | 0.2.0-SNAPSHOT (upstream, not changed by RWAMP) |
| RWAMP | 0.1.0-SNAPSHOT |

