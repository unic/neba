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

package io.neba.core.resourcemodels.metadata;

import io.neba.core.resourcemodels.mapping.testmodels.TestResourceModelWithLifecycleCallbacks;
import org.junit.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.util.ReflectionUtils.findMethod;

/**
 * @author Olaf Otto
 */
public class MethodMetadataTest {
	private MethodMetaData testee;

	@Test
	public void testDetectionOfPreMappingAnnotation() throws Exception {
		createMetadataForTestModelMethodWithName("beforeMapping");
		assertMappingIsPreMappingCallback();
		assertMappingIsNotPostMappingCallback();
	}

	@Test
	public void testDetectionOfMetaPreMappingAnnotation() throws Exception {
		createMetadataForTestModelMethodWithName("beforeMappingWithMetaAnnotation");
		assertMappingIsPreMappingCallback();
		assertMappingIsNotPostMappingCallback();
	}

	@Test
	public void testDetectionOfPostMappingAnnotation() throws Exception {
		createMetadataForTestModelMethodWithName("afterMapping");
		assertMappingIsPostMappingCallback();
		assertMappingIsNotPreMappingCallback();
	}

	@Test
	public void testDetectionOfMetaPostMappingAnnotation() throws Exception {
		createMetadataForTestModelMethodWithName("afterMappingWithMetaAnnotation");
		assertMappingIsPostMappingCallback();
		assertMappingIsNotPreMappingCallback();
	}

	@Test
    public void testHashCodeAndEquals() throws Exception {
        Method method = findMethod(TestResourceModelWithLifecycleCallbacks.class, "beforeMapping");

        MethodMetaData one = new MethodMetaData(method);
        MethodMetaData two = new MethodMetaData(method);

        assertThat(one.hashCode()).isEqualTo(two.hashCode());
        assertThat(one).isEqualTo(two);
        assertThat(two).isEqualTo(one);

        method = findMethod(TestResourceModelWithLifecycleCallbacks.class, "afterMapping");
        two = new MethodMetaData(method);

        assertThat(one.hashCode()).isNotEqualTo(two.hashCode());
        assertThat(one).isNotEqualTo(two);
        assertThat(two).isNotEqualTo(one);
    }

    private void assertMappingIsPostMappingCallback() {
		assertThat(this.testee.isPostMappingCallback()).isTrue();
	}

	private void assertMappingIsNotPostMappingCallback() {
		assertThat(this.testee.isPostMappingCallback()).isFalse();
	}

	private void assertMappingIsPreMappingCallback() {
		assertThat(this.testee.isPreMappingCallback()).isTrue();
	}

	private void assertMappingIsNotPreMappingCallback() {
		assertThat(this.testee.isPreMappingCallback()).isFalse();
	}

	private void createMetadataForTestModelMethodWithName(String name) {
		Method method = findMethod(TestResourceModelWithLifecycleCallbacks.class, name);
		this.testee = new MethodMetaData(method);
	}
}
