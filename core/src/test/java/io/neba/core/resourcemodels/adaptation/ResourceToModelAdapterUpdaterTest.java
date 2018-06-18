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

package io.neba.core.resourcemodels.adaptation;

import io.neba.core.Eventual;
import io.neba.core.resourcemodels.registration.ModelRegistry;
import io.neba.core.util.OsgiModelSource;
import org.apache.sling.api.adapter.AdapterFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

import java.util.Dictionary;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Olaf Otto
 */
@RunWith(MockitoJUnitRunner.class)
public class ResourceToModelAdapterUpdaterTest implements Eventual {
    //CHECKSTYLE:OFF
    private interface TestInterface {
    }

    private interface TestInterfaceExtended {
    }

    private static class TestModel implements TestInterface {
    }

    private static class TestModelDerived extends TestModel implements TestInterfaceExtended {
    }
    //CHECKSTYLE:ON

    @Mock
    private ModelRegistry registry;
    @Mock
    private ResourceToModelAdapter adapter;
    @Mock
    private ServiceRegistration registration;
    @Mock
    private BundleContext context;
    @Mock
    private Bundle bundle;

    private List<OsgiModelSource<?>> modelSources;
    private Dictionary<String, Object> updatedProperties;

    @InjectMocks
    private ResourceToModelAdapterUpdater testee;

    @Before
    @SuppressWarnings("unchecked")
    public void prepareTest() {
        this.modelSources = new LinkedList<>();

        when(this.registry.getModelSources())
                .thenReturn(this.modelSources);

        when(this.context.registerService(eq(AdapterFactory.class.getName()), eq(this.adapter), isA(Dictionary.class)))
                .thenAnswer(inv -> {
                    updatedProperties = (Dictionary<String, Object>) inv.getArguments()[2];
                    return registration;
                });

        when(this.context.getBundle())
                .thenReturn(this.bundle);

        this.testee.activate(this.context);
    }

    @Test
    public void testUnregistrationOfAlreadyUnregisteredService() throws Exception {
        signalIllegalStateWhenUnregisteringService();
        withStartingBundle();
        signalRegistryChange();
        assertUpdaterUpdatesModelAdapter();
    }

    @Test
    public void testUpdaterPerformsNoUpdatesWhileBundleNotReady() {
        withSynchronousExecutor();
        withResolvedBundle();
        signalRegistryChange();
        assertUpdaterDoesNotUpdateModelAdapter();
    }

    private void withResolvedBundle() {
        doReturn(Bundle.RESOLVED).when(this.bundle).getState();
    }

    @Test
    public void testUpdatePerformsUpdateWhenBundleStarting() throws Exception {
        withStartingBundle();
        signalRegistryChange();
        assertUpdaterUpdatesModelAdapter();
    }

    @Test
    public void testUpdaterPerformsUpdateWhenBundleActive() throws Exception {
        withActiveBundle();
        signalRegistryChange();
        assertUpdaterUpdatesModelAdapter();
    }

    @Test
    public void testHierarchyResolutionOfModelWithoutInheritance() throws Exception {
        assertAdapterDoesNotHaveAnyAdapters();
        withModel(TestModel.class);
        withActiveBundle();
        signalRegistryChange();
        assertUpdaterUpdatesModelAdapter();
        assertAdaptersPropertyIs(TestModel.class.getName(), TestInterface.class.getName());
    }

    @Test
    public void testHierarchyResolutionOfModelWithInheritance() throws Exception {
        assertAdapterDoesNotHaveAnyAdapters();
        withModel(TestModelDerived.class);
        withActiveBundle();
        signalRegistryChange();
        assertUpdaterUpdatesModelAdapter();
        assertAdaptersPropertyIs(TestModelDerived.class.getName(), TestModel.class.getName(),
                TestInterface.class.getName(), TestInterfaceExtended.class.getName());
    }

    private void withActiveBundle() {
        when(this.bundle.getState()).thenReturn(Bundle.ACTIVE);
    }

    private void withStartingBundle() {
        when(this.bundle.getState()).thenReturn(Bundle.STARTING);
    }

    private void withSynchronousExecutor() {
        ExecutorService executorService = mock(ExecutorService.class);
        doAnswer(i -> {
            ((Runnable) i.getArguments()[0]).run();
            return null;
        }).when(executorService).execute(isA(Runnable.class));
        this.testee.setExecutorService(executorService);
    }

    private void assertAdapterDoesNotHaveAnyAdapters() {
        assertAdaptersPropertyIs();
    }

    private void assertUpdaterUpdatesModelAdapter() throws InterruptedException {
        eventually(() -> {
            verify(this.registration).unregister();
            verify(this.context).registerService(anyString(), eq(this.adapter), eq(this.updatedProperties));
        });
    }

    private void assertUpdaterDoesNotUpdateModelAdapter() {
        verify(this.registration, never()).unregister();
    }

    private void assertAdaptersPropertyIs(Object... elements) {
        assertThat(this.updatedProperties).isNotNull();
        assertThat(this.updatedProperties.get("adapters"))
                .isNotNull()
                .isInstanceOf(Object[].class);
        Object[] adapters = (Object[]) this.updatedProperties.get("adapters");
        assertThat(adapters).containsOnly(elements);
    }

    private void signalRegistryChange() {
        this.testee.refresh();
    }

    @SuppressWarnings({"rawtypes"})
    private void withModel(Class<?> modelType) {
        OsgiModelSource source = mock(OsgiModelSource.class);
        when(source.getModelType()).thenReturn(modelType);
        this.modelSources.add(source);
    }

    private void signalIllegalStateWhenUnregisteringService() {
        doThrow(new IllegalStateException("THIS IS AN EXPECTED TEST EXCEPTION")).when(this.registration).unregister();
    }
}
