/*
  Copyright 2013 the original author or authors.

  Licensed under the Apache License, Version 2.0 the "License";
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
*/

package io.neba.core.resourcemodels.metadata;

import io.neba.api.annotations.AfterMapping;
import io.neba.api.annotations.ResourceModel;
import io.neba.core.resourcemodels.mapping.testmodels.CustomAnnotationWithAfterMappingMetaAnnotation;
import org.junit.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Olaf Otto
 */
public class MethodMetadataTest {
    private MethodMetaData testee;

    @Test
    public void testDetectionOfAfterMappingAnnotation() throws Exception {
        createMetadataForTestModelMethodWithName("afterMapping");
        assertMappingIsAfterMappingCallback();
    }

    @Test
    public void testDetectionOfMetaAfterMappingAnnotation() throws Exception {
        createMetadataForTestModelMethodWithName("afterMappingWithMetaAnnotation");
        assertMappingIsAfterMappingCallback();
    }

    @Test
    public void testHashCodeAndEquals() throws Exception {
        Method method = TestResourceModelWithLifecycleCallbacks.class.getMethod("afterMappingWithMetaAnnotation");

        MethodMetaData one = new MethodMetaData(method);
        MethodMetaData two = new MethodMetaData(method);

        assertThat(one.hashCode()).isEqualTo(two.hashCode());
        assertThat(one).isEqualTo(two);
        assertThat(two).isEqualTo(one);

        method = TestResourceModelWithLifecycleCallbacks.class.getMethod("afterMapping");
        two = new MethodMetaData(method);

        assertThat(one.hashCode()).isNotEqualTo(two.hashCode());
        assertThat(one).isNotEqualTo(two);
        assertThat(two).isNotEqualTo(one);
    }

    private void assertMappingIsAfterMappingCallback() {
        assertThat(this.testee.isAfterMappingCallback()).isTrue();
    }

    private void createMetadataForTestModelMethodWithName(String name) throws NoSuchMethodException {
        Method method = TestResourceModelWithLifecycleCallbacks.class.getMethod(name);
        this.testee = new MethodMetaData(method);
    }

    /**
     * @author Olaf Otto
     */
    @ResourceModel(types = "ignored/junit/test/type")
    public static class TestResourceModelWithLifecycleCallbacks {
        @AfterMapping
        public void afterMapping() {
        }

        @CustomAnnotationWithAfterMappingMetaAnnotation
        public void afterMappingWithMetaAnnotation() {
        }

    }
}
