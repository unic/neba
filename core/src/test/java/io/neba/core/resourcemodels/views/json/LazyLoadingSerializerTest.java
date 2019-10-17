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
package io.neba.core.resourcemodels.views.json;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import io.neba.api.resourcemodels.Lazy;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class LazyLoadingSerializerTest {
    @Mock
    private SerializerProvider provider;
    @Mock
    private JsonGenerator generator;

    private Lazy<?> value;

    private LazyLoadingSerializer testee;

    @Before
    public void setUp() {
        testee = new LazyLoadingSerializer();
    }

    @Test
    public void testEmptyLazyValueIsConsideredEmpty() {
        withEmptyLazyValue();
        assertSerializerDetectsEmptyValue();
    }

    @Test
    public void testNullLazyValueIsConsideredEmpty() {
        withNullLazyValue();
        assertSerializerDetectsEmptyValue();
    }

    /**
     * {@link Lazy} values are never null by definition, but can only be empty. Consequently, even if null values
     * are omitted in the Jackson serialization, lazy values can still resolve to null.
     */
    @Test
    public void testSerializationToleratesNullValue() throws IOException {
        withNullLazyValue();
        serializeValue();
        verifyNullValueIsSerialized();
    }

    /**
     * {@link Lazy} values are never null by definition, but can only be empty. Consequently, even if null values
     * are omitted in the Jackson serialization, lazy values can still resolve to null.
     */
    @Test
    public void testSerializationToleratesEmptyValue() throws IOException {
        withEmptyLazyValue();
        serializeValue();
        verifyNullValueIsSerialized();
    }

    private void verifyNullValueIsSerialized() throws IOException {
        verify(this.generator).writeObject(isNull());
    }

    private void serializeValue() throws IOException {
        this.testee.serialize(this.value, this.generator, this.provider);
    }

    private void withNullLazyValue() {
        this.value = null;
    }

    private void assertSerializerDetectsEmptyValue() {
        assertThat(testee.isEmpty(provider, this.value)).isTrue();
    }

    private void withEmptyLazyValue() {
        this.value = Optional::empty;
    }
}
