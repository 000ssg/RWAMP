package ssg.rwamp.api.webservices;

import ssg.rwamp.api.provider.model.ApiDefinition;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class JaxRsApiProviderTest {

    @Test
    void testNamespaceDetection() {
        var provider = new JaxRsApiProvider("test-api");

        String ns = provider.detectedNamespace();

        if (ns != null) {
            assertThat(ns).isIn(JaxRsApiProvider.JAKARTA_NS, JaxRsApiProvider.JAVAX_NS);
            assertThat(provider.isOperable()).isTrue();
        } else {
            assertThat(provider.isOperable()).isFalse();
        }
    }

    @Test
    void testType() {
        var provider = new JaxRsApiProvider("test-api");
        assertThat(provider.type()).isEqualTo("jax-rs");
    }

    @Test
    void testBuildNonOperable() {
        var scanner = new AnnotationScanner() {
            @Override
            public Class<?> loadAnnotation(String className) {
                if (className.startsWith("jakarta.ws.rs") || className.startsWith("javax.ws.rs")) {
                    return null;
                }
                return super.loadAnnotation(className);
            }
        };

        var provider = new JaxRsApiProvider("test-api", scanner);
        assertThat(provider.isOperable()).isFalse();

        ApiDefinition api = provider.build(Object.class);
        assertThat(api.name()).isEqualTo("test-api");
        assertThat(api.groups()).isEmpty();
    }

    @Test
    void testCanHandleNonOperable() {
        var scanner = new AnnotationScanner() {
            @Override
            public Class<?> loadAnnotation(String className) {
                if (className.startsWith("jakarta.ws.rs") || className.startsWith("javax.ws.rs")) {
                    return null;
                }
                return super.loadAnnotation(className);
            }
        };

        var provider = new JaxRsApiProvider("test-api", scanner);
        assertThat(provider.canHandle(null)).isFalse();
        assertThat(provider.canHandle(Object.class)).isFalse();
    }

    @Test
    void testNamespaceConstants() {
        assertThat(JaxRsApiProvider.JAKARTA_NS).isEqualTo("jakarta.ws.rs");
        assertThat(JaxRsApiProvider.JAVAX_NS).isEqualTo("javax.ws.rs");
    }

    @Test
    void testDelegatePattern() throws Exception {
        Method detectMethod = JaxRsApiProvider.class.getDeclaredMethod("detectNamespace");
        detectMethod.setAccessible(true);

        var provider = new JaxRsApiProvider("test-api");
        String ns = (String) detectMethod.invoke(provider);

        if (ns != null) {
            assertThat(provider.isOperable()).isTrue();
        }
    }

    @Test
    void testJaxRsNotOnClasspath() {
        // Simulate JAX-RS not being on the classpath
        var scanner = new AnnotationScanner() {
            @Override
            public Class<?> loadAnnotation(String className) {
                return null; // Nothing loadable
            }
        };

        var provider = new JaxRsApiProvider("test-api", scanner);
        assertThat(provider.isOperable()).isFalse();
        assertThat(provider.detectedNamespace()).isNull();
        assertThat(provider.canHandle(TestResource.class)).isFalse();
    }

    @Test
    void testJaxRsJakartaDetection() {
        // Simulate Jakarta EE namespace detection
        var scanner = new AnnotationScanner() {
            @Override
            public Class<?> loadAnnotation(String className) {
                if ("jakarta.ws.rs.Path".equals(className)) {
                    return ssg.rwamp.api.webservices.annotations.TestPath.class;
                }
                if (className.startsWith("jakarta.ws.rs") && className.endsWith(".GET")) {
                    return ssg.rwamp.api.webservices.annotations.TestGet.class;
                }
                if (className.startsWith("jakarta.ws.rs") && className.endsWith(".POST")) {
                    return ssg.rwamp.api.webservices.annotations.TestPost.class;
                }
                if (className.contains("PathParam")) {
                    return ssg.rwamp.api.webservices.annotations.TestPathParam.class;
                }
                if (className.contains("QueryParam")) {
                    return ssg.rwamp.api.webservices.annotations.TestQueryParam.class;
                }
                if (className.contains("HeaderParam") || className.contains("FormParam") ||
                        className.contains("CookieParam")) {
                    return ssg.rwamp.api.webservices.annotations.TestQueryParam.class;
                }
                if (className.contains("Consumes")) {
                    return ssg.rwamp.api.webservices.annotations.TestConsumes.class;
                }
                if (className.contains("Produces")) {
                    return ssg.rwamp.api.webservices.annotations.TestProduces.class;
                }
                return null;
            }
        };

        var provider = new JaxRsApiProvider("test-api", scanner);
        assertThat(provider.detectedNamespace()).isEqualTo("jakarta.ws.rs");
        assertThat(provider.isOperable()).isTrue();

        // Now it should be able to handle TestResource which has @TestPath
        ApiDefinition api = provider.build(TestResource.class);
        assertThat(api.name()).isEqualTo("test-api");
        assertThat(api.groups()).isNotEmpty();
    }

    @Test
    void testJaxRsJavaxDetection() {
        // Simulate javax namespace detection (no jakarta, but javax present)
        var scanner = new AnnotationScanner() {
            @Override
            public Class<?> loadAnnotation(String className) {
                if (className.startsWith("jakarta")) return null;
                if ("javax.ws.rs.Path".equals(className)) {
                    return ssg.rwamp.api.webservices.annotations.TestPath.class;
                }
                if (className.startsWith("javax.ws.rs") && className.endsWith(".GET")) {
                    return ssg.rwamp.api.webservices.annotations.TestGet.class;
                }
                if (className.startsWith("javax.ws.rs") && className.endsWith(".POST")) {
                    return ssg.rwamp.api.webservices.annotations.TestPost.class;
                }
                if (className.contains("PathParam")) {
                    return ssg.rwamp.api.webservices.annotations.TestPathParam.class;
                }
                if (className.contains("QueryParam")) {
                    return ssg.rwamp.api.webservices.annotations.TestQueryParam.class;
                }
                if (className.contains("HeaderParam") || className.contains("FormParam") ||
                        className.contains("CookieParam")) {
                    return ssg.rwamp.api.webservices.annotations.TestQueryParam.class;
                }
                if (className.contains("Consumes")) {
                    return ssg.rwamp.api.webservices.annotations.TestConsumes.class;
                }
                if (className.contains("Produces")) {
                    return ssg.rwamp.api.webservices.annotations.TestProduces.class;
                }
                return null;
            }
        };

        var provider = new JaxRsApiProvider("test-api", scanner);
        assertThat(provider.detectedNamespace()).isEqualTo("javax.ws.rs");
        assertThat(provider.isOperable()).isTrue();

        ApiDefinition api = provider.build(TestResource.class);
        assertThat(api.name()).isEqualTo("test-api");
        assertThat(api.groups()).isNotEmpty();
    }
}
