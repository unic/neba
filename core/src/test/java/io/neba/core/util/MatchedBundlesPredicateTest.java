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
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.Bundle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * @author Olaf Otto
 */
@RunWith(MockitoJUnitRunner.class)
public class MatchedBundlesPredicateTest {
	@Mock
	private Bundle bundle;
	@Mock
	private OsgiBeanSource<?> source;
	
	private long bundleId = 123L;

	private MatchedBundlesPredicate testee;

	@Before
	public void preparePredicate() {
		when(this.bundle.getBundleId()).thenReturn(this.bundleId);
		this.testee = new MatchedBundlesPredicate(this.bundle);
	}

	@Test
	public void testPredicateReturnsFalseForMatchedBundles() throws Exception {
		withSourceReferencingCurrentBundle();
		assertPredicateSignalsThatReferenceIsInvalid();
	}

	@Test
	public void testPredicateReturnsTrueForUnmatchedBundles() throws Exception {
		withSourceReferencingOtherBundle();
		assertPredicateSignalsThatReferenceIsValid();
	}

	private void withSourceReferencingOtherBundle() {
		when(this.source.getBundleId()).thenReturn(-1L);
	}

	private void assertPredicateSignalsThatReferenceIsValid() {
		assertThat(this.testee.evaluate(this.source)).isTrue();
	}

	private void assertPredicateSignalsThatReferenceIsInvalid() {
		assertThat(this.testee.evaluate(this.source)).isFalse();
	}

	private void withSourceReferencingCurrentBundle() {
		when(this.source.getBundleId()).thenReturn(this.bundleId);
	}
}
