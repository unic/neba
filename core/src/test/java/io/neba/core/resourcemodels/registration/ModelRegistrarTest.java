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

import io.neba.api.annotations.ResourceModel;
import io.neba.core.resourcemodels.adaptation.ResourceToModelAdapterUpdater;
import io.neba.core.resourcemodels.metadata.ResourceModelMetaDataRegistrar;
import io.neba.core.util.OsgiBeanSource;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.*;

/**
 * @author Olaf Otto
 */
@RunWith(MockitoJUnitRunner.class)
public class ModelRegistrarTest {
    private ConfigurableListableBeanFactory factory;
    private Bundle bundle;
    private BundleContext context;
    private Map<String, OsgiBeanSource<Object>> addedBeanSources;
    private Set<Object> beans = new HashSet<>();

    @Mock
    private ModelRegistry registry;
    @Mock
    private ResourceToModelAdapterUpdater updater;
    @Mock
    private ResourceModelMetaDataRegistrar resourceModelMetaDataRegistrar;
    @InjectMocks
    private ModelRegistrar testee;

    @Before
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void prepareModelRegistrar() {
        this.addedBeanSources = new HashMap<>();
        Answer registerModel = invocation -> {
            String[] resourceTypes = (String[]) invocation.getArguments()[0];
            OsgiBeanSource<Object> osgiBeanSource = (OsgiBeanSource<Object>) invocation.getArguments()[1];
            for (String type : resourceTypes) {
                addedBeanSources.put(type, osgiBeanSource);
            }
            return null;
        };
        doAnswer(registerModel).when(this.registry).add(isA(String[].class), isA(OsgiBeanSource.class));
    }

    @Before
    public void mockBundleContext() {
        this.bundle = mock(Bundle.class);
        when(this.bundle.getBundleId()).thenReturn(12345L);
        this.context = mock(BundleContext.class);
        when(this.context.getBundle()).thenReturn(this.bundle);
    }

    @Test
    public void testModelRegistration() throws Exception {
        withBeanFactory();
        withResourceModelsInApplicationContext("bean1", "bean2");
        registerResourceModels();
        assertBeanSourcesForAllBeansAddedToRegistry();
    }

    @Test
    public void testRegistrarRemovesBundleFromRegistryWhenBundleStops() throws Exception {
        sendStopEventToRegistrar();
        assertRegistrarRemovesBundleFromRegistry();
    }

    private void assertRegistrarRemovesBundleFromRegistry() {
        verify(this.registry).removeResourceModels(eq(this.bundle));
    }

    private void sendStopEventToRegistrar() {
        this.testee.unregister(this.bundle);
    }

    private void assertBeanSourcesForAllBeansAddedToRegistry() {
        assertThat(this.addedBeanSources, notNullValue());
        assertThat(this.addedBeanSources.size(), is(this.beans.size()));
        Set<Object> beansFromBeanSources = this.addedBeanSources.values().stream().map(OsgiBeanSource::getBean).collect(Collectors.toSet());
        assertThat(this.beans, is(beansFromBeanSources));
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
        this.beans.add(name);
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
