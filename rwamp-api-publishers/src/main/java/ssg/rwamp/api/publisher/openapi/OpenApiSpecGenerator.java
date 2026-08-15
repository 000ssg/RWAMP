package ssg.rwamp.api.publisher.openapi;

import ssg.rwamp.api.provider.model.*;

import java.util.*;
import java.util.stream.Stream;

/**
 * Generates OpenAPI 3.1.x compliant JSON from an {@link ApiDefinition}.
 * <p>
 * Produces a spec that can be consumed by Swagger UI, Redoc, or any
 * OpenAPI-compatible tooling.
 * <p>
 * The generator:
 * <ul>
 *   <li>Maps API groups to OpenAPI tags</li>
 *   <li>Maps operations to path items with HTTP methods</li>
 *   <li>Maps parameters to OpenAPI parameter objects</li>
 *   <li>Maps data types to JSON Schema (OpenAPI 3.1 uses pure JSON Schema)</li>
 *   <li>Includes security requirements from access annotations</li>
 *   <li>Resolves REST paths from {@code @ApiService.path} + {@code @Operation.path}</li>
 * </ul>
 *
 * @since 0.1.0
 */
public class OpenApiSpecGenerator {

    private String serverUrl = "/";
    private String contactName;
    private String contactEmail;
    private String licenseName;
    private List<String> securitySchemeNames = List.of("bearerAuth");
    private boolean securityEnabled = true;

    public OpenApiSpecGenerator serverUrl(String url) { this.serverUrl = url; return this; }
    public OpenApiSpecGenerator contact(String name, String email) { this.contactName = name; this.contactEmail = email; return this; }
    public OpenApiSpecGenerator license(String name) { this.licenseName = name; return this; }

    /** Enable or disable automatic security scheme inclusion */
    public OpenApiSpecGenerator securityEnabled(boolean enabled) { this.securityEnabled = enabled; return this; }

    /** Set custom security scheme names */
    public OpenApiSpecGenerator securitySchemeNames(List<String> names) { this.securitySchemeNames = names != null ? names : List.of(); return this; }

    /**
     * Generate OpenAPI 3.1 JSON as a Map (ready for serialization).
     */
    public Map<String, Object> generate(ApiDefinition def) {
        var spec = new LinkedHashMap<String, Object>();
        spec.put("openapi", "3.1.0");

        // Info
        var info = new LinkedHashMap<String, Object>();
        info.put("title", def.name());
        info.put("version", def.version());
        if (def.description() != null) info.put("description", def.description());
        if (def.extensions() != null && !def.extensions().isEmpty()) {
            for (var entry : def.extensions().entrySet()) {
                if (!"title".equals(entry.getKey()) && !"version".equals(entry.getKey())
                        && !"description".equals(entry.getKey())) {
                    info.put(entry.getKey(), entry.getValue());
                }
            }
        }
        var contact = new LinkedHashMap<String, String>();
        if (contactName != null) contact.put("name", contactName);
        if (contactEmail != null) contact.put("email", contactEmail);
        if (!contact.isEmpty()) info.put("contact", contact);
        if (licenseName != null) {
            info.put("license", Map.of("name", licenseName));
        }
        spec.put("info", info);

        // Servers
        spec.put("servers", List.of(Map.of("url", serverUrl)));

        // Paths — build from operations
        var paths = new LinkedHashMap<String, LinkedHashMap<String, Object>>();
        var tagSet = new LinkedHashSet<String>();

        for (var group : def.groups().values()) {
            for (var op : group.operations().values()) {
                if (op.hidden()) continue;
                tagSet.addAll(group.tags());
                tagSet.addAll(op.tags());

                String path = resolvePath(op);
                List<String> methods = resolveHttpMethods(op);
                for (String method : methods) {
                    paths.computeIfAbsent(path, k -> new LinkedHashMap<>()).put(method, buildOperation(op));
                }
            }
        }
        spec.put("paths", paths);

        // Components — schemas from types
        var components = new LinkedHashMap<String, Object>();
        var schemas = new LinkedHashMap<String, Object>();

        // Collect all schemas from operations and type definitions
        for (var group : def.groups().values()) {
            // Group-level types
            if (group.types() != null) {
                for (var entry : group.types().entrySet()) {
                    schemas.put(schemaName(entry.getKey()), buildJsonSchema(entry.getValue()));
                }
            }
            for (var op : group.operations().values()) {
                if (op.response() != null && !isScalar(op.response())) {
                    String name = op.response().javaType() != null
                            ? schemaName(op.response().javaType())
                            : op.response().name();
                    if (!schemas.containsKey(name)) {
                        schemas.put(name, buildJsonSchema(op.response()));
                    }
                }
                // Also register parameter types
                for (var param : op.parameters()) {
                    if (param.type() != null && !param.type().isScalar() && param.type().javaType() != null) {
                        String name = schemaName(param.type().javaType());
                        if (!schemas.containsKey(name)) {
                            schemas.put(name, buildJsonSchema(param.type()));
                        }
                    }
                }
            }
        }

        // Add top-level type definitions
        for (var entry : def.types().entrySet()) {
            String name = schemaName(entry.getKey());
            if (!schemas.containsKey(name)) {
                schemas.put(name, buildJsonSchema(entry.getValue()));
            }
        }

        // Security schemes
        var securitySchemesMap = new LinkedHashMap<String, Object>();
        for (String schemeName : securitySchemeNames) {
            if ("bearerAuth".equals(schemeName) || "bearer".equals(schemeName)) {
                securitySchemesMap.put(schemeName, Map.of(
                        "type", "http", "scheme", "bearer", "bearerFormat", "JWT"));
            } else if ("apiKey".equals(schemeName)) {
                securitySchemesMap.put(schemeName, Map.of(
                        "type", "apiKey", "in", "header", "name", "X-API-Key"));
            } else if ("basicAuth".equals(schemeName)) {
                securitySchemesMap.put(schemeName, Map.of(
                        "type", "http", "scheme", "basic"));
            } else {
                securitySchemesMap.put(schemeName, Map.of("type", "oauth2"));
            }
        }
        if (!schemas.isEmpty()) components.put("schemas", schemas);
        if (!securitySchemesMap.isEmpty()) components.put("securitySchemes", securitySchemesMap);
        if (!components.isEmpty()) spec.put("components", components);

        // Tags
        if (!tagSet.isEmpty()) {
            spec.put("tags", tagSet.stream().map(t -> Map.of("name", t)).toList());
        }

        // Security
        if (securityEnabled && !securitySchemeNames.isEmpty()) {
            var secRequirements = new ArrayList<Object>();
            for (String scheme : securitySchemeNames) {
                secRequirements.add(Map.of(scheme, List.of()));
            }
            spec.put("security", secRequirements);
        }

        return spec;
    }

