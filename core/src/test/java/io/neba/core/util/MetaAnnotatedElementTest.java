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

import org.junit.Test;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.fest.assertions.Assertions.assertThat;

/**
 * @author Olaf Otto
 */
public class MetaAnnotatedElementTest {
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

    private MetaAnnotatedElement testee = new MetaAnnotatedElement(TestType.class);

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
        this.testee.getAnnotation(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testHandlingOfNullElementArgumentForExistenceTest() throws Exception {
        this.testee.isAnnotatedWith(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testHandlingOfNullTypeArgumentForConstructor() throws Exception {
        new MetaAnnotatedElement(null);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testGetAnnotations() throws Exception {
        assertAnnotationsAre(MetaAnnotation.class, CyclicAnnotation.class);
    }

    private void assertAnnotationsAre(Class<? extends Annotation>... annotations) {
        for (Class<? extends Annotation> annotationType : annotations) {
            assertThat(this.testee.isAnnotatedWith(annotationType)).isTrue();
            assertThat(this.testee.getAnnotation(annotationType)).isNotNull();
        }
    }

    private void assertAnnotationInstanceCanBeObtained(Class<? extends Annotation> type) {
        assertThat(this.testee.getAnnotation(type)).isInstanceOf(type);
    }

    private void assertAnnotationIsPresent(Class<? extends Annotation> type) {
        assertThat(this.testee.isAnnotatedWith(type)).isTrue();
    }
}