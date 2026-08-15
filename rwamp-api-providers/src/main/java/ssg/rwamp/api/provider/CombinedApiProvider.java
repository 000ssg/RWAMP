package ssg.rwamp.api.provider;

import ssg.rwamp.api.provider.model.*;

import java.util.*;

/**
 * Aggregates multiple {@link ApiProvider} instances and merges their output.
 * <p>
 * When {@link #build(Object)} is called, each provider is checked with
 * {@link ApiProvider#canHandle(Object)} and the results are merged into
 * a single {@link ApiDefinition}.
 * <p>
 * Useful for combining annotation-based discovery with manual overrides
 * or convention-based providers.
 *
 * @since 0.1.0
 */
public class CombinedApiProvider implements ApiProvider {

    private final String apiName;
    private final String version;
    private final List<ApiProvider> providers = new ArrayList<>();

    public CombinedApiProvider(String apiName) {
        this(apiName, "1.0.0");
    }

    public CombinedApiProvider(String apiName, String version) {
        this.apiName = Objects.requireNonNull(apiName);
        this.version = version;
    }

    public CombinedApiProvider addProvider(ApiProvider provider) {
        if (provider != null) providers.add(provider);
        return this;
    }

    public CombinedApiProvider addProviders(Iterable<ApiProvider> providers) {
        if (providers != null) providers.forEach(this::addProvider);
        return this;
    }

    @Override
    public boolean canHandle(Object target) {
        return providers.stream().anyMatch(p -> p.canHandle(target));
    }

    @Override
    public ApiDefinition build(Object target) {
        var mergedGroups = new LinkedHashMap<String, ApiGroup>();
        var mergedTypes = new LinkedHashMap<String, ApiDataType>();

        for (ApiProvider provider : providers) {
            if (!provider.canHandle(target)) continue;
            ApiDefinition def = provider.build(target);

            // Merge groups
            for (var entry : def.groups().entrySet()) {
                String gName = entry.getKey();
                ApiGroup existing = mergedGroups.get(gName);
                if (existing == null) {
                    mergedGroups.put(gName, entry.getValue());
                } else {
                    // Merge operations within the same group
                    var mergedOps = new LinkedHashMap<String, ApiOperation>(existing.operations());
                    mergedOps.putAll(entry.getValue().operations());
                    mergedGroups.put(gName, new ApiGroup(
                            existing.name(), existing.description(), mergedOps,
                            mergeMaps(existing.types(), entry.getValue().types()),
                            List.of(), mergeLists(existing.tags(), entry.getValue().tags())
                    ));
                }
            }

            // Merge types
            mergedTypes.putAll(def.types());
        }

        return new ApiDefinition(apiName, version, null, mergedGroups, mergedTypes, Map.of());
    }

    private <K, V> Map<K, V> mergeMaps(Map<K, V> a, Map<K, V> b) {
        if (a == null && b == null) return Map.of();
        var result = new LinkedHashMap<K, V>();
        if (a != null) result.putAll(a);
        if (b != null) result.putAll(b);
        return result;
    }

    private List<String> mergeLists(List<String> a, List<String> b) {
        var result = new LinkedHashSet<String>();
        if (a != null) result.addAll(a);
        if (b != null) result.addAll(b);
        return List.copyOf(result);
    }

    @Override
    public String type() {
        return "combined";
    }
}
