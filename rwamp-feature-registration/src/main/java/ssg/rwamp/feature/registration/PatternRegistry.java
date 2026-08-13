package ssg.rwamp.feature.registration;

import ssg.legoflow.wamp.core.transport.WampTransport;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Registry for pattern-based procedure registrations.
 * <p>
 * Stores registrations matched by pattern type (exact, prefix, wildcard) and
 * routes incoming procedure URIs to the best matching registration. Unlike
 * lego-flow's Dealer which only supports exact matching, this registry allows
 * a callee to register under a pattern like {@code com.example.*} or
 * {@code com.example.}.
 * <p>
 * Thread-safe for concurrent access.
 *
 * @since 0.1.0
 */
public class PatternRegistry {

    private final AtomicLong nextId = new AtomicLong(1);

    /** Match type -> list of entries */
    private final Map<String, List<PatternEntry>> byMatchType = new ConcurrentHashMap<>();

    /** registration ID -> entry (for fast lookup by ID) */
    private final Map<Long, PatternEntry> byId = new ConcurrentHashMap<>();

    private PatternRegistry() {
        byMatchType.put("exact", List.of());
        byMatchType.put("prefix", List.of());
        byMatchType.put("wildcard", List.of());
    }

    /**
     * Creates a new empty registry.
     */
    public static PatternRegistry create() {
        return new PatternRegistry();
    }

    /**
     * Registers a procedure under a pattern.
     *
     * @param procedure   the procedure URI (may contain wildcards for wildcard/prefix matching)
     * @param matchType   one of "exact", "prefix", "wildcard"
     * @param invokePolicy invoke policy (e.g., "single", "roundrobin")
     * @param transport   the callee's transport
     * @return the new registration ID
     */
    public long register(String procedure, String matchType, String invokePolicy,
                         WampTransport transport) {
        long regId = nextId.getAndIncrement();
        var entry = new PatternEntry(regId, procedure, matchType, invokePolicy, transport);
        byId.put(regId, entry);

        var current = byMatchType.computeIfAbsent(matchType, k -> new java.util.ArrayList<>());
        var list = new java.util.ArrayList<>(current);
        list.add(entry);
        byMatchType.put(matchType, List.copyOf(list));

        return regId;
    }

    /**
     * Removes a registration by ID.
     *
     * @param regId the registration ID
     * @return {@code true} if the registration was found and removed
     */
    public boolean unregister(long regId) {
        var entry = byId.remove(regId);
        if (entry == null) return false;

        byMatchType.compute(entry.matchType(), (key, list) -> {
            if (list == null) return null;
            var filtered = list.stream()
                    .filter(e -> e.regId() != regId)
                    .collect(Collectors.toList());
            return filtered.isEmpty() ? List.of() : filtered;
        });

        return true;
    }

    /**
     * Finds the best matching registration for a procedure URI.
     * <p>
     * Searches in priority order: exact > prefix > wildcard.
     *
     * @param procedure  the procedure URI to match
     * @param matchType  the match policy to use ("exact", "prefix", "wildcard", or null for all)
     * @return the best matching entry, or empty
     */
    public Optional<PatternEntry> find(String procedure, String matchType) {
        if ("exact".equals(matchType) || matchType == null) {
            var exact = findIn("exact", procedure);
            if (exact.isPresent()) return exact;
        }

        if ("prefix".equals(matchType) || matchType == null) {
            var prefix = findIn("prefix", procedure);
            if (prefix.isPresent()) return prefix;
        }

        if ("wildcard".equals(matchType) || matchType == null) {
            var wildcard = findIn("wildcard", procedure);
            if (wildcard.isPresent()) return wildcard;
        }

        return Optional.empty();
    }

    /**
     * Finds the matching registration within a single match type.
     */
    Optional<PatternEntry> findIn(String matchType, String procedure) {
        var entries = byMatchType.get(matchType);
        if (entries == null || entries.isEmpty()) return Optional.empty();

        for (var entry : entries) {
            if (PatternMatcher.matches(entry.matchType(), entry.pattern(), procedure)) {
                return Optional.of(entry);
            }
        }
        return Optional.empty();
    }

    /**
     * Returns the entry for a registration ID, if any.
     */
    public Optional<PatternEntry> getById(long regId) {
        return Optional.ofNullable(byId.get(regId));
    }

    /**
     * Returns all registered entries.
     */
    public List<PatternEntry> getAll() {
        return List.copyOf(byId.values());
    }

    /**
     * Returns the number of registered patterns.
     */
    public int size() {
        return byId.size();
    }

    /**
     * A single pattern-based registration entry.
     *
     * @param regId        the unique registration ID
     * @param pattern      the registered procedure URI (may contain wildcards)
     * @param matchType    the match type: "exact", "prefix", or "wildcard"
     * @param invokePolicy invoke policy for the registration
     * @param transport    the callee's transport
     */
    public record PatternEntry(long regId, String pattern, String matchType,
                               String invokePolicy, WampTransport transport) {
    }
}
