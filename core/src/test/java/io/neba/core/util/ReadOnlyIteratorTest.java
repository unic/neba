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

import java.util.Iterator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

/**
 * @author Olaf Otto
 */
@RunWith(MockitoJUnitRunner.class)
public class ReadOnlyIteratorTest {
    @Mock
    private Iterator<Object> iterator;

    private ReadOnlyIterator<?> testee;

    @Before
    public void setUp() throws Exception {
        this.testee = new ReadOnlyIterator<>(this.iterator);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testIteratorDoesNotAcceptNullValues() throws Exception {
        new ReadOnlyIterator<>(null);
    }

    @Test
    public void testConvenienceMethodYieldsNewIterator() throws Exception {
        assertThat(ReadOnlyIterator.readOnly(this.iterator)).isNotNull();
    }

    @Test
    public void testHasNextDelegation() throws Exception {
        assertIteratorDoesNotHaveNext();
        verifyHasNextWasInvokedOnWrappedIterator();
    }

    @Test
    public void testNextElementDelegation() throws Exception {
        withNextElement("ELEMENT");
        assertIteratorsNextElementIs("ELEMENT");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testIteratorPreventsModification() throws Exception {
        remove();
    }

    public void remove() {
        this.testee.remove();
    }

    private void assertIteratorsNextElementIs(String expected) {
        assertThat(this.testee.next()).isEqualTo(expected);
    }

    private void withNextElement(String element) {
        doReturn(element).when(this.iterator).next();
    }

    private void verifyHasNextWasInvokedOnWrappedIterator() {
        verify(this.iterator).hasNext();
    }

    private void assertIteratorDoesNotHaveNext() {
        assertThat(this.testee.hasNext()).isFalse();
    }
}