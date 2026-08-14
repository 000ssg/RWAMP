# rwamp-demos — Requirements

## Commit: Initial — Demos Module (2026-08-14)

### Original Request
> "add 'demos' module to illustrate RWAMP intended usage scenarios, include there simple and composite variants (including multiple routers, multiple clients publishing multiple methods and topics and consuming. same node may connect to multiple routers and multiple realms. same procedure may be published on by several callers and invoked in different modes including sharded). demonstrate how to invoke same procedure via WAMP and REST. demonstrate how multiple versions of same API might be published on WAMP and via REST over WAMP. add reasonable and clearly described usage scenarios with explained pros and cons (resources usage, delays, reliability and any other related aspects). also demonstrate usage of different transports, mixed roles (router with other roles to allow inter-router communication). document demos on review and in detail so it was easy to find use cases and judge of pros/cons and any side effects."

### Reformulated Requirements
1. Create `rwamp-demos` module with simple, composite, feature, and advanced demo categories
2. Simple demos: basic RPC, basic pub/sub, session management
3. Composite demos: multi-client RPC with shared registration, multi-topic pub/sub with pattern matching, multi-realm isolation, shared registration with all invoke policies
4. Feature demos: testament, reflection API, statistics, virtual sessions, REST bridge
5. Advanced demos: multi-router with cross-realm rerouting, multi-version API (WAMP + REST), composite scenario combining all features
6. Each demo must include detailed Javadoc with pros/cons, resource usage, and trade-offs
7. Comprehensive README with scenario descriptions, transport comparison, architecture patterns
8. DemoRunner entry point for running all or individual demos
9. Module excluded from Maven publishing and JaCoCo coverage

### Final Design Decisions
- **InMemoryTransport used throughout** — ensures deterministic, fast execution without network dependencies
- **One demo per class** — clear separation of concerns, independent execution
- **Typed result records** — each demo returns structured results for verification
- **main() in each class** — standalone execution for individual demos
- **DemoRunner orchestrator** — single entry point for running all demos
- **No tests** — demos are executable examples, not unit tests
- **Excluded from publishing** — demos module is reference material, not a library

### Implementation Details
- Files created: 17 (14 demos + DemoRunner + InMemoryTransport)
- Packages: `ssg.rwamp.demo.simple`, `.composite`, `.feature`, `.advanced`, `.infrastructure`
- Module build config: `build.gradle.kts` (depends on all RWAMP modules)
- Root `build.gradle.kts` updated: excluded demos from JaCoCo and Maven publish
- `settings.gradle.kts` updated: added `rwamp-demos` to include list

### Cost Estimate
| Metric | Value |
|--------|-------|
| Files created | 17 |
| Lines added | ~3200 |
| Tests added | 0 (demos are not tests) |
| Demos implemented | 14 |
| Documentation files | 4 (README, ARCHITECTURE, REQUIREMENTS, CODE_OVERVIEW) |
