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

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Olaf Otto
 */
public class ConcurrentLinkedDistinctQueueTest {
	private ConcurrentLinkedDistinctQueue<String> testee;

	@Before
	public void prepare() {
		this.testee = new ConcurrentLinkedDistinctQueue<>();
	}

	@Test
	public void testAddElement() {
		testee.add("one");
		testee.add("one");
		assertListContainsOnly("one");

		testee.add("two");
		assertListContainsOnly("one", "two");
	}

	@Test
	public void testAddAll() {
		List<String> other = new ArrayList<>();
		other.add("one");
		other.add("one");
		other.add("two");
		other.add("two");

		testee.addAll(other);

		assertListContainsOnly("one", "two");
	}

	private void assertListContainsOnly(String... elements) {
		assertThat(this.testee).containsOnly(elements);
	}
}
