package ssg.rwamp.api.webservices;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Describes a parameter of a web-service method discovered via annotation scanning.
 * <p>
 * Unlike plain {@link ssg.rwamp.api.provider.model.ApiParameter}, this carries
 * web-service-specific metadata such as parameter source (path, query, header, form).
 *
 * @since 0.1.0
 */
public class WsParameter {

    private final String name;
    private final String type;
    private final String description;
    private final String source;
    private final boolean required;
    private final String defaultValue;
    private final List<String> style;

    public enum Source {
        PATH, QUERY, HEADER, FORM, COOKIE, MATRIX, BODY
    }

    public WsParameter(String name, String type) {
        this(name, type, null, null, true, null, null, null);
    }

    public WsParameter(String name, String type, String description, String source,
                       boolean required, String defaultValue, List<String> style, Map<String, Object> extensions) {
        this.name = name;
        this.type = type;
        this.description = description;
        this.source = source != null ? source : Source.QUERY.name();
        this.required = required;
        this.defaultValue = defaultValue;
        this.style = style;
    }

    public String name() { return name; }
    public String type() { return type; }
    public String description() { return description; }
    public String source() { return source; }
    public boolean required() { return required; }
    public String defaultValue() { return defaultValue; }
    public List<String> style() { return style; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WsParameter that = (WsParameter) o;
        return required == that.required &&
                Objects.equals(name, that.name) &&
                Objects.equals(type, that.type) &&
                Objects.equals(source, that.source);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, type, source, required);
    }

    @Override
    public String toString() {
        return "WsParameter{" +
                "name='" + name + '\'' +
                ", type='" + type + '\'' +
                ", source='" + source + '\'' +
                ", required=" + required +
                '}';
    }
}