    /** Convert a fully-qualified class name or type name to a short schema name */
    private String schemaName(String fqName) {
        if (fqName == null) return "Object";
        int lastDot = fqName.lastIndexOf('.');
        return lastDot >= 0 ? fqName.substring(lastDot + 1) : fqName;
    }

    private String resolvePath(ApiOperation op) {
        var ext = op.extensions();
        if (ext != null && ext.get("path") != null) {
            String path = (String) ext.get("path");
            if (path != null && !path.isEmpty()) return path;
        }
        return "/" + op.name();
    }

    private List<String> resolveHttpMethods(ApiOperation op) {
        var ext = op.extensions();
        if (ext != null) {
            var methods = ext.get("httpMethods");
            if (methods instanceof List<?> methodList && !methodList.isEmpty()) {
                return methodList.stream()
                        .map(Object::toString)
                        .map(s -> s.toLowerCase(Locale.ROOT))
                        .toList();
            }
        }
        // Default: no params = GET, has params = POST
        return List.of(op.parameters().isEmpty() ? "get" : "post");
    }

    private Map<String, Object> buildOperation(ApiOperation op) {
        var operation = new LinkedHashMap<String, Object>();
        if (op.summary() != null) operation.put("summary", op.summary());
        if (op.description() != null) operation.put("description", op.description());
        if (op.operationId() != null) operation.put("operationId", op.operationId());
        if (!op.tags().isEmpty()) operation.put("tags", op.tags());
        if (op.deprecated()) operation.put("deprecated", true);

        // Parameters — query and path params (scalar types)
        if (!op.parameters().isEmpty()) {
            var params = new ArrayList<Object>();
            for (var param : op.parameters()) {
                if (param.kind() == ApiParameterKind.INPUT && param.type() != null && param.type().isScalar()) {
                    params.add(buildParameter(param));
                }
            }
            if (!params.isEmpty()) operation.put("parameters", params);
        }

        // Request body for POST/PUT/PATCH with complex types
        var httpMethods = resolveHttpMethods(op);
        boolean isBodyMethod = httpMethods.stream().anyMatch(m ->
                m.equals("post") || m.equals("put") || m.equals("patch"));

        var bodyParams = op.parameters().stream()
                .filter(p -> p.kind() == ApiParameterKind.INPUT && p.type() != null && !p.type().isScalar())
                .toList();

        if (isBodyMethod && !bodyParams.isEmpty()) {
            var bodySchema = new LinkedHashMap<String, Object>();
            bodySchema.put("type", "object");
            var props = new LinkedHashMap<String, Object>();
            for (var bp : bodyParams) {
                props.put(bp.name(), buildJsonSchema(bp.type()));
            }
            bodySchema.put("properties", props);
            var required = bodyParams.stream()
                    .filter(ApiParameter::required)
                    .map(ApiParameter::name)
                    .toList();
            if (!required.isEmpty()) bodySchema.put("required", required);

            var requestBody = new LinkedHashMap<String, Object>();
            requestBody.put("required", bodyParams.stream().anyMatch(ApiParameter::required));
            requestBody.put("content", Map.of("application/json", Map.of("schema", bodySchema)));
            operation.put("requestBody", requestBody);
        }

        // Response
        var responses = new LinkedHashMap<String, Object>();
        if (op.response() != null) {
            var respSchema = buildJsonSchema(op.response());
            responses.put("200", Map.of(
                    "description", op.summary() != null ? op.summary() : "Successful operation",
                    "content", Map.of("application/json", Map.of("schema", respSchema))
            ));
        } else {
            responses.put("204", Map.of("description", "No content"));
        }

        // Errors
        if (op.errors() != null && !op.errors().isEmpty()) {
            for (var error : op.errors()) {
                int code = parseErrorCode(error.code());
                var errorResponse = new LinkedHashMap<String, Object>();
                errorResponse.put("description", error.description());
                errorResponse.put("content", Map.of(
                        "application/json", Map.of("schema", Map.of(
                                "type", "object",
                                "properties", Map.of(
                                        "code", Map.of("type", "string"),
                                        "message", Map.of("type", "string")
                                ))
                        )
                ));
                responses.put(String.valueOf(code), errorResponse);
            }
        }
        operation.put("responses", responses);

        // Security overrides from extensions
        if (op.extensions() != null && op.extensions().get("roles") instanceof List roles) {
            var opSecurity = roles.stream()
                    .filter(role -> role instanceof String str && securitySchemeNames.stream().anyMatch(s -> str.contains(s)))
                    .map(role -> Map.of(role, List.of()))
                    .toList();
            if (!opSecurity.isEmpty()) {
                operation.put("security", opSecurity);
            }
        }

        return operation;
    }

