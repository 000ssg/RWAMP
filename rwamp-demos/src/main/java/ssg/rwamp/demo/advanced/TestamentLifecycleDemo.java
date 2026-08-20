package ssg.rwamp.demo.advanced;

import ssg.legoflow.wamp.core.router.WampRouter;
import ssg.rwamp.feature.testament.TestamentApi;
import ssg.rwamp.feature.testament.TestamentManager;

import java.util.List;
import java.util.Map;

/**
 * Demonstrates the WAMP testament lifecycle: adding testaments,
 * flushing, and automatic publishing on session close.
 * <p>
 * Testaments are scheduled events that publish to a topic when
 * a session is detached or destroyed — useful for cleanup,
 * final announcements, or audit logging.
 *
 * @since 0.2.0
 */
public final class TestamentLifecycleDemo {

    private TestamentLifecycleDemo() {}

    public static void main(String[] args) {
        System.out.println("--- RWAMP Testament Lifecycle Demo ---");

        var manager = new TestamentManager();
        var router = new WampRouter();

        // Register testament procedures
        TestamentApi.register(router, manager);
        System.out.println("  Registered testament meta procedures:");
        System.out.println("    - " + TestamentApi.PROC_ADD_TESTAMENT);
        System.out.println("    - " + TestamentApi.PROC_FLUSH_TESTAMENT);

        long sessionId = 12345L;

        // Add testaments for destroyed scope
        manager.add(sessionId, "user.left", List.of("Alice"), Map.of(),
                    TestamentManager.SCOPE_DESTROYED, Map.of());
        manager.add(sessionId, "audit.logout", List.of("Charlie", "session_expired"), Map.of("priority", 10),
                    TestamentManager.SCOPE_DESTROYED, Map.of());
        System.out.println("\n  Added 2 testaments (scope=destroyed)");

        // Add testament for detached scope
        manager.add(sessionId, "user.disconnected", List.of("Bob"), Map.of(),
                    TestamentManager.SCOPE_DETACHED, Map.of());
        System.out.println("  Added 1 testament (scope=detached)");

        // Take all testaments (simulates collecting for publishing)
        var allTestaments = manager.takeAll(sessionId);
        System.out.println("\n  Total testaments: " + allTestaments.size());
        for (var entry : allTestaments) {
            System.out.println("    - " + entry.topic() + ": " + entry.args());
        }

        // Add new testaments
        manager.add(sessionId, "final.cleanup", List.of("Session " + sessionId), Map.of(),
                    TestamentManager.SCOPE_DESTROYED, Map.of());
        System.out.println("\n  Added final testament: final.cleanup");
        System.out.println("  Total testaments: " + manager.takeAll(sessionId).size());

        // Flush all (takeAll already consumed them)
        var remaining = manager.takeAll(sessionId);
        System.out.println("  Remaining after take: " + remaining.size());

        // Add fresh testaments for flush demo
        manager.add(sessionId, "before.flush", List.of("test"), Map.of(),
                    TestamentManager.SCOPE_DESTROYED, Map.of());
        System.out.println("  Added 1 testament before flush");
        manager.flush(sessionId, TestamentManager.SCOPE_DESTROYED);
        System.out.println("  Flushed destroyed scope");
        System.out.println("  Remaining after flush: " + manager.takeAll(sessionId).size());

        // Unregister procedures
        TestamentApi.unregister(router);
        System.out.println("\n  Unregistered testament meta procedures");

        System.out.println("\nTestament lifecycle demo completed.");
    }
}
