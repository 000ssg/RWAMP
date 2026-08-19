package ssg.rwamp.demo;

import ssg.rwamp.demo.advanced.CompositeScenarioDemo;
import ssg.rwamp.demo.advanced.DistributedRoutingDemo;
import ssg.rwamp.demo.advanced.MultiRouterDemo;
import ssg.rwamp.demo.advanced.MultiVersionApiDemo;
import ssg.rwamp.demo.advanced.TestamentLifecycleDemo;
import ssg.rwamp.demo.composite.MultiClientRpcDemo;
import ssg.rwamp.demo.composite.MultiRealmDemo;
import ssg.rwamp.demo.composite.MultiTopicPubSubDemo;
import ssg.rwamp.demo.composite.SharedRegistrationDemo;
import ssg.rwamp.demo.feature.ApiProvidersDemo;
import ssg.rwamp.demo.feature.ApiPublishersDemo;
import ssg.rwamp.demo.feature.RestBridgeDemo;
import ssg.rwamp.demo.feature.ReflectionDemo;
import ssg.rwamp.demo.feature.StatisticsDemo;
import ssg.rwamp.demo.feature.TestamentDemo;
import ssg.rwamp.demo.feature.VirtualSessionDemo;
import ssg.rwamp.demo.feature.WebServicesDemo;
import ssg.rwamp.demo.simple.SessionManagementDemo;
import ssg.rwamp.demo.simple.SimplePubSubDemo;
import ssg.rwamp.demo.simple.SimpleRpcDemo;

/**
 * Entry point for running all RWAMP demos.
 * <p>
 * Each demo is self-contained and can be run individually via its main method,
 * or all demos can be executed together through this runner.
 * <p>
 * Usage:
 * <pre>{@code
 * DemoRunner.runAll();
 * }</pre>
 *
 * @see SimpleRpcDemo
 * @see SimplePubSubDemo
 * @see SessionManagementDemo
 * @see MultiClientRpcDemo
 * @see MultiTopicPubSubDemo
 * @see MultiRealmDemo
 * @see SharedRegistrationDemo
 * @see TestamentDemo
 * @see ReflectionDemo
 * @see StatisticsDemo
 * @see VirtualSessionDemo
 * @see RestBridgeDemo
 * @see ApiProvidersDemo
 * @see ApiPublishersDemo
 * @see WebServicesDemo
 * @see MultiRouterDemo
 * @see MultiVersionApiDemo
 * @see CompositeScenarioDemo
 * @see DistributedRoutingDemo
 * @see TestamentLifecycleDemo
 *
 * @since 0.1.0
 */
public final class DemoRunner {

    private DemoRunner() {}

    /**
     * Runs all demos sequentially, printing results for each.
     */
    public static void runAll() {
        System.out.println("╔══════════════════════════════════════════════════════════╗");
        System.out.println("║              RWAMP Demo Suite — v0.1.0                  ║");
        System.out.println("║       Demonstrating WAMP v2 features with RWAMP         ║");
        System.out.println("╚══════════════════════════════════════════════════════════╝");
        System.out.println();

        // Simple demos
        run("Simple RPC", SimpleRpcDemo::main);
        run("Simple Pub/Sub", SimplePubSubDemo::main);
        run("Session Management", SessionManagementDemo::main);

        // Composite demos
        run("Multi-Client RPC", MultiClientRpcDemo::main);
        run("Multi-Topic Pub/Sub", MultiTopicPubSubDemo::main);
        run("Multi-Realm", MultiRealmDemo::main);
        run("Shared Registration", SharedRegistrationDemo::main);

        // Feature demos
        run("Testament", TestamentDemo::main);
        run("Reflection API", ReflectionDemo::main);
        run("Statistics", StatisticsDemo::main);
        run("Virtual Sessions", VirtualSessionDemo::main);
        run("REST Bridge", RestBridgeDemo::main);
        run("API Providers", ApiProvidersDemo::main);
        run("API Publishers", ApiPublishersDemo::main);
        run("Web Services", WebServicesDemo::main);

        // Advanced demos
        run("Multi-Router", MultiRouterDemo::main);
        run("Multi-Version API", MultiVersionApiDemo::main);
        run("Composite Scenario", CompositeScenarioDemo::main);
        run("Distributed Routing", DistributedRoutingDemo::main);
        run("Testament Lifecycle", TestamentLifecycleDemo::main);

        System.out.println();
        System.out.println("All demos completed.");
    }

    /**
     * Run a single demo by name.
     *
     * @param name the demo name
     */
    public static void run(String name) {
        switch (name.toLowerCase()) {
            case "rpc" -> SimpleRpcDemo.main(new String[0]);
            case "pubsub" -> SimplePubSubDemo.main(new String[0]);
            case "session" -> SessionManagementDemo.main(new String[0]);
            case "multi-client" -> MultiClientRpcDemo.main(new String[0]);
            case "multi-topic" -> MultiTopicPubSubDemo.main(new String[0]);
            case "multi-realm" -> MultiRealmDemo.main(new String[0]);
            case "shared" -> SharedRegistrationDemo.main(new String[0]);
            case "testament" -> TestamentDemo.main(new String[0]);
            case "reflection" -> ReflectionDemo.main(new String[0]);
            case "statistics" -> StatisticsDemo.main(new String[0]);
            case "virtual" -> VirtualSessionDemo.main(new String[0]);
            case "rest" -> RestBridgeDemo.main(new String[0]);
            case "providers" -> ApiProvidersDemo.main(new String[0]);
            case "publishers" -> ApiPublishersDemo.main(new String[0]);
            case "webservices" -> WebServicesDemo.main(new String[0]);
            case "multi-router" -> MultiRouterDemo.main(new String[0]);
            case "multi-version" -> MultiVersionApiDemo.main(new String[0]);
            case "composite" -> CompositeScenarioDemo.main(new String[0]);
            case "distributed" -> DistributedRoutingDemo.main(new String[0]);
            case "testament-lifecycle" -> TestamentLifecycleDemo.main(new String[0]);
            default -> System.out.println("Unknown demo: " + name);
        }
    }

    @FunctionalInterface
    private interface MainRunner {
        void run(String[] args);
    }

    private static void run(String name, MainRunner runner) {
        System.out.println("--- " + name + " ---");
        try {
            runner.run(new String[0]);
        } catch (Exception e) {
            System.err.println("  ERROR: " + e.getMessage());
        }
        System.out.println();
    }

    /**
     * Main entry point.
     */
    public static void main(String[] args) {
        if (args.length > 0) {
            run(args[0]);
        } else {
            runAll();
        }
    }
}
