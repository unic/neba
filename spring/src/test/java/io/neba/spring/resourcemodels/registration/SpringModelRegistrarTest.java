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

package io.neba.spring.resourcemodels.registration;

import io.neba.api.annotations.ResourceModel;
import io.neba.api.spi.ResourceModelFactory;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Olaf Otto
 */
@RunWith(MockitoJUnitRunner.class)
public class SpringModelRegistrarTest {
    private ConfigurableListableBeanFactory factory;
    @Mock
    private Bundle bundle;
    @Mock
    private BundleContext context;
    private Set<String> beanNamesInApplicationContext = new HashSet<>();
    private ResourceModelFactory publishedService;
    private ServiceRegistration serviceRegistration;

    @InjectMocks
    private SpringModelRegistrar testee;

    @Before
    public void mockBundleContext() {
        when(this.bundle.getBundleId()).thenReturn(12345L);
        when(this.context.getBundle()).thenReturn(this.bundle);
        when(this.bundle.getBundleContext()).thenReturn(this.context);

        doAnswer(i ->  {
            publishedService = (ResourceModelFactory) i.getArguments()[1];
            serviceRegistration = mock(ServiceRegistration.class);
            return serviceRegistration;
        }).when(this.context)
          .registerService(
                  eq(ResourceModelFactory.class.getName()),
                  isA(ResourceModelFactory.class),
                  isA(Dictionary.class));
    }

    @Test
    public void testModelRegistration() throws Exception {
        withBeanFactory();
        withResourceModelsInApplicationContext("bean1", "bean2");
        registerResourceModels();

        assertAllModelsArePublishedViaModelFactory();
    }

    @Test
    public void testRegistrarUnregistersModelFactoryWhenBundleStops() throws Exception {
        withBeanFactory();
        withResourceModelsInApplicationContext("bean1", "bean2");
        registerResourceModels();

        sendStopEventToRegistrar();

        assertRegistrarUnregistersModelFactory();
    }

    @Test
    public void testRegistrarUnregistersAllFactoriesWhenRegistrarStops() throws Exception {
        withBeanFactory();
        withResourceModelsInApplicationContext("bean1", "bean2");
        registerResourceModels();

        shutdown();

        assertRegistrarUnregistersModelFactory();
    }

    private void shutdown() {
        this.testee.shutdown();
    }

    private void assertRegistrarUnregistersModelFactory() {
        verify(this.serviceRegistration).unregister();
    }

    private void sendStopEventToRegistrar() {
        this.testee.unregister(this.bundle);
    }

    private void assertAllModelsArePublishedViaModelFactory() {
        assertThat(this.publishedService).isNotNull();

        assertThat(this.publishedService.getModelDefinitions())
                .hasSameSizeAs(this.beanNamesInApplicationContext);

        assertThat(this.publishedService.getModelDefinitions().stream()
                .map(ResourceModelFactory.ModelDefinition::getName))
                .containsOnlyElementsOf(this.beanNamesInApplicationContext);
    }

    private void withResourceModelsInApplicationContext(String... beanNames) {
        when(this.factory.getBeanNamesForType(eq(Object.class))).thenReturn(beanNames);
        for (String name : beanNames) {
            mockResourceModelWithBeanName(name);
        }
    }

    private void mockResourceModelWithBeanName(String name) {
        ResourceModel type = mockResourceModelAnnotation(name);
        when(this.factory.findAnnotationOnBean(eq(name), eq(ResourceModel.class))).thenReturn(type);
        this.beanNamesInApplicationContext.add(name);
        when(this.factory.getBean(eq(name))).thenReturn(name);
    }

    private ResourceModel mockResourceModelAnnotation(String name) {
        ResourceModel type = mock(ResourceModel.class);
        when(type.types()).thenReturn(new String[] {"/junit/test/" + name});
        return type;
    }

    private void registerResourceModels() {
        this.testee.registerModels(this.context, this.factory);
    }

    private void withBeanFactory() {
        this.factory = mock(ConfigurableListableBeanFactory.class);
    }
}
