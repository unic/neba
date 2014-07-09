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

import static org.fest.assertions.Assertions.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.Bundle;

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
	public void testEqualsHandHashCodeForBundleId() throws Exception {
		OsgiBeanReference<Object> referenceOne = createReference();
		OsgiBeanReference<Object> referenceTwo = createReference();
		
		assertThat(referenceOne).isEqualTo(referenceTwo);
		assertThat(referenceTwo).isEqualTo(referenceOne);
		assertThat(referenceOne.hashCode()).isEqualTo(referenceTwo.hashCode());
		
		withBundleId(100L); 
		referenceTwo = createReference();

		assertThat(referenceOne).isNotEqualTo(referenceTwo);
		assertThat(referenceTwo).isNotEqualTo(referenceOne);
		assertThat(referenceOne.hashCode()).isNotEqualTo(referenceTwo.hashCode());
	}

	@Test
	public void testEqualsHandHashCodedependsOnBeanTypeAndNotOnInstance() throws Exception {
		OsgiBeanReference<Object> referenceOne = createReference();
		
		withNewBean(); 
		OsgiBeanReference<Object> referenceTwo = createReference();

		assertThat(referenceOne).isEqualTo(referenceTwo);
		assertThat(referenceTwo).isEqualTo(referenceOne);
		assertThat(referenceOne.hashCode()).isEqualTo(referenceTwo.hashCode());
		
		withStringBean();
		referenceTwo = createReference();
		assertThat(referenceOne).isNotEqualTo(referenceTwo);
		assertThat(referenceTwo).isNotEqualTo(referenceOne);
		assertThat(referenceOne.hashCode()).isNotEqualTo(referenceTwo.hashCode());
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
		return new OsgiBeanReference<Object>(this.bean, this.bundleId);
	}
}
