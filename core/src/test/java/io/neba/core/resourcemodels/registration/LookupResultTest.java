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

package io.neba.core.resourcemodels.registration;

import io.neba.core.util.OsgiBeanSource;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * @author Olaf Otto
 */
@RunWith(MockitoJUnitRunner.class)
public class LookupResultTest {
    @Mock
    private OsgiBeanSource<?> beanSource;
    private LookupResult testee;

    @Before
    public void setUp() throws Exception {
        this.testee = new LookupResult(this.beanSource, "junit/test/type");
        doReturn(123L).when(this.beanSource).getBundleId();
        doReturn(Object.class).when(this.beanSource).getBeanType();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorArgumentSourceMustNotBeNull() throws Exception {
        new LookupResult(null, "some/resource/type");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorArgumentResourceTypeMustNotBeNull() throws Exception {
        new LookupResult(this.beanSource, null);
    }

    @Test
    public void testEqualsNull() throws Exception {
        assertLookupResultIsNotEqualTo(null);
    }

    @Test
    public void testEqualToOtherType() throws Exception {
        assertLookupResultIsNotEqualTo(new Object());
    }

    @Test
    public void testEqualsToResultForOtherSource() throws Exception {
        OsgiBeanSource<?> beanSource = mock(OsgiBeanSource.class);
        LookupResult other = new LookupResult(beanSource, "junit/test/type");

        assertLookupResultIsNotEqualTo(other);
    }

    @Test
    public void testEqualsToResultForDifferentResourceType() throws Exception {
        assertLookupResultIsNotEqualTo(new LookupResult(this.beanSource, "junit/other/type"));
    }

    @Test
    public void testEqualsToResultForSameSourceAndResourceType() throws Exception {
        assertThat(this.testee).isEqualTo(new LookupResult(this.beanSource, "junit/test/type"));
    }

    @Test
    public void testStringRepresentation() throws Exception {
        assertThat(this.testee.toString()).isEqualTo("junit/test/type -> [beanSource]");
    }

    private void assertLookupResultIsNotEqualTo(Object other) {
        assertThat(this.testee).isNotEqualTo(other);
    }
}
