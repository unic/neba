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

import io.neba.api.spi.ResourceModelFactory;
import io.neba.api.spi.ResourceModelFactory.ModelDefinition;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.Bundle;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.osgi.framework.Bundle.ACTIVE;

/**
 * @author Olaf Otto
 */
@RunWith(MockitoJUnitRunner.class)
public class OsgiModelSourceTest {
    @Mock
    private ResourceModelFactory factory;
    @Mock
    private Bundle bundleOne;
    @Mock
    private Bundle bundleTwo;
    @Mock
    private ModelDefinition modelDefinition;

    private String modelSourceAsString;

    private OsgiModelSource<Object> testee;

    @Before
    public void prepareModelSource() {
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

        doReturn("testModel").when(this.modelDefinition).getName();

        this.testee = new OsgiModelSource<>(this.modelDefinition, this.factory, this.bundleOne);
    }

    @Test
    public void testToStringRepresentation() throws Exception {
        modelSourceToString();
        assertModelSourceAsStringIs("Model \"testModel\" from bundle with id 123");
    }

    @Test
    public void testModelRetrievalFromFactory() throws Exception {
        getModel();
        verifyModelSourceGetsModelFromModelFactory();
    }

    @Test
    public void testModelTypeRetrievalFromFactory() throws Exception {
        getModelType();
        verifyModelSourceModelTypeFromModelDefinitision();
    }

    @Test
    public void testHashCodeAndEquals() throws Exception {
        OsgiModelSource<?> one = new OsgiModelSource<>(modelDefinition("one"), mock(ResourceModelFactory.class), this.bundleOne);
        OsgiModelSource<?> two = new OsgiModelSource<>(modelDefinition("one"), mock(ResourceModelFactory.class), this.bundleOne);

        assertThat(one.hashCode()).isEqualTo(two.hashCode());
        assertThat(one).isEqualTo(two);
        assertThat(two).isEqualTo(one);

        one = new OsgiModelSource<>(modelDefinition("one"), mock(ResourceModelFactory.class), this.bundleOne);
        two = new OsgiModelSource<>(modelDefinition("two"), mock(ResourceModelFactory.class), this.bundleOne);

        assertThat(one.hashCode()).isNotEqualTo(two.hashCode());
        assertThat(one).isNotEqualTo(two);
        assertThat(two).isNotEqualTo(one);

        one = new OsgiModelSource<>(modelDefinition("one"), mock(ResourceModelFactory.class), this.bundleOne);
        two = new OsgiModelSource<>(modelDefinition("one"), mock(ResourceModelFactory.class), this.bundleTwo);

        assertThat(one.hashCode()).isNotEqualTo(two.hashCode());
        assertThat(one).isNotEqualTo(two);
        assertThat(two).isNotEqualTo(one);
    }

    private ModelDefinition modelDefinition(String modelName) {
        ModelDefinition definition = mock(ModelDefinition.class);
        doReturn(modelName).when(definition).getName();
        return definition;
    }


    private void verifyModelSourceModelTypeFromModelDefinitision() {
        verify(this.modelDefinition).getType();
    }

    private void getModelType() {
        this.testee.getModelType();
    }

    private void verifyModelSourceGetsModelFromModelFactory() {
        verify(this.factory).getModel(this.modelDefinition);
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
