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
package io.neba.api.resourcemodels;

import org.junit.Test;

import java.util.NoSuchElementException;
import java.util.Optional;

import static java.util.Optional.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * @author Olaf Otto
 */
public class LazyTest {
    private Lazy<Object> testee;

    @Test(expected = NoSuchElementException.class)
    public void testGetIsDelegatedToOptional() throws Exception {
        withoutValue();
        this.testee.get();
    }

    @Test
    public void testIsPresent() throws Exception {
        withoutValue();
        assertThat(this.testee.isPresent()).describedAs("Expected the Lazy value to be present").isFalse();

        withValue();
        assertThat(this.testee.isPresent()).describedAs("Expected the Lazy value not to be present").isTrue();
    }

    @Test
    public void testIfPresent() throws Exception {
        withoutValue();
        this.testee.ifPresent(t -> fail("The value must not be present"));

        withValue();
        final Object[] argument = new Object[1];
        this.testee.ifPresent(t -> argument[0] = t);

        assertThat(argument)
                .describedAs("Expected the consumer to be invoked with the value contained in the Lazy instance")
                .containsExactly("value");
    }

    @Test
    public void testFilter() throws Exception {
        withoutValue();
        this.testee.filter(t -> {
            fail("The value should not be filtered");
            return false;
        });

        withValue();
        final Object[] argument = new Object[1];
        Lazy<Object> filter = this.testee.filter(t -> {
            argument[0] = t;
            return false;
        });

        assertThat(filter.asOptional()).isEmpty();
        assertThat(argument)
                .describedAs("Expected the predicate to be invoked with the value contained in the Lazy instance")
                .containsExactly("value");
    }

    @Test
    public void testMap() throws Exception {
        withoutValue();
        this.testee.map(t -> {
            fail("The value should not be mapped");
            return null;
        });
        assertThat(this.testee.isPresent()).describedAs("Expected the Lazy value not to be present").isFalse();

        withValue();
        assertThat(this.testee
                .map(t -> "mapped")
                .get())
                .isEqualTo("mapped");
    }

    @Test
    public void testFlatMap() throws Exception {
        withoutValue();
        this.testee.flatMap(t -> {
            fail("The value should not be mapped");
            return null;
        });
        assertThat(this.testee.isPresent()).describedAs("Expected the Lazy value not to be present").isFalse();

        withValue();
        assertThat(this.testee
                .flatMap(t -> () -> of("mapped"))
                .get())
                .isEqualTo("mapped");
    }

    @Test
    public void testOrElse() throws Exception {
        withoutValue();
        assertThat(this.testee.orElse("defaultValue")).isEqualTo("defaultValue");

        withValue();
        assertThat(this.testee.orElse("defaultValue")).isEqualTo("value");
    }

    @Test
    public void testOrElseGet() throws Exception {
        withoutValue();
        assertThat(this.testee.orElseGet(() -> "defaultValue")).isEqualTo("defaultValue");

        withValue();
        assertThat(this.testee.orElseGet(() -> "defaultValue")).isEqualTo("value");
    }

    @Test(expected = IllegalStateException.class)
    public void testOrElseThrowWithoutValue() throws Exception {
        withoutValue();
        this.testee.orElseThrow(() -> new IllegalStateException("THIS IS AN EXPECTED TEST EXCEPTION"));
    }

    @Test
    public void testOrElseThrowWithValue() throws Exception {
        withValue();
        assertThat(this.testee.orElseThrow(
                () -> new IllegalStateException("THIS IS AN EXPECTED TEST EXCEPTION"))
        ).isEqualTo("value");
    }

    private void withValue() {
        this.testee = () -> of("value");
    }

    private void withoutValue() {
        this.testee = Optional::empty;
    }
}