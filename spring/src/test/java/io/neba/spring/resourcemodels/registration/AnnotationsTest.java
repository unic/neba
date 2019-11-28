/*
  Copyright 2013 the original author or authors.
  <p>
  Licensed under the Apache License, Version 2.0 the "License";
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at
  <p>
  http://www.apache.org/licenses/LICENSE-2.0
  <p>
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
 */
package io.neba.spring.resourcemodels.registration;

import org.junit.Test;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;

import static io.neba.spring.resourcemodels.registration.Annotations.annotations;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.assertj.core.api.Assertions.assertThat;

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

    private Annotations testee = annotations(TestType.class);

    @Test
    public void testDetectionOfDirectAnnotation() {
        assertAnnotationIsPresent(TestAnnotation.class);
        assertAnnotationInstanceCanBeObtained(TestAnnotation.class);
    }

    @Test
    public void testDetectionOfMetaAnnotations() {
        assertAnnotationIsPresent(MetaAnnotation.class);
        assertAnnotationInstanceCanBeObtained(MetaAnnotation.class);
    }

    @Test
    public void testDetectionOfCyclicMetaAnnotation() {
        assertAnnotationIsPresent(CyclicAnnotation.class);
        assertAnnotationInstanceCanBeObtained(CyclicAnnotation.class);
    }


    @Test(expected = IllegalArgumentException.class)
    public void testHandlingOfNullElementArgumentForLookup() {
        this.testee.get(null);
    }


    @Test(expected = IllegalArgumentException.class)
    public void testHandlingOfNullTypeArgumentForConstructor() {
        annotations(null);
    }

    @Test
    public void testGetAnnotations() {
        assertAnnotationsAre(MetaAnnotation.class, CyclicAnnotation.class);
    }

    @SafeVarargs
    private final void assertAnnotationsAre(Class<? extends Annotation>... annotations) {
        for (Class<? extends Annotation> annotationType : annotations) {
            assertThat(this.testee.get(annotationType)).isNotNull();
        }
    }

    private void assertAnnotationInstanceCanBeObtained(Class<? extends Annotation> type) {
        assertThat(this.testee.get(type)).isInstanceOf(type);
    }

    private void assertAnnotationIsPresent(Class<? extends Annotation> type) {
        assertThat(this.testee.get(type)).isNotNull();
    }
}
