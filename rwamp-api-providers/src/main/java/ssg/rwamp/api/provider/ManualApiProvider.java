package ssg.rwamp.api.provider;

import ssg.rwamp.api.provider.model.*;

import java.util.*;

/**
 * Builds API definitions programmatically via a fluent builder API.
 * <p>
 * Inspired by xLib's {@code DB_API_Builder}. This provider does not use
 * reflection — instead it provides a type-safe, fluent interface for
 * constructing API definitions at runtime. Ideal for:
 * <ul>
 *   <li>Wrapping third-party APIs that lack annotations</li>
 *   <li>Dynamically assembled APIs from configuration</li>
 *   <li>Testing and fixtures</li>
 * </ul>
 * <p>
 * Usage:
 * <pre>{@code
 * var api = ManualApiProvider.builder("my-api")
 *     .version("2.0.0")
 *     .description("My awesome service")
 *     .group("users", "User operations", g -> g
 *         .operation("get-user", "Get user by ID", op -> op
 *             .param("id", ApiDataType.INTEGER)
 *             .response(ApiDataType.fromClass(User.class))
 *         )
 *         .operation("list-users", "List all users", op -> op
 *             .param("page", ApiDataType.INTEGER, false)
 *             .response(ApiDataType.fromClass(List.class))
 *         )
 *     )
 *     .build();
 * }</pre>
 *
 * @since 0.1.0
 */
public class ManualApiProvider implements ApiProvider {

    private final ApiDefinition definition;

    ManualApiProvider(ApiDefinition definition) {
        this.definition = definition;
    }

    /**
     * Starts a new fluent builder for an API definition.
     */
    public static Builder builder(String name) {
        return new Builder(name);
    }

    @Override
    public boolean canHandle(Object target) {
        return target instanceof Map;
    }

    @Override
    public ApiDefinition build(Object target) {
        return definition;
    }

    @Override
    public String type() {
        return "manual";
    }

    // ── Fluent Builder ──

    /**
     * Fluent builder for {@link ApiDefinition}.
     */
    public static class Builder {
        private final String name;
        private String version = "1.0.0";
        private String description;
        private final LinkedHashMap<String, GroupBuilder> groups = new LinkedHashMap<>();
        private final LinkedHashMap<String, ApiDataType> types = new LinkedHashMap<>();
        private final LinkedHashMap<String, Object> extensions = new LinkedHashMap<>();

        Builder(String name) {
            this.name = Objects.requireNonNull(name);
        }

        public Builder version(String v) { this.version = v; return this; }
        public Builder description(String d) { this.description = d; return this; }
        public Builder extension(String key, Object value) { this.extensions.put(key, value); return this; }

        /**
         * Add a top-level group with operations.
         */
        public Builder group(String name, String desc, GroupBuilder.Configurator config) {
            var gb = new GroupBuilder(name, desc);
            if (config != null) config.configure(gb);
            groups.put(name, gb);
            return this;
        }

        /** Add a type definition */
        public Builder type(String name, ApiDataType type) {
            this.types.put(name, type);
            return this;
        }

        public ApiDefinition build() {
            var resolvedGroups = new LinkedHashMap<String, ApiGroup>();
            for (var gb : groups.values()) {
                resolvedGroups.put(gb.name(), gb.build());
            }
            return new ApiDefinition(name, version, description, resolvedGroups, types, extensions);
        }
    }

    /**
     * Fluent builder for {@link ApiGroup}.
     */
    public static class GroupBuilder {
        public interface Configurator { void configure(GroupBuilder gb); }

        private final String name;
        private final String description;
        private final LinkedHashMap<String, OperationBuilder> operations = new LinkedHashMap<>();
        private final List<String> tags = new ArrayList<>();

        String name() { return name; }

        GroupBuilder(String name, String description) {
            this.name = Objects.requireNonNull(name);
            this.description = description;
        }

        public GroupBuilder tag(String tag) { this.tags.add(tag); return this; }

        public GroupBuilder operation(String name, String desc, OperationBuilder.Configurator config) {
            var ob = new OperationBuilder(name);
            ob.description(desc);
            if (config != null) config.configure(ob);
            operations.put(name, ob);
            return this;
        }

        public ApiGroup build() {
            var resolvedOps = new LinkedHashMap<String, ApiOperation>();
            for (var ob : operations.values()) {
                resolvedOps.put(ob.name(), ob.build());
            }
            return new ApiGroup(name, description, resolvedOps, null, null, List.copyOf(tags));
        }
    }

    /**
     * Fluent builder for {@link ApiOperation}.
     */
    public static class OperationBuilder {
        public interface Configurator { void configure(OperationBuilder ob); }

        private final String name;
        private String description;
        private String summary;
        private String operationId;
        private final List<ApiParameter> parameters = new ArrayList<>();
        private ApiDataType response;
        private final List<ApiError> errors = new ArrayList<>();
        private final List<String> tags = new ArrayList<>();
        private boolean deprecated;
        private boolean hidden;
        private final Map<String, Object> extensions = new LinkedHashMap<>();

        OperationBuilder(String name) {
            this.name = Objects.requireNonNull(name);
            this.operationId = name;
        }

        public OperationBuilder description(String d) { this.description = d; return this; }
        public OperationBuilder summary(String s) { this.summary = s; return this; }
        public OperationBuilder operationId(String id) { this.operationId = id; return this; }
        public OperationBuilder deprecated(boolean v) { this.deprecated = v; return this; }
        public OperationBuilder hidden(boolean v) { this.hidden = v; return this; }

        public OperationBuilder param(String name, ApiDataType type) {
            parameters.add(ApiParameter.input(name, type));
            return this;
        }

        public OperationBuilder param(String name, ApiDataType type, boolean required) {
            parameters.add(new ApiParameter(name, type, ApiParameterKind.INPUT, required, null, null, null, null));
            return this;
        }

        public OperationBuilder response(ApiDataType type) {
            this.response = type;
            return this;
        }

        public OperationBuilder error(String code, String desc) {
            errors.add(ApiError.of(code, desc));
            return this;
        }

        public OperationBuilder tag(String tag) { this.tags.add(tag); return this; }
        public OperationBuilder extension(String key, Object value) { this.extensions.put(key, value); return this; }

        public ApiOperation build() {
            return new ApiOperation(name, description, summary, operationId,
                    List.copyOf(parameters), response, errors.isEmpty() ? null : List.copyOf(errors),
                    List.copyOf(tags), deprecated, hidden,
                    extensions.isEmpty() ? null : extensions);
        }

        String name() { return name; }
    }
}
