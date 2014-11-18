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

package io.neba.core.rendering;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;

/**
 * @author Olaf Otto
 */
@RunWith(MockitoJUnitRunner.class)
public class VelocityBindingsTest {
    private VelocityBindings testee = new VelocityBindings();

    @Test
    public void testClear() throws Exception {
        put("a", "b");
        assertBindingsSizeIs(1);

        clear();

        assertBindingsSizeIs(0);
        assertBindingsAreEmpty();
    }

    @Test
    public void testContains() throws Exception {
        assertBindingsDoesNotContain("a");

        put("a", "b");

        assertBindingsContains("a");
    }

    @Test
    public void testContainsValue() throws Exception {
        assertBindingsDoesNotContainValue("b");

        put("a", "b");

        assertBindingsContainsValue("b");
    }

    @Test
    public void testEntrySet() throws Exception {
        put("a", "b");
        put("b", "c");

        assertEntrySetHasSize(2);
    }

    @Test
    public void testGet() throws Exception {
        assertBindingsReturnsNullFor("a");

        put("a", "b");

        assertBindingsReturns("a", "b");
    }

    @Test
    public void testKeySet() throws Exception {
        put("a", "b");
        put("b", "c");

        assertBindingsKeySetIs("a", "b");
    }

    @Test
    public void testPutAll() throws Exception {
        Map<String, Object> source = new HashMap<String, Object>();
        source.put("a", "b");
        source.put("b", "c");

        putAll(source);

        assertEntrySetHasSize(2);
        assertBindingsKeySetIs("a", "b");
    }

    @Test
    public void testRemove() throws Exception {
        put("a", "b");
        assertBindingsContains("a");

        remove("a");

        assertBindingsAreEmpty();
    }

    @Test
    public void testSize() throws Exception {
        assertBindignsSizeIs(0);

        put("a", "b");

        assertBindignsSizeIs(1);
    }

    @Test
    public void testName() throws Exception {
        assertBindingsValuesAre();

        put("a", "b");

        assertBindingsValuesAre("b");
    }

    private void assertBindingsValuesAre(Object... values) {
        assertThat(this.testee.values()).containsOnly(values);
    }

    private void assertBindignsSizeIs(int expected) {
        assertThat(this.testee.size()).isEqualTo(expected);
    }

    private void remove(String key) {
        this.testee.remove(key);
    }

    private void putAll(Map<String, Object> source) {
        this.testee.putAll(source);
    }

    private void assertBindingsKeySetIs(Object... keys) {
        assertThat(this.testee.keySet()).containsOnly(keys);
    }

    private void assertBindingsReturns(String key, Object value) {
        assertThat(this.testee.get(key)).isEqualTo(value);
    }

    private void assertBindingsReturnsNullFor(String a) {
        assertThat(this.testee.get(a)).isNull();
    }

    private void assertEntrySetHasSize(int expected) {
        assertThat(this.testee.entrySet()).hasSize(expected);
    }

    private void assertBindingsContainsValue(Object value) {
        assertThat(this.testee.containsValue(value)).isTrue();
    }

    private void assertBindingsDoesNotContainValue(Object value) {
        assertThat(this.testee.containsValue(value)).isFalse();
    }

    private void assertBindingsContains(String key) {
        assertThat(this.testee.containsKey(key)).isTrue();
    }

    private void assertBindingsDoesNotContain(String key) {
        assertThat(this.testee.containsKey(key)).isFalse();
    }

    private boolean assertBindingsAreEmpty() {
        return this.testee.isEmpty();
    }

    private void clear() {
        this.testee.clear();
    }

    private void put(String key, String value) {
        this.testee.put(key, value);
    }

    private void assertBindingsSizeIs(int expected) {
        assertBindignsSizeIs(expected);
    }
}