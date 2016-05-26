/**
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 the "License";
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/
package io.neba.core.util;

import org.junit.Before;
import org.junit.Test;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.util.HashSet;
import java.util.Set;

import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Olaf Otto
 */
public class AnnotationsTest {
    @TestAnnotation
    private static class TestType {
    }

    @Retention(RUNTIME)
    @MetaAnnotation
    private @interface TestAnnotation {
    }

    @Retention(RUNTIME)
    @CyclicAnnotation
    private @interface MetaAnnotation {
    }

    @Retention(RUNTIME)
    @MetaAnnotation
    @CyclicAnnotation
    private @interface CyclicAnnotation {
    }

    private Set<Annotation> allAnnotations;

    private Annotations testee = new Annotations(TestType.class);

    @Before
    public void setUp() throws Exception {
        this.allAnnotations = new HashSet<>();
        this.allAnnotations.addAll(asList(TestType.class.getAnnotations()));
        this.allAnnotations.addAll(asList(TestAnnotation.class.getAnnotations()));
        this.allAnnotations.addAll(asList(MetaAnnotation.class.getAnnotations()));
        this.allAnnotations.addAll(asList(CyclicAnnotation.class.getAnnotations()));
        this.allAnnotations.addAll(asList(Retention.class.getAnnotations()));
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

    @Test
    public void testContainsAnnotationWithName() throws Exception {
        assertAnnotationWithNameIsNotPresent("does.no.Exist");
        assertAnnotationWithNameIsPresent(TestAnnotation.class.getName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testHandlingOfNullElementArgumentForLookup() throws Exception {
        this.testee.get(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testHandlingOfNullElementArgumentForExistenceTest() throws Exception {
        this.testee.contains(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testHandlingOfNullTypeArgumentForConstructor() throws Exception {
        new Annotations(null);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGetAnnotations() throws Exception {
        assertAnnotationsAre(MetaAnnotation.class, CyclicAnnotation.class);
    }

    @Test
    public void testIteratorProvidesExpectedAnnotations() throws Exception {
        assertThat(this.testee.iterator()).containsOnly(this.allAnnotations.toArray(new Annotation[]{}));
    }

    @SafeVarargs
    private final void assertAnnotationsAre(Class<? extends Annotation>... annotations) {
        for (Class<? extends Annotation> annotationType : annotations) {
            assertThat(this.testee.contains(annotationType)).isTrue();
            assertThat(this.testee.get(annotationType)).isNotNull();
        }
    }

    private void assertAnnotationInstanceCanBeObtained(Class<? extends Annotation> type) {
        assertThat(this.testee.get(type)).isInstanceOf(type);
    }

    private void assertAnnotationIsPresent(Class<? extends Annotation> type) {
        assertThat(this.testee.contains(type)).isTrue();
    }

    private void assertAnnotationWithNameIsPresent(String typeName) {
        assertThat(this.testee.containsName(typeName)).isTrue();
    }

    private void assertAnnotationWithNameIsNotPresent(String typeName) {
        assertThat(this.testee.containsName(typeName)).isFalse();
    }
}