package ssg.rwamp.api.webservices;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WsModelTest {

    @Test
    void testWsParameterBasic() {
        var param = new WsParameter("id", "String");
        assertThat(param.name()).isEqualTo("id");
        assertThat(param.type()).isEqualTo("String");
        assertThat(param.source()).isEqualTo("QUERY"); // default
        assertThat(param.required()).isTrue();
        assertThat(param.defaultValue()).isNull();
    }

    @Test
    void testWsParameterFull() {
        var param = new WsParameter("name", "String", "User name", "PATH",
                false, "admin", List.of("simple"), Map.of());
        assertThat(param.name()).isEqualTo("name");
        assertThat(param.type()).isEqualTo("String");
        assertThat(param.description()).isEqualTo("User name");
        assertThat(param.source()).isEqualTo("PATH");
        assertThat(param.required()).isFalse();
        assertThat(param.defaultValue()).isEqualTo("admin");
        assertThat(param.style()).containsExactly("simple");
    }

    @Test
    void testWsParameterEquals() {
        var p1 = new WsParameter("id", "String", null, "PATH", true, null, null, null);
        var p2 = new WsParameter("id", "String", "desc", "PATH", true, null, null, null);
        var p3 = new WsParameter("id", "Integer", null, "PATH", true, null, null, null);
        var p4 = new WsParameter("id", "String", null, "QUERY", true, null, null, null);

        assertThat(p1).isEqualTo(p2); // description doesn't affect equality
        assertThat(p1).isNotEqualTo(p3); // type differs
        assertThat(p1).isNotEqualTo(p4); // source differs
        assertThat(p1.hashCode()).isEqualTo(p2.hashCode());
    }

    @Test
    void testWsParameterToString() {
        var param = new WsParameter("id", "String");
        String str = param.toString();
        assertThat(str).contains("id");
        assertThat(str).contains("String");
    }

    @Test
    void testWsParameterSourceEnum() {
        assertThat(WsParameter.Source.PATH.name()).isEqualTo("PATH");
        assertThat(WsParameter.Source.QUERY.name()).isEqualTo("QUERY");
        assertThat(WsParameter.Source.HEADER.name()).isEqualTo("HEADER");
        assertThat(WsParameter.Source.FORM.name()).isEqualTo("FORM");
        assertThat(WsParameter.Source.COOKIE.name()).isEqualTo("COOKIE");
        assertThat(WsParameter.Source.MATRIX.name()).isEqualTo("MATRIX");
        assertThat(WsParameter.Source.BODY.name()).isEqualTo("BODY");
    }

    @Test
    void testWsMethodBasic() {
        var method = new WsMethod("listUsers", "/api/users", "GET", "List<String>");
        assertThat(method.name()).isEqualTo("listUsers");
        assertThat(method.path()).isEqualTo("/api/users");
        assertThat(method.httpMethod()).isEqualTo("GET");
        assertThat(method.returnType()).isEqualTo("List<String>");
        assertThat(method.description()).isNull();
        assertThat(method.provider()).isNull();
        assertThat(method.consumes()).isEmpty();
        assertThat(method.produces()).isEmpty();
        assertThat(method.roles()).isEmpty();
        assertThat(method.tags()).isEmpty();
        assertThat(method.parameters()).isEmpty();
        assertThat(method.extensions()).isEmpty();
    }

    @Test
    void testWsMethodFull() {
        var method = new WsMethod("createUser", "/api/users", "POST", "String",
                "Create a user", "jax-rs",
                List.of("application/json"), List.of("application/json"),
                List.of("admin"), List.of("users"),
                List.of(new WsParameter("name", "String")),
                Map.of("deprecated", false));

        assertThat(method.name()).isEqualTo("createUser");
        assertThat(method.path()).isEqualTo("/api/users");
        assertThat(method.httpMethod()).isEqualTo("POST");
        assertThat(method.description()).isEqualTo("Create a user");
        assertThat(method.provider()).isEqualTo("jax-rs");
        assertThat(method.consumes()).containsExactly("application/json");
        assertThat(method.produces()).containsExactly("application/json");
        assertThat(method.roles()).containsExactly("admin");
        assertThat(method.tags()).containsExactly("users");
        assertThat(method.parameters()).hasSize(1);
        assertThat(method.extensions()).containsEntry("deprecated", false);
    }

    @Test
    void testWsMethodEquals() {
        var m1 = new WsMethod("list", "/users", "GET", "String");
        var m2 = new WsMethod("list", "/users", "GET", "Integer");
        var m3 = new WsMethod("list", "/users", "POST", "String");

        assertThat(m1).isEqualTo(m2); // returnType doesn't affect equality
        assertThat(m1).isNotEqualTo(m3); // httpMethod differs
        assertThat(m1.hashCode()).isEqualTo(m2.hashCode());
    }

    @Test
    void testWsMethodToString() {
        var method = new WsMethod("list", "/users", "GET", "String");
        String str = method.toString();
        assertThat(str).contains("list");
        assertThat(str).contains("/users");
        assertThat(str).contains("GET");
    }
}
