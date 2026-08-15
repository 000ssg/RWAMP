package ssg.rwamp.api.publisher.jsdoc;

import ssg.rwamp.api.provider.model.*;
import ssg.rwamp.api.publisher.ApiPublisher;

import java.util.*;

/**
 * Generates an interactive HTML page for API documentation and testing.
 * <p>
 * Inspired by Swagger UI but self-contained (no external dependencies).
 * Produces a single HTML file with:
 * <ul>
 *   <li>Foldable sections per API group</li>
 *   <li>Parameter input forms for each operation</li>
 *   <li>Try-it-out buttons with WAMP/REST call simulation</li>
 *   <li>Responsive styling</li>
 * </ul>
 * <p>
 * Usage:
 * <pre>{@code
 * var publisher = new JsDocPublisher("https://api.example.com");
 * String html = publisher.publish(definition);
 * }</pre>
 *
 * @since 0.1.0
 */
public class JsDocPublisher implements ApiPublisher {

    private String baseUrl;
    private String title;
    private String theme = "light";

    public JsDocPublisher baseUrl(String url) { this.baseUrl = url; return this; }
    public JsDocPublisher title(String t) { this.title = t; return this; }
    public JsDocPublisher theme(String t) { this.theme = t; return this; }

    @Override
    public Object publish(ApiDefinition definition) {
        this.title = this.title != null ? this.title : (definition.name() != null ? definition.name() : "API");
        return buildHtml(definition);
    }

    private String buildHtml(ApiDefinition def) {
        var sb = new StringBuilder();

        sb.append("""
                <!DOCTYPE html>
                <html lang="en">
                <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>""");
        sb.append(escapeHtml(title));
        sb.append("""
                </title>
                <style>
                :root { --bg: #fff; --fg: #333; --border: #ddd; --primary: #007bff; --hover: #f5f5f5; }
                body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; margin: 0; padding: 20px; background: var(--bg); color: var(--fg); }
                h1 { border-bottom: 2px solid var(--border); padding-bottom: 10px; }
                h2 { margin-top: 30px; }
                .group { border: 1px solid var(--border); border-radius: 8px; margin: 16px 0; overflow: hidden; }
                .group-header { background: #f8f9fa; padding: 12px 16px; cursor: pointer; display: flex; justify-content: space-between; align-items: center; }
                .group-header:hover { background: var(--hover); }
                .group-content { padding: 0 16px; max-height: 0; overflow: hidden; transition: max-height 0.3s ease; }
                .group.open .group-content { max-height: 10000px; padding: 16px; }
                .operation { margin: 12px 0; padding: 12px; border-left: 3px solid var(--primary); background: var(--hover); border-radius: 0 4px 4px 0; }
                .op-name { font-weight: 600; font-size: 1.1em; }
                .op-desc { color: #666; margin: 4px 0; }
                .op-tags { display: inline-flex; gap: 4px; margin: 4px 0; }
                .tag { background: var(--primary); color: #fff; padding: 2px 8px; border-radius: 12px; font-size: 0.8em; }
                .param-table { width: 100%; margin: 8px 0; border-collapse: collapse; }
                .param-table th, .param-table td { padding: 6px 10px; border: 1px solid var(--border); text-align: left; }
                .param-table th { background: #f0f0f0; }
                .param-input { padding: 4px 8px; border: 1px solid var(--border); border-radius: 4px; width: 200px; }
                .btn { padding: 6px 16px; border: none; border-radius: 4px; cursor: pointer; font-size: 0.9em; margin: 4px 2px; }
                .btn-primary { background: var(--primary); color: #fff; }
                .btn-primary:hover { opacity: 0.9; }
                .result { margin-top: 8px; padding: 10px; background: #f0f8ff; border-radius: 4px; white-space: pre-wrap; font-family: monospace; font-size: 0.9em; display: none; }
                .result.visible { display: block; }
                .meta { color: #888; font-size: 0.85em; }
                .stats { display: flex; gap: 20px; margin: 10px 0; }
                .stat { padding: 8px 16px; background: #f0f0f0; border-radius: 8px; text-align: center; }
                .stat-num { font-size: 1.5em; font-weight: 700; }
                </style>
                </head>
                <body>
                """);

        sb.append("<h1>").append(escapeHtml(title)).append("</h1>");
        if (def.description() != null) {
            sb.append("<p class='meta'>").append(escapeHtml(def.description())).append("</p>");
        }
        sb.append("<p class='meta'>Version: ").append(escapeHtml(def.version())).append(" | ")
          .append(def.allOperations().size()).append(" operations</p>");

        // Render groups
        for (var group : def.groups().values()) {
            sb.append("<div class='group'>\n");
            sb.append("  <div class='group-header' onclick='this.parentElement.classList.toggle(\"open\")'>\n");
            sb.append("    <strong>").append(escapeHtml(group.name())).append("</strong>\n");
            sb.append("    <span>").append(group.operations().size()).append(" operations</span>\n");
            sb.append("  </div>\n");
            sb.append("  <div class='group-content'>\n");

            for (var op : group.operations().values()) {
                sb.append(buildOperationHtml(op));
            }

            sb.append("  </div>\n");
            sb.append("</div>\n");
        }

        // JS for interactivity
        sb.append("""
                <script>
                function toggleResult(id) {
                    var el = document.getElementById(id);
                    el.classList.toggle('visible');
                }
                function tryOperation(name, inputs) {
                    var params = [];
                    for (var i = 0; i < inputs.length; i++) {
                        var val = document.getElementById('inp_' + name + '_' + i).value;
                        if (val) params.push(val);
                    }
                    var resultDiv = document.getElementById('result_' + name);
                    resultDiv.textContent = JSON.stringify({ operation: name, params: params, status: 'simulated' }, null, 2);
                    resultDiv.classList.add('visible');
                }
                </script>
                """);

        sb.append("</body></html>");
        return sb.toString();
    }

