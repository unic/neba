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

package io.neba.core.util;

import org.junit.Before;
import org.junit.Test;

import static io.neba.core.util.ConcurrentMultiValueMapAssert.assertThat;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Olaf Otto
 */
public class ConcurrentDistinctMultiValueMapTest {
    private ConcurrentDistinctMultiValueMap<String, String> testee;

    @Before
    public void prepareTest() {
        this.testee = new ConcurrentDistinctMultiValueMap<>();
    }

    @Test
    public void testDistinctValuesAreGuaranteed() {
        this.testee.put("test", "test1");
        this.testee.put("test", "test1");

        assertThat(this.testee).containsExactlyOneValueFor("test");
    }

    @Test
    public void testDifferentValuesAreAccepted() {
        this.testee.put("test", "test1");
        this.testee.put("test", "test2");

        assertThat(this.testee).contains("test", "test1", "test2");
    }

    @Test
    public void testRemoveKey() {
        this.testee.put("test", "test1");
        this.testee.put("test", "test2");
        this.testee.remove("test");

        assertThat(this.testee).doesNotContain("test");
    }

    @Test
    public void testRemoveValue() {
        this.testee.put("test", "test1");
        this.testee.put("test", "test2");
        this.testee.removeValue("test1");

        assertThat(this.testee).containsOnly("test", "test2");

        this.testee.removeValue("test2");

        assertThat(this.testee).containsOnly("test");
    }

    @Test
    public void testIsEmpty() {
        this.testee.put("test", "test1");

        assertThat(this.testee.isEmpty()).isFalse();
    }

    @Test
    public void testComputeIfAbsentOfExistingEntry() {
        this.testee.put("key", "existing value");

        assertThat(this.testee.computeIfAbsent("key", key -> singleton("new value"))).containsOnly("existing value");
        assertThat(this.testee).containsOnly("key", "existing value");
    }

    @Test
    public void testComputeIfAbsentOfNonExistingEntry() {
        assertThat(this.testee.computeIfAbsent("key", key -> singleton("previously absent value"))).containsOnly("previously absent value");
        assertThat(this.testee).containsOnly("key", "previously absent value");
    }

    @Test
    public void testMultiValuePutAddsValues() {
        testee.put("key", "value");
        testee.put("key", singletonList("additional value"));

        assertThat(testee).containsOnly("key", "value", "additional value");
    }

    @Test
    public void testMultiValuePutInitializesValues() {
        testee.put("key", singletonList("value"));

        assertThat(testee).containsOnly("key", "value");
    }

    @Test
    public void testShallowCopyContainsAllData() {
        testee.put("key", "value");

        assertThat(testee.shallowCopy()).containsOnlyKeys("key");
        assertThat(testee.shallowCopy().get("key")).containsExactly("value");
    }

    @Test
    public void testShallowCopyReturnsShallowCopy() {
        testee.put("key", "value");
        testee.shallowCopy().clear();

        assertThat(testee).containsOnly("key", "value");

        testee.shallowCopy().get("key").add("additional value");

        assertThat(testee).containsOnly("key", "value");
    }

    @Test
    public void testGetValues() {
        testee.put("key", "value1");
        testee.put("key", "value2");

        assertThat(testee.values()).hasSize(1);
        assertThat(testee.values().iterator().next()).containsExactly("value1", "value2");
    }
}
