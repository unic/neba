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

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;

import static io.neba.core.util.ConcurrentMultivalueMapAssert.assertThat;

/**
 * @author Olaf Otto
 */
public class ConcurrentDistinctMultiValueMapTest {
	private ConcurrentDistinctMultiValueMap<String, Object> testee;
	
	@Before
	public void prepareTest() {
		this.testee = new ConcurrentDistinctMultiValueMap<>();
	}
	
	@Test
	public void testDistinctValuesAreGuaranteed() throws Exception {
		put("test", "test1");
		put("test", "test1");
		assertOnlyOneValueFor("test");
	}

	@Test
	public void testDifferentValuesAreAccepted() throws Exception {
		put("test", "test1");
		put("test", "test2");
		assertMapContains("test", "test1", "test2");
	}
	
	@Test
	public void testRemoveKey() throws Exception {
		put("test", "test1");
		put("test", "test2");

		removeKey("test");

		assertMapDoesNotContain("test");
	}

	@Test
	public void testRemoveValue() throws Exception {
		put("test", "test1");
		put("test", "test2");

		removeValue("test1");
		assertMapContainsOnly("test", "test2");

		removeValue("test2");
		assertMapContainsOnly("test");
	}

	@Test
    public void testIsEmpty() throws Exception {
        Assertions.assertThat(this.testee.isEmpty()).isTrue();
        put("test", "test1");
        Assertions.assertThat(this.testee.isEmpty()).isFalse();
    }

    private void assertMapDoesNotContain(String key) {
		assertThat(this.testee).doesNotContain(key);
	}

	private void removeKey(String key) {
		this.testee.remove(key);
	}

	private void removeValue(String value) {
		this.testee.removeValue(value);
	}

	private ConcurrentMultivalueMapAssert assertMapContains(String key, Object... value) {
		return assertThat(this.testee).contains(key, value);
	}

	private ConcurrentMultivalueMapAssert assertMapContainsOnly(String key, Object... value) {
		return assertThat(this.testee).containsOnly(key, value);
	}

	private ConcurrentMultivalueMapAssert assertOnlyOneValueFor(String key) {
		return assertThat(this.testee).containsExactlyOneValueFor(key);
	}

	private void put(String key, String value) {
		this.testee.put(key, value);
	}
}