    private String buildOperationHtml(ApiOperation op) {
        var sb = new StringBuilder();
        String id = op.name().replace("-", "_").replace(".", "_");

        sb.append("    <div class='operation'>\n");
        sb.append("      <div class='op-name'>").append(escapeHtml(op.name())).append("</div>\n");

        if (op.summary() != null) {
            sb.append("      <div class='op-desc'>").append(escapeHtml(op.summary())).append("</div>\n");
        }

        if (!op.tags().isEmpty()) {
            sb.append("      <div class='op-tags'>\n");
            for (var tag : op.tags()) {
                sb.append("        <span class='tag'>").append(escapeHtml(tag)).append("</span>\n");
            }
            sb.append("      </div>\n");
        }

        // Parameters
        if (!op.parameters().isEmpty()) {
            sb.append("      <table class='param-table'>\n");
            sb.append("        <tr><th>Name</th><th>Type</th><th>Required</th><th>Value</th></tr>\n");
            for (int i = 0; i < op.parameters().size(); i++) {
                var p = op.parameters().get(i);
                if (p.kind() == ApiParameterKind.INPUT) {
                    sb.append("        <tr>\n");
                    sb.append("          <td>").append(escapeHtml(p.name())).append("</td>\n");
                    sb.append("          <td>").append(escapeHtml(p.type().name())).append("</td>\n");
                    sb.append("          <td>").append(p.required() ? "✓" : "—").append("</td>\n");
                    sb.append("          <td><input class='param-input' id='inp_")
                        .append(id).append("_").append(i).append("'></td>\n");
                    sb.append("        </tr>\n");
                }
            }
            sb.append("      </table>\n");
        }

        // Buttons
        sb.append("      <button class='btn btn-primary' onclick='tryOperation(\"")
          .append(escapeHtml(op.name())).append("\", [");
        for (int i = 0; i < op.parameters().size(); i++) {
            if (op.parameters().get(i).kind() == ApiParameterKind.INPUT) {
                if (i > 0) sb.append(", ");
                sb.append("document.getElementById('inp_").append(id).append("_").append(i).append("')");
            }
        }
        sb.append("])'>Try it</button>\n");

        // Result area
        sb.append("      <div class='result' id='result_").append(id).append("'></div>\n");

        // Response type
        if (op.response() != null) {
            sb.append("      <div class='meta'>Response: ").append(escapeHtml(op.response().name())).append("</div>\n");
        }

        sb.append("    </div>\n");
        return sb.toString();
    }

    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    @Override
    public String type() {
        return "jsdoc";
    }
}
