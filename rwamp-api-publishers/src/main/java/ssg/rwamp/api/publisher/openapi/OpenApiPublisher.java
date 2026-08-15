package ssg.rwamp.api.publisher.openapi;

import ssg.rwamp.api.provider.model.ApiDefinition;
import ssg.rwamp.api.publisher.ApiPublisher;

import java.util.*;

/**
 * Publishes an {@link ApiDefinition} as an OpenAPI 3.1.x specification.
 * <p>
 * Generates the full OpenAPI spec as JSON or YAML. Supports both formats
 * with no external dependencies — uses a built-in JSON writer and a simple
 * YAML serializer.
 * <p>
 * Usage:
 * <pre>{@code
 * var publisher = new OpenApiPublisher()
 *     .serverUrl("https://api.example.com")
 *     .contact("Dev Team", "dev@example.com")
 *     .license("MIT");
 * String json = publisher.publishAsJson(definition);
 * String yaml = publisher.publishAsYaml(definition);
 * }</pre>
 *
 * @since 0.1.0
 */
public class OpenApiPublisher implements ApiPublisher {

    private final OpenApiSpecGenerator generator = new OpenApiSpecGenerator();

    public OpenApiPublisher serverUrl(String url) { generator.serverUrl(url); return this; }
    public OpenApiPublisher contact(String name, String email) { generator.contact(name, email); return this; }
    public OpenApiPublisher license(String name) { generator.license(name); return this; }
    public OpenApiPublisher securityEnabled(boolean enabled) { generator.securityEnabled(enabled); return this; }

    @Override
    public Object publish(ApiDefinition definition) {
        return generator.generate(definition);
    }

    /**
     * Publishes as a JSON string (pretty-printed).
     */
    public String publishAsJson(ApiDefinition definition) {
        return toJson(generator.generate(definition));
    }

    /**
     * Publishes as a YAML string.
     */
    public String publishAsYaml(ApiDefinition definition) {
        return toYaml(generator.generate(definition));
    }

    @Override
    public String type() {
        return "openapi";
    }

    // ── Minimal JSON writer ──

    static String toJson(Object obj) {
        return new JsonWriter().write(obj);
    }

    // ── Minimal YAML writer ──

    static String toYaml(Object obj) {
        return new YamlWriter().write(obj);
    }

    private static class JsonWriter {
        private final StringBuilder sb = new StringBuilder();
        private int indent = 0;

        String write(Object obj) {
            encode(obj);
            return sb.toString();
        }

        private void encode(Object obj) {
            if (obj == null) {
                sb.append("null");
            } else if (obj instanceof String s) {
                sb.append('"');
                for (char c : s.toCharArray()) {
                    switch (c) {
                        case '"': sb.append("\\\""); break;
                        case '\\': sb.append("\\\\"); break;
                        case '\n': sb.append("\\n"); break;
                        case '\t': sb.append("\\t"); break;
                        default: sb.append(c);
                    }
                }
                sb.append('"');
            } else if (obj instanceof Number || obj instanceof Boolean) {
                sb.append(obj);
            } else if (obj instanceof Map<?, ?> map) {
                sb.append('{');
                indent++;
                boolean first = true;
                for (var entry : map.entrySet()) {
                    if (!first) sb.append(',');
                    newline();
                    encode(entry.getKey());
                    sb.append(": ");
                    encode(entry.getValue());
                    first = false;
                }
                indent--;
                if (!map.isEmpty()) { newline(); }
                sb.append('}');
            } else if (obj instanceof Iterable<?> iter) {
                sb.append('[');
                indent++;
                boolean first = true;
                for (var item : iter) {
                    if (!first) sb.append(',');
                    newline();
                    encode(item);
                    first = false;
                }
                indent--;
                if (iter.toString().length() > 2) { newline(); }
                sb.append(']');
            } else {
                sb.append('"').append(obj).append('"');
            }
        }

        private void newline() {
            sb.append("\n");
            for (int i = 0; i < indent * 2; i++) sb.append(' ');
        }
    }

    private static class YamlWriter {
        private final StringBuilder sb = new StringBuilder();
        private int indent = 0;

        String write(Object obj) {
            writeValue(obj, 0, false);
            return sb.toString();
        }

        private void writeValue(Object obj, int level, boolean isSequenceItem) {
            if (obj == null) {
                sb.append("null");
            } else if (obj instanceof String s) {
                sb.append(quoteIfNeeded(s));
            } else if (obj instanceof Boolean) {
                sb.append(obj);
            } else if (obj instanceof Number) {
                sb.append(obj);
            } else if (obj instanceof Map<?, ?> map) {
                writeMap(map, level);
            } else if (obj instanceof Iterable<?> iter) {
                writeList(iter, level);
            } else {
                sb.append(obj);
            }
        }

        private void writeMap(Map<?, ?> map, int level) {
            for (var entry : map.entrySet()) {
                indent(level);
                sb.append(entry.getKey()).append(": ");
                writeValue(entry.getValue(), level + 1, false);
                sb.append("\n");
            }
        }

        private void writeList(Iterable<?> list, int level) {
            for (var item : list) {
                indent(level);
                sb.append("- ");
                if (item instanceof Map) {
                    writeMap((Map<?, ?>) item, level + 1);
                } else if (item instanceof Iterable && !(item instanceof String)) {
                    writeList((Iterable<?>) item, level + 1);
                } else {
                    writeValue(item, level + 1, true);
                    sb.append("\n");
                }
            }
        }

        private void indent(int level) {
            for (int i = 0; i < level * 2; i++) sb.append(' ');
        }

        private String quoteIfNeeded(String s) {
            if (s == null || s.isEmpty()) return "''";
            boolean needsQuote = s.contains(":") || s.contains("#") || s.contains("'")
                    || s.contains("\"") || s.startsWith("- ") || s.contains("\n")
                    || s.equals("true") || s.equals("false") || s.equals("null")
                    || s.matches("^\\d+$");
            if (!needsQuote) return s;

            if (s.contains("'") && !s.contains("\"")) {
                return "'" + s + "'";
            }
            if (s.contains("\"")) {
                return "'" + s.replace("'", "''") + "'";
            }
            return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
        }
    }
}
