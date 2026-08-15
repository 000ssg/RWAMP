package ssg.rwamp.api.webservices;

import ssg.rwamp.api.provider.model.ApiDefinition;
import ssg.rwamp.api.provider.model.ApiGroup;
import ssg.rwamp.api.provider.model.ApiOperation;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GenericAnnotationApiProviderTest {

    private static final String TEST_PATH = "ssg.rwamp.api.webservices.annotations.TestPath";
    private static final String TEST_GET = "ssg.rwamp.api.webservices.annotations.TestGet";
    private static final String TEST_POST = "ssg.rwamp.api.webservices.annotations.TestPost";
    private static final String TEST_PATH_PARAM = "ssg.rwamp.api.webservices.annotations.TestPathParam";
    private static final String TEST_QUERY_PARAM = "ssg.rwamp.api.webservices.annotations.TestQueryParam";
    private static final String TEST_CONSUMES = "ssg.rwamp.api.webservices.annotations.TestConsumes";
    private static final String TEST_PRODUCES = "ssg.rwamp.api.webservices.annotations.TestProduces";

    @Test
    void testIsOperableWithValidAnnotations() {
        var provider = new GenericAnnotationApiProvider("test-api")
                .setTypeAnnotations(TEST_PATH)
                .setMethodAnnotations(TEST_GET, TEST_POST)
                .setParameterAnnotations(TEST_PATH_PARAM, TEST_QUERY_PARAM);

        assertThat(provider.isOperable()).isTrue();
    }

    @Test
    void testIsOperableWithNonExistentAnnotations() {
        var provider = new GenericAnnotationApiProvider("test-api")
                .setTypeAnnotations("com.nonexistent.FakeTypeAnnotation")
                .setMethodAnnotations("com.nonexistent.FakeMethodAnnotation");

        assertThat(provider.isOperable()).isFalse();
    }

    @Test
    void testIsOperableWithEmptyConfig() {
        var provider = new GenericAnnotationApiProvider("test-api");
        assertThat(provider.isOperable()).isFalse();
    }

    @Test
    void testCanHandleAnnotatedClass() {
        var provider = new GenericAnnotationApiProvider("test-api")
                .setTypeAnnotations(TEST_PATH)
                .setMethodAnnotations(TEST_GET, TEST_POST)
                .setParameterAnnotations(TEST_PATH_PARAM, TEST_QUERY_PARAM);

        assertThat(provider.canHandle(TestResource.class)).isTrue();
        assertThat(provider.canHandle(TestOrderResource.class)).isTrue();
    }

    @Test
    void testCanHandleUnannotatedClass() {
        var provider = new GenericAnnotationApiProvider("test-api")
                .setTypeAnnotations(TEST_PATH)
                .setMethodAnnotations(TEST_GET, TEST_POST)
                .setParameterAnnotations(TEST_PATH_PARAM, TEST_QUERY_PARAM);

        assertThat(provider.canHandle(NoAnnotationResource.class)).isFalse();
    }

    @Test
    void testCanHandleNullTarget() {
        var provider = new GenericAnnotationApiProvider("test-api")
                .setTypeAnnotations(TEST_PATH)
                .setMethodAnnotations(TEST_GET, TEST_POST);

        assertThat(provider.canHandle(null)).isFalse();
    }

    @Test
    void testBuildTestResource() {
        var provider = new GenericAnnotationApiProvider("test-api")
                .setTypeAnnotations(TEST_PATH)
                .setMethodAnnotations(TEST_GET, TEST_POST)
                .setParameterAnnotations(TEST_PATH_PARAM, TEST_QUERY_PARAM)
                .setConsumesAnnotations(TEST_CONSUMES)
                .setProducesAnnotations(TEST_PRODUCES);

        ApiDefinition api = provider.build(TestResource.class);

        assertThat(api.name()).isEqualTo("test-api");
        assertThat(api.version()).isEqualTo("1.0.0");
        assertThat(api.groups()).isNotEmpty();

        // The group should be named after the @TestPath value
        var group = api.groups().get("/api/users");
        assertThat(group).isNotNull();
        assertThat(group.operations()).isNotEmpty();

        // Check that operations were discovered
        var ops = group.operations();
        assertThat(ops.containsKey("listUsers")).isTrue();
        assertThat(ops.containsKey("getUser")).isTrue();
        assertThat(ops.containsKey("searchUsers")).isTrue();
        assertThat(ops.containsKey("createUser")).isTrue();

        // internalHelper should NOT be included (no method annotation)
        assertThat(ops.containsKey("internalHelper")).isFalse();
        assertThat(ops.containsKey("toString")).isFalse();
    }

    @Test
    void testBuildWithPathParameters() {
        var provider = new GenericAnnotationApiProvider("test-api")
                .setTypeAnnotations(TEST_PATH)
                .setMethodAnnotations(TEST_GET, TEST_POST)
                .setParameterAnnotations(TEST_PATH_PARAM, TEST_QUERY_PARAM);

        ApiDefinition api = provider.build(TestResource.class);
        var group = api.groups().get("/api/users");
        ApiOperation getUser = group.operations().get("getUser");

        assertThat(getUser).isNotNull();
        assertThat(getUser.parameters()).hasSize(1);
        assertThat(getUser.parameters().get(0).name()).isEqualTo("id");
        assertThat(getUser.parameters().get(0).extensions()).containsEntry("source", "PATH");
    }

    @Test
    void testBuildWithQueryParameters() {
        var provider = new GenericAnnotationApiProvider("test-api")
                .setTypeAnnotations(TEST_PATH)
                .setMethodAnnotations(TEST_GET, TEST_POST)
                .setParameterAnnotations(TEST_PATH_PARAM, TEST_QUERY_PARAM);

        ApiDefinition api = provider.build(TestResource.class);
        var group = api.groups().get("/api/users");
        ApiOperation searchUsers = group.operations().get("searchUsers");

        assertThat(searchUsers).isNotNull();
        assertThat(searchUsers.parameters()).hasSize(2);

        var queryParam = searchUsers.parameters().get(0);
        assertThat(queryParam.name()).isEqualTo("q");
        assertThat(queryParam.extensions()).containsEntry("source", "QUERY");
    }

    @Test
    void testBuildNonOperable() {
        var provider = new GenericAnnotationApiProvider("test-api")
                .setTypeAnnotations("com.nonexistent.FakeType")
                .setMethodAnnotations("com.nonexistent.FakeMethod");

        ApiDefinition api = provider.build(TestResource.class);

        assertThat(api.name()).isEqualTo("test-api");
        assertThat(api.groups()).isEmpty();
    }

    @Test
    void testBuildOrderResource() {
        var provider = new GenericAnnotationApiProvider("order-api")
                .setTypeAnnotations(TEST_PATH)
                .setMethodAnnotations(TEST_GET, TEST_POST)
                .setParameterAnnotations(TEST_PATH_PARAM, TEST_QUERY_PARAM);

        ApiDefinition api = provider.build(TestOrderResource.class);

        assertThat(api.name()).isEqualTo("order-api");
        var group = api.groups().get("/api/orders");
        assertThat(group).isNotNull();
        assertThat(group.operations()).hasSize(3);
    }

    @Test
    void testBuildUnannotatedResource() {
        var provider = new GenericAnnotationApiProvider("test-api")
                .setTypeAnnotations(TEST_PATH)
                .setMethodAnnotations(TEST_GET, TEST_POST);

        ApiDefinition api = provider.build(NoAnnotationResource.class);
        assertThat(api.groups()).isEmpty();
    }

    @Test
    void testType() {
        var provider = new GenericAnnotationApiProvider("test-api");
        assertThat(provider.type()).isEqualTo("generic-annotations");
    }

    @Test
    void testOperationExtensions() {
        var provider = new GenericAnnotationApiProvider("test-api")
                .setTypeAnnotations(TEST_PATH)
                .setMethodAnnotations(TEST_GET, TEST_POST)
                .setParameterAnnotations(TEST_PATH_PARAM, TEST_QUERY_PARAM);

        ApiDefinition api = provider.build(TestResource.class);
        var group = api.groups().get("/api/users");
        ApiOperation listUsers = group.operations().get("listUsers");

        assertThat(listUsers).isNotNull();
        assertThat(listUsers.extensions()).containsKey("path");
        assertThat(listUsers.extensions()).containsKey("httpMethod");
        assertThat(listUsers.extensions().get("httpMethod")).isEqualTo("TestGet");
    }

    @Test
    void testBuildWithObjectInstance() {
        var provider = new GenericAnnotationApiProvider("test-api")
                .setTypeAnnotations(TEST_PATH)
                .setMethodAnnotations(TEST_GET, TEST_POST)
                .setParameterAnnotations(TEST_PATH_PARAM, TEST_QUERY_PARAM);

        var instance = new TestResource();
        assertThat(provider.canHandle(instance)).isTrue();

        ApiDefinition api = provider.build(instance);
        assertThat(api.groups()).isNotEmpty();
    }

    @Test
    void testCustomPropertyNames() {
        var provider = new GenericAnnotationApiProvider("test-api")
                .setTypeAnnotations(TEST_PATH)
                .setMethodAnnotations(TEST_GET, TEST_POST)
                .setParameterAnnotations(TEST_PATH_PARAM, TEST_QUERY_PARAM)
                .setHttpMethodProperty("description");

        ApiDefinition api = provider.build(TestResource.class);
        var group = api.groups().get("/api/users");

        // The httpMethod should now be read from the "description" property
        // which is empty by default for our test annotations
        assertThat(group.operations()).isNotEmpty();
    }

    @Test
    void testSkipsStaticMethods() {
        // No static methods in TestResource, but verify that static methods
        // would be skipped by checking the SKIP_METHODS set is honored
        var provider = new GenericAnnotationApiProvider("test-api")
                .setTypeAnnotations(TEST_PATH)
                .setMethodAnnotations(TEST_GET, TEST_POST);

        ApiDefinition api = provider.build(TestResource.class);
        // toString is in SKIP_METHODS - ensure it's not in the output
        var group = api.groups().get("/api/users");
        assertThat(group.operations()).doesNotContainKeys("toString", "hashCode", "equals");
    }
}
