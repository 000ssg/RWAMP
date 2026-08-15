package ssg.rwamp.api.webservices;

import java.util.*;

/**
 * Describes a web-service method discovered via annotation scanning.
 * <p>
 * Carries path, HTTP method, parameters, response type, and annotation metadata.
 *
 * @since 0.1.0
 */
public class WsMethod {

    private final String name;
    private final String path;
    private final String httpMethod;
    private final String returnType;
    private final String description;
    private final String provider;
    private final List<String> consumes;
    private final List<String> produces;
    private final List<String> roles;
    private final List<String> tags;
    private final List<WsParameter> parameters;
    private final Map<String, Object> extensions;

    public WsMethod(String name, String path, String httpMethod, String returnType) {
        this(name, path, httpMethod, returnType, null, null,
                List.of(), List.of(), List.of(), List.of(),
                List.of(), Map.of());
    }

    public WsMethod(String name, String path, String httpMethod, String returnType,
                    String description, String provider,
                    List<String> consumes, List<String> produces,
                    List<String> roles, List<String> tags,
                    List<WsParameter> parameters, Map<String, Object> extensions) {
        this.name = name;
        this.path = path;
        this.httpMethod = httpMethod;
        this.returnType = returnType;
        this.description = description;
        this.provider = provider;
        this.consumes = consumes;
        this.produces = produces;
        this.roles = roles;
        this.tags = tags;
        this.parameters = parameters;
        this.extensions = extensions;
    }

    public String name() { return name; }
    public String path() { return path; }
    public String httpMethod() { return httpMethod; }
    public String returnType() { return returnType; }
    public String description() { return description; }
    public String provider() { return provider; }
    public List<String> consumes() { return consumes; }
    public List<String> produces() { return produces; }
    public List<String> roles() { return roles; }
    public List<String> tags() { return tags; }
    public List<WsParameter> parameters() { return parameters; }
    public Map<String, Object> extensions() { return extensions; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WsMethod that = (WsMethod) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(path, that.path) &&
                Objects.equals(httpMethod, that.httpMethod);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, path, httpMethod);
    }

    @Override
    public String toString() {
        return "WsMethod{" +
                "name='" + name + '\'' +
                ", path='" + path + '\'' +
                ", httpMethod='" + httpMethod + '\'' +
                '}';
    }
}
