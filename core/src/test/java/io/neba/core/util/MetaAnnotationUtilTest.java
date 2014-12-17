package io.neba.core.util;

import org.junit.Test;
import org.junit.runners.model.TestClass;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.reflect.AnnotatedElement;
import java.util.Map;

import static io.neba.core.util.MetaAnnotationUtil.getAnnotation;
import static io.neba.core.util.MetaAnnotationUtil.getAnnotations;
import static io.neba.core.util.MetaAnnotationUtil.isAnnotationPresent;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.fest.assertions.Assertions.assertThat;

/**
 * @author Olaf Otto
 */
public class MetaAnnotationUtilTest {
    @TestAnnotation
    private static class TestType {
    }

    @Retention(RUNTIME)
    @MetaAnnotation
    private static @interface TestAnnotation {
    }

    @Retention(RUNTIME)
    @CyclicAnnotation
    private static @interface MetaAnnotation {
    }

    @Retention(RUNTIME)
    @MetaAnnotation
    @CyclicAnnotation
    private static @interface CyclicAnnotation {
    }

    @Test
    public void testDetectionOfDirectAnnotation() throws Exception {
        assertAnnotationIsPresent(TestAnnotation.class);
        assertAnnotationInstanceCanBeObtained(TestAnnotation.class);
    }

    @Test
    public void testDetectionOfMetaAnnotations() throws Exception {
        assertAnnotationIsPresent(MetaAnnotation.class);
        assertAnnotationInstanceCanBeObtained(MetaAnnotation.class);
    }

    @Test
    public void testDetectionOfCyclicMetaAnnotation() throws Exception {
        assertAnnotationIsPresent(CyclicAnnotation.class);
        assertAnnotationInstanceCanBeObtained(CyclicAnnotation.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testHandlingOfNullElementArgumentForLookup() throws Exception {
        getAnnotation(TestClass.class, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testHandlingOfNullElementArgumentForExistenceTest() throws Exception {
        isAnnotationPresent(TestClass.class, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testHandlingOfNullTypeArgumentForLookup() throws Exception {
        getAnnotation(null, MetaAnnotation.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testHandlingOfNullTypeArgumentForExistenceTest() throws Exception {
        isAnnotationPresent(null, MetaAnnotation.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGetAnnotations() throws Exception {
        assertAnnotationsAre(TestType.class, TestAnnotation.class, MetaAnnotation.class, CyclicAnnotation.class);
    }

    private void assertAnnotationsAre(AnnotatedElement type, Class<? extends Annotation>... annotations) {
        Map<Class<? extends Annotation>, Annotation> detectedAnnotations = getAnnotations(type);
        for (Class<? extends Annotation> annotationType : annotations) {
            assertThat(detectedAnnotations.keySet()).contains(annotationType);
            assertThat(detectedAnnotations.get(annotationType)).isInstanceOf(annotationType);
        }
    }

    private void assertAnnotationInstanceCanBeObtained(Class<? extends Annotation> type) {
        assertThat(getAnnotation(TestType.class, type)).isInstanceOf(type);
    }

    private void assertAnnotationIsPresent(Class<? extends Annotation> type) {
        assertThat(isAnnotationPresent(TestType.class, type)).isTrue();
    }
}