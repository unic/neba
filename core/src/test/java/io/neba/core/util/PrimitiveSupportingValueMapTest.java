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

import org.apache.sling.api.resource.ValueMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;


/**
 * @author Olaf Otto
 */
@RunWith(MockitoJUnitRunner.class)
public class PrimitiveSupportingValueMapTest {
	@Mock
	private ValueMap wrapped;

	private Object result;

    private PrimitiveSupportingValueMap testee;

    @Before
    public void prepareValueMap() {
        this.testee = new PrimitiveSupportingValueMap(this.wrapped);
    }

    @Test
    public void testPrimitiveBooleanSupport() throws Exception {
        with(true);
        get(boolean.class);
        assertResultIs(true);
    }

    @Test
    public void testPrimitiveIntSupport() throws Exception {
        with(1);
        get(int.class);
        assertResultIs(1);
    }

    @Test
    public void testPrimitiveLongSupport() throws Exception {
        with(1L);
        get(long.class);
        assertResultIs(1L);
    }

    @Test
    public void testPrimitiveDoubleSupport() throws Exception {
        with(1D);
        get(double.class);
        assertResultIs(1D);
    }

    @Test
    public void testPrimitiveFloatSupport() throws Exception {
        with(1F);
        get(float.class);
        assertResultIs(1F);
    }

    @Test
    public void testPrimitiveByteSupport() throws Exception {
        byte b = 1;
        with(b);
        get(byte.class);
        assertResultIs(b);
    }

    @Test
    public void testPrimitiveCharSupport() throws Exception {
        char c = 1;
        with(c);
        get(char.class);
        assertResultIs(c);
    }

    @Test
    public void testPrimitiveShortSupport() throws Exception {
        short s = 1;
        with(s);
        get(short.class);
        assertResultIs(s);
    }

    @Test
    public void testDefaultValueIsNotUsedIfValueExists() throws Exception {
        with(1F);
        get(2F);
        assertResultIs(1F);
    }

    @Test
    public void testFallbackValueIsUsedIfValueDoesNotExist() throws Exception {
        get(2F);
        assertResultIs(2F);
    }

    @Test
    public void testUntypedGetWithValue() throws Exception {
        with("JUnitTestValue");
        get();
        assertResultIs("JUnitTestValue");
    }

    @Test
    public void testUntypedGetWithoutValue() throws Exception {
        get();
        assertResultIsNull();
    }

    private void assertResultIsNull() {
        assertThat(this.result).isNull();
    }

    private void assertResultIs(Object expected) {
        assertThat(this.result).isEqualTo(expected);
    }

    private <T> void get(Class<T> type) {
        this.result = this.testee.get("test", type);
    }

    private <T> void get(T defaultValue) {
        this.result = this.testee.get("test", defaultValue);
    }

    private void get() {
        this.result = this.testee.get("test");
    }

    private <T> void with(T value) {
        when(this.wrapped.get(eq("test"), eq(typeOf(value)))).thenReturn(value);
        when(this.wrapped.get(eq("test"))).thenReturn(value);
    }
    
	@SuppressWarnings("unchecked")
	private <T> Class<T> typeOf(T value) {
		return (Class<T>) value.getClass();
	}
}
