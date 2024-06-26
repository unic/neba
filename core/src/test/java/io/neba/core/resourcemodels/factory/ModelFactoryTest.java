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
import io.neba.api.spi.ResourceModelFactory.ContentToModelMappingCallback;
import io.neba.api.spi.ResourceModelFactory.ModelDefinition;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.springframework.stereotype.Component;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * @author Olaf Otto
 */
@RunWith(MockitoJUnitRunner.class)
public class ModelFactoryTest {
    @Mock
    private Bundle bundle;
    @Mock
    private BundleContext bundleContext;
    @Mock
    private ContentToModelMappingCallback<ModelClass> callback;

    private ModelFactory testee;

    @Before
    public void setUp() throws Exception {
        Dictionary<String, String> headers = new Hashtable<>();
        headers.put("Neba-Packages", "first.package, second.package");
        doReturn(headers)
                .when(this.bundle)
                .getHeaders();

        doReturn(this.bundleContext).when(this.bundle).getBundleContext();

        List<Class<?>> modelTypes = asList(ModelClass.class, ModelClassWithMetaAnnotation.class, NonModelClass.class, SpringModelClass.class, SpringModelClassWithMetaAnnotation.class);

        Vector<URL> vector = modelTypes.stream()
                // The actual protocol for OSGi bundles is "bundleresource:", but this protocol is not registered for unit tests.
                .map(cls -> "file://bundleId.bundleVersion" + "/" + cls.getName().replace('.', '/') + ".class")
                .map(ModelFactoryTest::toUrl).collect(java.util.stream.Collectors.toCollection(Vector::new));

        doReturn(vector.elements()).when(this.bundle).findEntries("/first/package", "*.class", true);
        modelTypes.forEach(cls -> {
            try {
                doReturn(cls).when(this.bundle).loadClass(cls.getName());
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        });

        doAnswer(inv -> inv.getArguments()[0]).when(callback).map(any());

        this.testee = new ModelFactory(this.bundle);
    }

    @Test
    @SuppressWarnings("rawtypes")
    public void testModelFactoryFindsResourceModel() {
        assertThat(this.testee.getModelDefinitions())
                .hasSize(2);

        assertThat(this.testee.getModelDefinitions())
                .extracting(def -> (Class) def.getType())
                .containsExactly(ModelClass.class, ModelClassWithMetaAnnotation.class);

        assertThat(this.testee.getModelDefinitions())
                .extracting(ModelDefinition::getName)
                .containsExactly("modelClass", "modelClassWithMetaAnnotation");

        assertThat(this.testee.getModelDefinitions()).extracting(ModelDefinition::getResourceModel)
                .containsExactly(
                        ModelClass.class.getAnnotation(ResourceModel.class),
                        ModelClassWithMetaAnnotation.class.getAnnotation(CustomModelStereotype.class).annotationType().getAnnotation(ResourceModel.class)
                );
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testModelFactoryCanCreateInstanceForModelDefinition() {
        ModelDefinition<ModelClass> next = (ModelDefinition<ModelClass>) this.testee.getModelDefinitions().iterator().next();
        Object instance = this.testee.provideModel(next, this.callback);
        assertThat(instance).isInstanceOf(ModelClass.class);
    }

    @Test(expected = IllegalStateException.class)
    @SuppressWarnings("unchecked")
    public void testHandlingOfMissingModelForModelDefinition() {
        this.testee.provideModel(mock(ModelDefinition.class), this.callback);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testModelDefinitionsAreUnmodifiable() {
        this.testee.getModelDefinitions().add(mock(ModelDefinition.class));
    }

    @Test
    public void testSpringModelsAreExcluded() {
        assertDetectedModelsDoesNotInclude(SpringModelClass.class);
    }

    private void assertDetectedModelsDoesNotInclude(Class<?> modelType) {
        List<Class<?>> detectedTypes =
                this.testee.getModelDefinitions()
                        .stream()
                        .map(ModelDefinition::getType)
                        .collect(toList());

        assertThat(detectedTypes).doesNotContain(modelType);
    }

    @ResourceModel("some/type")
    public static class ModelClass {
    }

    @CustomModelStereotype
    public static class ModelClassWithMetaAnnotation {
    }

    public static class NonModelClass {
    }

    @Component
    @ResourceModel("some/type")
    public static class SpringModelClass {
    }

    @CustomSpringModelStereotype
    @ResourceModel("some/type")
    public static class SpringModelClassWithMetaAnnotation {
    }

    @ResourceModel("some/type")
    @Retention(RetentionPolicy.RUNTIME)
    public @interface CustomModelStereotype {

    }

    @Component
    @Retention(RetentionPolicy.RUNTIME)
    public @interface CustomSpringModelStereotype {
    }

    private static URL toUrl(String s) {
        try {
            return new URL(s);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
}
