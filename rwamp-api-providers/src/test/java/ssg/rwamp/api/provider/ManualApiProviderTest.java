package ssg.rwamp.api.provider;

import ssg.rwamp.api.provider.model.*;
import org.junit.jupiter.api.Test;

import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ManualApiProvider}.
 */
class ManualApiProviderTest {

    @Test
    void builder_basic() {
        var def = ManualApiProvider.builder("test-api")
                .version("3.0.0")
                .description("Test API")
                .build();

        assertThat(def.name()).isEqualTo("test-api");
        assertThat(def.version()).isEqualTo("3.0.0");
        assertThat(def.description()).isEqualTo("Test API");
        assertThat(def.groups()).isEmpty();
    }

    @Test
    void builder_withGroup() {
        var def = ManualApiProvider.builder("test-api")
                .group("orders", "Order management", g -> g
                        .tag("ecommerce")
                        .operation("get-order", "Get order details", op -> op
                                .summary("Retrieve order by ID")
                                .param("orderId", ApiDataType.INTEGER)
                                .response(ApiDataType.STRING)
                        )
                        .operation("cancel-order", "Cancel an order", op -> op
                                .param("orderId", ApiDataType.INTEGER)
                        )
                )
                .build();

        var group = def.groups().get("orders");
        assertThat(group.description()).isEqualTo("Order management");
        assertThat(group.tags()).containsExactly("ecommerce");
        assertThat(group.operations()).hasSize(2);

        var getOp = group.operations().get("get-order");
        assertThat(getOp.summary()).isEqualTo("Retrieve order by ID");
        assertThat(getOp.parameters()).hasSize(1);
        assertThat(getOp.response()).isEqualTo(ApiDataType.STRING);

        var cancelOp = group.operations().get("cancel-order");
        assertThat(cancelOp.parameters()).hasSize(1);
        assertThat(cancelOp.response()).isNull();
    }

    @Test
    void builder_withType() {
        var orderType = new ApiDataType("Order", "object", "com.app.Order", true,
                Map.of("id", ApiDataType.INTEGER, "status", ApiDataType.STRING));

        var def = ManualApiProvider.builder("test-api")
                .type("com.app.Order", orderType)
                .build();

        assertThat(def.types()).containsKey("com.app.Order");
        assertThat(def.types().get("com.app.Order").name()).isEqualTo("Order");
    }

    @Test
    void builder_extensions() {
        var def = ManualApiProvider.builder("test-api")
                .extension("contact", "dev@example.com")
                .extension("license", "MIT")
                .build();

        assertThat(def.extensions()).containsEntry("contact", "dev@example.com");
        assertThat(def.extensions()).containsEntry("license", "MIT");
    }

    @Test
    void builder_complexOperation() {
        var def = ManualApiProvider.builder("test-api")
                .group("items", null, g -> g
                        .operation("search", "Search items", op -> op
                                .summary("Full-text search")
                                .operationId("searchItems")
                                .param("query", ApiDataType.STRING)
                                .param("page", ApiDataType.INTEGER, false)
                                .response(ApiDataType.STRING)
                                .error("NOT_FOUND", "No items found")
                                .error("INVALID_QUERY", "Search query is invalid")
                                .tag("search")
                        )
                )
                .build();

        var op = def.groups().get("items").operations().get("search");
        assertThat(op.operationId()).isEqualTo("searchItems");
        assertThat(op.parameters()).hasSize(2);
        assertThat(op.parameters().get(1).required()).isFalse();
        assertThat(op.errors()).hasSize(2);
        assertThat(op.tags()).containsExactly("search");
    }

    @Test
    void canHandle_andBuild() {
        var provider = ManualApiProvider.builder("test").build();
        var manualProvider = new ManualApiProvider(provider);

        assertThat(manualProvider.canHandle(new Object())).isFalse();
        assertThat(manualProvider.type()).isEqualTo("manual");
    }
}
