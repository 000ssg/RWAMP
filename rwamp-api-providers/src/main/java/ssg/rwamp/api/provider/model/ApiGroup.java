package ssg.rwamp.api.provider.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A named container for API operations, types, and sub-groups.
 * <p>
 * Inspired by xLib's {@code APIGroup}. Provides hierarchical organization
 * of operations (e.g. "UserService" containing "getUser", "createUser").

 * @param name        group identifier
 * @param description human-readable description
 * @param operations  operations in this group (never null, may be empty)
 * @param types       data type definitions (never null, may be empty)
 * @param subGroups   nested groups (never null, may be empty)
 * @param tags        grouping labels (never null, may be empty)
 */
public record ApiGroup(
        String name,
        String description,
        Map<String, ApiOperation> operations,
        Map<String, ApiDataType> types,
        List<ApiGroup> subGroups,
        List<String> tags
) {
    public ApiGroup {
        if (operations == null) operations = new LinkedHashMap<>();
        if (types == null) types = new LinkedHashMap<>();
        if (subGroups == null) subGroups = Collections.emptyList();
        if (tags == null) tags = Collections.emptyList();
    }

    /** Returns the fully-qualified name (dot-separated scope + name) */
    public String fqn(String... scope) {
        if (scope == null || scope.length == 0) return name;
        StringBuilder sb = new StringBuilder();
        for (String s : scope) {
            if (sb.length() > 0) sb.append('.');
            sb.append(s);
        }
        sb.append('.');
        sb.append(name);
        return sb.toString();
    }

    /** All operations flattened from this group and sub-groups */
    public List<ApiOperation> allOperations() {
        Set<ApiOperation> result = new LinkedHashSet<>(operations.values());
        for (ApiGroup sub : subGroups) {
            result.addAll(sub.allOperations());
        }
        return List.copyOf(result);
    }

    /** Returns true if this group has no operations, types, or sub-groups */
    public boolean isEmpty() {
        return operations.isEmpty() && types.isEmpty() && subGroups.isEmpty();
    }
}
