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

import io.neba.api.resourcemodels.ResourceModelFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.Bundle;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static org.osgi.framework.Bundle.ACTIVE;
import static org.osgi.framework.Bundle.UNINSTALLED;

/**
 * @author Olaf Otto
 */
@RunWith(MockitoJUnitRunner.class)
public class OsgiModelSourceSourceTest {
    @Mock
    private ResourceModelFactory factory;
    @Mock
    private Bundle bundleOne;
    @Mock
    private Bundle bundleTwo;

    private String modelName = "testBean";
    private String modelSourceAsString;

    private OsgiModelSourceSource<Object> testee;

    @Before
    public void prepareBeanSource() {
        doReturn(ACTIVE)
                .when(this.bundleOne)
                .getState();

        doReturn(ACTIVE)
                .when(this.bundleTwo)
                .getState();

        doReturn(123L)
                .when(this.bundleOne)
                .getBundleId();

        doReturn(1234L)
                .when(this.bundleTwo)
                .getBundleId();

        this.testee = new OsgiModelSourceSource<>(this.modelName, this.factory, this.bundleOne);
    }
    
    @Test
    public void testToStringRepresentation() throws Exception {
        modelSourceToString();
        assertModelSourceAsStringIs("Bean \"testBean\" from bundle with id 123");
    }
    
    @Test
    public void testBeanRetrievalFromFactory() throws Exception {
        getModel();
        verifyModelSourceGetsModelFromModelFactory();
    }
    
    @Test
    public void testBeanTypeRetrievalFromFactory() throws Exception {
        getModelType();
        verifyBeanSourceGetsBeanTypeFromFactory();
    }

    @Test
    public void testIsValidIsTrueWhenBundleIsActive() throws Exception {
        assertSourceIsValid();
    }

    @Test
    public void testIsValidIsTrueWhenBundleIsNotActive() throws Exception {
        withUninstalledBundles();
        assertSourceIsInvalid();
    }

    @Test
    public void testHashCodeAndEquals() throws Exception {
        OsgiModelSourceSource<?> one = new OsgiModelSourceSource<>("one", mock(ResourceModelFactory.class), this.bundleOne);
        OsgiModelSourceSource<?> two = new OsgiModelSourceSource<>("one", mock(ResourceModelFactory.class), this.bundleOne);

        assertThat(one.hashCode()).isEqualTo(two.hashCode());
        assertThat(one).isEqualTo(two);
        assertThat(two).isEqualTo(one);

        one = new OsgiModelSourceSource<>("one", mock(ResourceModelFactory.class), this.bundleOne);
        two = new OsgiModelSourceSource<>("two", mock(ResourceModelFactory.class), this.bundleOne);

        assertThat(one.hashCode()).isNotEqualTo(two.hashCode());
        assertThat(one).isNotEqualTo(two);
        assertThat(two).isNotEqualTo(one);

        one = new OsgiModelSourceSource<>("one", mock(ResourceModelFactory.class), this.bundleOne);
        two = new OsgiModelSourceSource<>("one", mock(ResourceModelFactory.class), this.bundleTwo);

        assertThat(one.hashCode()).isNotEqualTo(two.hashCode());
        assertThat(one).isNotEqualTo(two);
        assertThat(two).isNotEqualTo(one);
    }

    private void assertSourceIsInvalid() {
        assertThat(this.testee.isValid()).isFalse();
    }

    private void withUninstalledBundles() {
        doReturn(UNINSTALLED).when(this.bundleOne).getState();
        doReturn(UNINSTALLED).when(this.bundleTwo).getState();
    }

    private void assertSourceIsValid() {
        assertThat(this.testee.isValid()).isTrue();
    }

    private void verifyBeanSourceGetsBeanTypeFromFactory() {
        verify(this.factory).getType(eq(this.modelName));
    }

    private void getModelType() {
        this.testee.getModelType();
    }

    private void verifyModelSourceGetsModelFromModelFactory() {
        verify(this.factory).getModel(eq(this.modelName));
    }

    private void getModel() {
        this.testee.getModel();
    }

    private void assertModelSourceAsStringIs(String string) {
        assertThat(this.modelSourceAsString).isEqualTo(string);
    }

    private void modelSourceToString() {
        this.modelSourceAsString = this.testee.toString();
    }
}
