package ssg.rwamp.api.provider.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Top-level API definition — a named collection of groups, operations, and types.
 * <p>
 * Inspired by xLib's {@code API}. This is the root container that an
 * {@link ApiProvider} produces from reflection or manual definition.
 * <p>
 * Usage:
 * <pre>{@code
 * var api = new ApiDefinition("my-api", "1.0.0", "My Service API",
 *     Map.of("users", new ApiGroup("users", null, Map.of(...), null, null, null)),
 *     null, null);
 * }</pre>
 *
 * @param name        API identifier
 * @param version     semantic version string
 * @param description human-readable description
 * @param groups      top-level groups (sorted by insertion order)
 * @param types       global type definitions
 * @param extensions  arbitrary metadata (provider, contact, license, etc.)
 */
public record ApiDefinition(
        String name,
        String version,
        String description,
        Map<String, ApiGroup> groups,
        Map<String, ApiDataType> types,
        Map<String, Object> extensions
) {
    public ApiDefinition {
        if (groups == null) groups = new LinkedHashMap<>();
        if (types == null) types = new LinkedHashMap<>();
        if (extensions == null) extensions = Map.of();
    }

    /** All operations flattened from all groups */
    public List<ApiOperation> allOperations() {
        var result = new java.util.LinkedHashSet<ApiOperation>();
        for (ApiGroup group : groups.values()) {
            result.addAll(group.allOperations());
        }
        return List.copyOf(result);
    }

    /** Find an operation by fully-qualified name */
    public ApiOperation findOperation(String fqn) {
        for (ApiGroup group : groups.values()) {
            for (ApiOperation op : group.allOperations()) {
                if (op.name().equals(fqn) || (group.name() + "." + op.name()).equals(fqn)) {
                    return op;
                }
            }
        }
        return null;
    }

    /** Returns true if this definition has no content */
    public boolean isEmpty() {
        return groups.values().stream().allMatch(ApiGroup::isEmpty) && types.isEmpty();
    }
}
