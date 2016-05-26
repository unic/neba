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
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.Bundle;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Olaf Otto
 */
@RunWith(MockitoJUnitRunner.class)
public class OsgiBeanReferenceTest {
	@Mock
	private Bundle bundle;
	private Object bean = new Object();
	private long bundleId = 123L;

	@Test
	public void testEqualsAndHashCodeForBundleId() throws Exception {
		OsgiBeanReference<Object> referenceOne = createReference();
		OsgiBeanReference<Object> referenceTwo = createReference();
		
		assertThat(referenceOne.equals(referenceTwo)).isTrue();
		assertThat(referenceTwo.equals(referenceOne)).isTrue();
		assertThat(referenceOne.hashCode()).isEqualTo(referenceTwo.hashCode());
		
		withBundleId(100L); 
		referenceTwo = createReference();

		assertThat(referenceOne.equals(referenceTwo)).isFalse();
		assertThat(referenceTwo.equals(referenceOne)).isFalse();
		assertThat(referenceOne.hashCode()).isNotEqualTo(referenceTwo.hashCode());
	}

	@Test
	public void testEqualsHandHashCodeDependsOnBeanTypeAndNotOnInstance() throws Exception {
		OsgiBeanReference<Object> referenceOne = createReference();
		
		withNewBean(); 
		OsgiBeanReference<Object> referenceTwo = createReference();

		assertThat(referenceOne.equals(referenceTwo)).isTrue();
		assertThat(referenceTwo.equals(referenceOne));
		assertThat(referenceOne.hashCode()).isEqualTo(referenceTwo.hashCode());
		
		withStringBean();
		referenceTwo = createReference();
		assertThat(referenceOne.equals(referenceTwo)).isFalse();
		assertThat(referenceTwo.equals(referenceOne)).isFalse();
		assertThat(referenceOne.hashCode()).isNotEqualTo(referenceTwo.hashCode());
	}

    @Test
    public void testEqualsSelf() throws Exception {
        OsgiBeanReference<Object> reference = createReference();
        assertThat(reference.equals(reference)).isTrue();
    }

    @Test
    public void testEqualsWithOtherType() throws Exception {
        OsgiBeanReference<Object> reference = createReference();
        assertThat(reference.equals("")).isFalse();
    }

    @Test
    public void testEqualsToNull() throws Exception {
        OsgiBeanReference<Object> reference = createReference();
        assertThat(reference.equals(null)).isFalse();
    }

    @Test
    public void testToStringRepresentation() throws Exception {
        assertThat(createReference().toString()).isEqualTo("Bean with type \"java.lang.Object\" from bundle with id 123");
    }

    private void withStringBean() {
		this.bean = "";
	}

	private void withNewBean() {
		this.bean = new Object();
	}

	private void withBundleId(long l) {
		this.bundleId = l;
	}

	private OsgiBeanReference<Object> createReference() {
		return new OsgiBeanReference<>(this.bean, this.bundleId);
	}
}
