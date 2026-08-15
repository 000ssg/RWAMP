package ssg.rwamp.demo.feature;

import ssg.rwamp.api.provider.*;
import ssg.rwamp.api.provider.annotations.*;
import ssg.rwamp.api.provider.model.ApiDataType;
import ssg.rwamp.api.provider.model.ApiDefinition;

import java.util.List;

/**
 * Demo of reflection-based API discovery using api-providers.
 * <p>
 * This demo shows three ways to define APIs:
 * <ol>
 *   <li>Annotation-based: Scan {@code @ApiService} classes</li>
 *   <li>Convention-based: Derive operations from JavaBean getter/setter pairs</li>
 *   <li>Manual: Build API definitions programmatically with a fluent builder</li>
 * </ol>
 *
 * @since 0.1.0
 */
public class ApiProvidersDemo {

    @ApiService(name = "products", description = "Product catalog API",
            path = "/api/products", tags = {"catalog"})
    public static class ProductService {
        @Operation(summary = "Get product by ID", httpMethods = {HttpMethod.GET})
        public Product getProduct(@ApiParam(name = "id") Long id) {
            return new Product(id, "Widget", 9.99);
        }

        @Operation(summary = "Search products", httpMethods = {HttpMethod.GET})
        public List<Product> search(@ApiParam(name = "query") String query,
                                     @ApiParam(required = false) int page) {
            return List.of();
        }
    }

    public static class OrderBean {
        private String status;
        private int itemCount;

        public String getStatus() { return status; }
        public void setStatus(String s) { this.status = s; }
        public int getItemCount() { return itemCount; }
        public void setItemCount(int n) { this.itemCount = n; }
    }

    public record Product(Long id, String name, double price) {}

    public record DemoResult(String annotationResult,
                              String conventionResult,
                              String manualResult) {}

    public DemoResult run() {
        // 1. Annotation-based
        var annotationProvider = new AnnotationBasedApiProvider("annotation-api");
        ApiDefinition annotationApi = annotationProvider.build(ProductService.class);
        String annotationResult = "Groups: " + annotationApi.groups().keySet() +
                ", Operations: " + annotationApi.allOperations().size();

        // 2. Convention-based (getter/setter)
        var conventionProvider = new GetterSetterApiProvider("convention-api");
        ApiDefinition conventionApi = conventionProvider.build(OrderBean.class);
        String conventionResult = "Groups: " + conventionApi.groups().keySet() +
                ", Operations: " + conventionApi.allOperations().size();

        // 3. Manual (fluent builder)
        ApiDefinition manualApi = ManualApiProvider.builder("manual-api")
                .version("1.0.0")
                .description("Manually defined API")
                .group("orders", "Order management", g -> g
                        .tag("ecommerce")
                        .operation("create-order", "Create a new order", op -> op
                                .param("productId", ApiDataType.INTEGER)
                                .param("quantity", ApiDataType.INTEGER)
                                .response(ApiDataType.STRING)
                        )
                )
                .build();
        String manualResult = "Groups: " + manualApi.groups().keySet() +
                ", Operations: " + manualApi.allOperations().size();

        return new DemoResult(annotationResult, conventionResult, manualResult);
    }

    public static void main(String[] args) {
        var demo = new ApiProvidersDemo();
        var result = demo.run();
        System.out.println("ApiProvidersDemo:");
        System.out.println("  Annotation-based: " + result.annotationResult());
        System.out.println("  Convention-based: " + result.conventionResult());
        System.out.println("  Manual: " + result.manualResult());
    }
}
