package ssg.rwamp.api.webservices;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class AnnotationScannerTest {

    private AnnotationScanner scanner;

    @BeforeEach
    void setUp() {
        scanner = new AnnotationScanner();
    }

    @Test
    void testLoadExistingAnnotation() {
        Class<?> clazz = scanner.loadAnnotation(
                "ssg.rwamp.api.webservices.annotations.TestPath");
        assertThat(clazz).isNotNull();
        assertThat(clazz.isAnnotation()).isTrue();
    }

    @Test
    void testLoadNonExistentAnnotation() {
        Class<?> clazz = scanner.loadAnnotation(
                "com.nonexistent.annotation.DoesNotExist");
        assertThat(clazz).isNull();
    }

    @Test
    void testLoadNonAnnotationClass() {
        Class<?> clazz = scanner.loadAnnotation("java.lang.String");
        assertThat(clazz).isNull();
    }

    @Test
    void testMatchesExistingAnnotation() {
        var resource = new TestResource();
        Annotation pathAnno = resource.getClass().getAnnotation(
                ssg.rwamp.api.webservices.annotations.TestPath.class);
        assertThat(pathAnno).isNotNull();

        boolean matches = scanner.matches(pathAnno,
                "ssg.rwamp.api.webservices.annotations.TestPath");
        assertThat(matches).isTrue();
    }

    @Test
    void testMatchesNonExistingAnnotation() {
        var resource = new TestResource();
        Annotation pathAnno = resource.getClass().getAnnotation(
                ssg.rwamp.api.webservices.annotations.TestPath.class);

        boolean matches = scanner.matches(pathAnno,
                "com.nonexistent.FakeAnnotation");
        assertThat(matches).isFalse();
    }

    @Test
    void testMatchesNullAnnotation() {
        assertThat(scanner.matches(null, "some.class")).isFalse();
    }

    @Test
    void testMatchesMultipleClassNames() {
        var resource = new TestResource();
        Annotation pathAnno = resource.getClass().getAnnotation(
                ssg.rwamp.api.webservices.annotations.TestPath.class);

        assertThat(scanner.matches(pathAnno,
                "com.nonexistent.Fake",
                "ssg.rwamp.api.webservices.annotations.TestPath")).isTrue();
    }

    @Test
    void testFindClassAnnotation() {
        Annotation anno = scanner.findClassAnnotation(
                TestResource.class,
                "ssg.rwamp.api.webservices.annotations.TestPath");
        assertThat(anno).isNotNull();
    }

    @Test
    void testFindMethodAnnotation() throws Exception {
        Method method = TestResource.class.getMethod("listUsers");
        Annotation anno = scanner.findMethodAnnotation(method,
                "ssg.rwamp.api.webservices.annotations.TestGet");
        assertThat(anno).isNotNull();
    }

    @Test
    void testFindAnnotationNotFound() throws Exception {
        Method method = TestResource.class.getMethod("listUsers");
        Annotation anno = scanner.findMethodAnnotation(method,
                "com.nonexistent.FakeAnnotation");
        assertThat(anno).isNull();
    }

    @Test
    void testGetStringValue() {
        var resource = new TestResource();
        Annotation pathAnno = resource.getClass().getAnnotation(
                ssg.rwamp.api.webservices.annotations.TestPath.class);

        String value = scanner.getString(pathAnno, "value");
        assertThat(value).isEqualTo("/api/users");
    }

    @Test
    void testGetStringValueNotFound() {
        var resource = new TestResource();
        Annotation pathAnno = resource.getClass().getAnnotation(
                ssg.rwamp.api.webservices.annotations.TestPath.class);

        String value = scanner.getString(pathAnno, "nonexistent");
        assertThat(value).isNull();
    }

    @Test
    void testGetStringValueNullAnnotation() {
        assertThat(scanner.getString(null, "value")).isNull();
        assertThat(scanner.getString(null, null)).isNull();
    }

    @Test
    void testGetStringList() {
        var resource = new TestResource();
        Annotation consumesAnno = resource.getClass().getAnnotation(
                ssg.rwamp.api.webservices.annotations.TestConsumes.class);

        var values = scanner.getStringList(consumesAnno, "value");
        assertThat(values).containsExactly("application/json");
    }

    @Test
    void testGetStringListEmpty() {
        assertThat(scanner.getStringList(null, "value")).isEmpty();
        assertThat(scanner.getStringList(null, null)).isEmpty();
    }

    @Test
    void testGetBoolean() {
        var resource = new TestResource();
        Annotation pathAnno = resource.getClass().getAnnotation(
                ssg.rwamp.api.webservices.annotations.TestPath.class);
        Boolean value = scanner.getBoolean(pathAnno, "value");
        assertThat(value).isNull(); // "value" is a String, not Boolean
    }

    @Test
    void testHasAny() {
        assertThat(scanner.hasAny(
                "ssg.rwamp.api.webservices.annotations.TestPath")).isTrue();
        assertThat(scanner.hasAny(
                "com.nonexistent.Fake1",
                "com.nonexistent.Fake2")).isFalse();
    }

    @Test
    void testHasAll() {
        assertThat(scanner.hasAll(
                "ssg.rwamp.api.webservices.annotations.TestPath",
                "ssg.rwamp.api.webservices.annotations.TestGet")).isTrue();
        assertThat(scanner.hasAll(
                "ssg.rwamp.api.webservices.annotations.TestPath",
                "com.nonexistent.Fake")).isFalse();
    }

    @Test
    void testLoadableAnnotations() {
        var loadable = scanner.loadableAnnotations(
                "ssg.rwamp.api.webservices.annotations.TestPath",
                "com.nonexistent.Fake",
                "ssg.rwamp.api.webservices.annotations.TestGet");
        assertThat(loadable).hasSize(2);
        assertThat(loadable).containsKey(
                "ssg.rwamp.api.webservices.annotations.TestPath");
        assertThat(loadable).containsKey(
                "ssg.rwamp.api.webservices.annotations.TestGet");
    }

    @Test
    void testCacheBehavior() {
        Class<?> first = scanner.loadAnnotation(
                "ssg.rwamp.api.webservices.annotations.TestPath");
        Class<?> second = scanner.loadAnnotation(
                "ssg.rwamp.api.webservices.annotations.TestPath");
        assertThat(first).isSameAs(second);
    }

    @Test
    void testCustomClassLoader() {
        var customScanner = new AnnotationScanner(
                TestResource.class.getClassLoader());
        assertThat(customScanner.loadAnnotation(
                "ssg.rwamp.api.webservices.annotations.TestPath")).isNotNull();
    }
}
