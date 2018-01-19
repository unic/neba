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
package io.neba.core.resourcemodels.factory;

import io.neba.api.annotations.ResourceModel;
import io.neba.api.spi.ResourceModelFactory.ModelDefinition;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import java.net.URL;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Vector;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@RunWith(MockitoJUnitRunner.class)
public class ModelFactoryTest {
    @Mock
    private Bundle bundle;
    @Mock
    private BundleContext bundleContext;

    private ModelFactory testee;

    @Before
    public void setUp() throws Exception {
        Dictionary<String, String> headers = new Hashtable<>();
        headers.put("Neba-Packages", "first.package, second.package");
        doReturn(headers)
                .when(this.bundle)
                .getHeaders();

        doReturn(this.bundleContext).when(this.bundle).getBundleContext();

        // The actual protocol for OSGi bundles is "bundleresource:", but this protocol is not registered for unit tests.
        URL modelClassResource = new URL("file://bundleId.bundleVersion" + "/" + ModelClass.class.getName().replace('.', '/') + ".class");
        URL nonModelClassResource = new URL("file://bundleId.bundleVersion" + "/" + NonModelClass.class.getName().replace('.', '/') + ".class");

        Vector<URL> vector = new Vector<>();
        vector.add(modelClassResource);
        vector.add(nonModelClassResource);

        doReturn(vector.elements()).when(this.bundle).findEntries("/first/package", "*.class", true);
        doReturn(ModelClass.class).when(this.bundle).loadClass(ModelClass.class.getName());
        doReturn(NonModelClass.class).when(this.bundle).loadClass(NonModelClass.class.getName());

        this.testee = new ModelFactory(this.bundle);
    }

    @Test
    public void testModelFactoryFindsResourceModel() {
        assertThat(this.testee.getModelDefinitions())
                .hasSize(1);
        assertThat(this.testee.getModelDefinitions().iterator().next().getType())
                .isSameAs(ModelClass.class);
        assertThat(this.testee.getModelDefinitions().iterator().next().getName())
                .isEqualTo("modelClass");
        assertThat(this.testee.getModelDefinitions().iterator().next().getResourceModel())
                .isSameAs(ModelClass.class.getAnnotation(ResourceModel.class));
    }

    @Test
    public void testModelFactoryCanCreateInstanceForModelDefinition() {
        assertThat(this.testee.getModel(this.testee.getModelDefinitions().iterator().next()))
                .isInstanceOf(ModelClass.class);
    }

    @Test(expected = IllegalStateException.class)
    public void testHandlingOfMissingModelForModelDefinition() {
        this.testee.getModel(mock(ModelDefinition.class));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testModelDefinitionsAreUnmodifiable() {
        this.testee.getModelDefinitions().add(mock(ModelDefinition.class));
    }

    @ResourceModel(types = "some/type")
    public static class ModelClass {
    }

    public static class NonModelClass {
    }
}