    private Map<String, Object> buildParameter(ApiParameter param) {
        var p = new LinkedHashMap<String, Object>();
        p.put("name", param.name());
        p.put("in", paramLocation(param));
        p.put("required", param.required());
        p.put("schema", buildJsonSchema(param.type()));
        if (param.description() != null) p.put("description", param.description());
        if (param.defaultValue() != null) p.put("default", param.defaultValue());
        if (param.examples() != null && !param.examples().isEmpty()) {
            p.put("example", param.examples().get(0));
        }
        return p;
    }

    private String paramLocation(ApiParameter param) {
        var ext = param.extensions();
        if (ext != null && ext.get("in") != null) {
            String loc = ((String) ext.get("in")).toLowerCase();
            return switch (loc) {
                case "path", "query", "header", "cookie" -> loc;
                default -> "query";
            };
        }
        return param.type() != null && param.type().isScalar() ? "query" : "body";
    }

    private Map<String, Object> buildJsonSchema(ApiDataType type) {
        if (type == null) return Map.of("type", "object");
        var schema = new LinkedHashMap<String, Object>();

        if (type.isScalar()) {
            schema.put("type", type.name());
        } else if (type.isObject() && type.properties() != null) {
            schema.put("type", "object");
            var props = new LinkedHashMap<String, Object>();
            var required = new ArrayList<String>();
            for (var entry : type.properties().entrySet()) {
                var propSchema = buildJsonSchema(entry.getValue());
                props.put(entry.getKey(), propSchema);
                if (!entry.getValue().nullable()) {
                    required.add(entry.getKey());
                }
            }
            schema.put("properties", props);
            if (!required.isEmpty()) schema.put("required", required);
        } else if ("array".equals(type.format())) {
            schema.put("type", "array");
            if (type.properties() != null && type.properties().containsKey("items")) {
                schema.put("items", buildJsonSchema(type.properties().get("items")));
            }
        } else {
            schema.put("type", type.name() != null ? type.name() : "object");
        }

        // OpenAPI 3.1 uses type arrays for nullable
        if (type.nullable()) {
            var currentType = schema.get("type");
            if (currentType instanceof String strType) {
                schema.put("type", List.of(strType, "null"));
            }
        }

        return schema;
    }

    private boolean isScalar(ApiDataType type) {
        return type != null && type.isScalar();
    }

    private List<String> extractHttpMethods(ApiOperation op) {
        return resolveHttpMethods(op);
    }

    private int parseErrorCode(String code) {
        if (code == null) return 500;
        code = code.toUpperCase(Locale.ROOT);
        return switch (code) {
            case "NOT_FOUND" -> 404;
            case "BAD_REQUEST", "INVALID_QUERY" -> 400;
            case "UNAUTHORIZED" -> 401;
            case "FORBIDDEN" -> 403;
            case "CONFLICT" -> 409;
            case "UNPROCESSABLE" -> 422;
            case "RATE_LIMITED" -> 429;
            default -> 500;
        };
    }
}
