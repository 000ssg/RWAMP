package ssg.rwamp.api.webservices;

import ssg.rwamp.api.webservices.annotations.*;

import java.util.Map;

/**
 * Another sample resource for testing multiple resources.
 */
@TestPath("/api/orders")
public class TestOrderResource {

    @TestGet
    public String listOrders() {
        return "all-orders";
    }

    @TestGet
    @TestPath("/{orderId}")
    public String getOrder(@TestPathParam("orderId") Long orderId) {
        return "order:" + orderId;
    }

    @TestPost
    @TestPath("/checkout")
    @TestConsumes({"application/json"})
    @TestProduces({"application/json"})
    public Map<String, Object> checkout(
            @TestQueryParam("cartId") String cartId) {
        return Map.of("status", "ok");
    }
}
