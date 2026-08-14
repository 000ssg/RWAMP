package ssg.rwamp.feature.reflection;

import ssg.legoflow.wamp.core.router.Broker;
import ssg.legoflow.wamp.core.router.Dealer;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Provides live introspection of WAMP router state.
 * <p>
 * Reads procedure and topic information directly from lego-flow's
 * {@link Dealer} and {@link Broker}. No separate state tracking —
 * queries the underlying components at call time.
 * <p>
 * Usage:
 * <pre>{@code
 * var registry = new ReflectionRegistry(dealer, broker);
 * var procedures = registry.getProcedures(); // live view
 * var topics = registry.getTopics();         // live view
 * }</pre>
 *
 * @since 0.1.0
 */
public class ReflectionRegistry {

    private final Dealer dealer;
    private final Broker broker;
    /** user-defined type/error definitions (not from protocol) */
    private final Map<String, Map<String, Object>> definitions = new LinkedHashMap<>();

    /**
     * Creates a new registry backed by the given Dealer and Broker.
     *
     * @param dealer the WAMP dealer
     * @param broker the WAMP broker
     */
    public ReflectionRegistry(Dealer dealer, Broker broker) {
        this.dealer = dealer;
        this.broker = broker;
    }

    /**
     * Returns the set of all registered procedure URIs.
     * Reads live from the Dealer.
     *
     * @return unmodifiable set of procedure names
     */
    public Set<String> getProcedures() {
        return dealer.getRegisteredProcedures();
    }

    /**
     * Returns procedure details: name -> {callees, invoke_policy}.
     *
     * @return map of procedure metadata
     */
    public Map<String, Map<String, Object>> getProcedureDetails() {
        var result = new LinkedHashMap<String, Map<String, Object>>();
        for (String proc : dealer.getRegisteredProcedures()) {
            int count = dealer.getRegistrationCount(proc);
            result.put(proc, Map.of("callees", count));
        }
        return result;
    }

    /**
     * Returns the set of all topics with active exact-match subscriptions.
     * Reads live from the Broker.
     *
     * @return unmodifiable set of topic URIs
     */
    public Set<String> getTopics() {
        return broker.getSubscriptionTopics();
    }

    /**
     * Returns topic details: name -> {subscribers, retained}.
     *
     * @return map of topic metadata
     */
    public Map<String, Map<String, Object>> getTopicDetails() {
        var result = new LinkedHashMap<String, Map<String, Object>>();
        for (String topic : broker.getSubscriptionTopics()) {
            int count = broker.getSubscriptionCount(topic);
            boolean retained = broker.getRetainedEvent(topic) != null;
            result.put(topic, Map.of("subscribers", count, "retained", retained));
        }
        return result;
    }

    /**
     * Returns the set of prefix subscription patterns.
     */
    public Set<String> getPrefixTopics() {
        return broker.getPrefixSubscriptionPatterns();
    }

    /**
     * Returns the set of wildcard subscription patterns.
     */
    public Set<String> getWildcardTopics() {
        return broker.getWildcardSubscriptionPatterns();
    }

    /**
     * Stores a user-defined type or error definition.
     *
     * @param name     the type/error URI
     * @param metadata the definition metadata
     */
    public void define(String name, Map<String, Object> metadata) {
        definitions.put(name, metadata);
    }

    /**
     * Removes a definition.
     *
     * @param name the type/error URI
     */
    public void undefine(String name) {
        definitions.remove(name);
    }

    /**
     * Returns all user-defined definitions.
     *
     * @return unmodifiable map of definitions
     */
    public Map<String, Map<String, Object>> getDefinitions() {
        return Map.copyOf(definitions);
    }
}
