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

package io.neba.core.resourcemodels.adaptation;

import io.neba.core.resourcemodels.registration.ModelRegistry;
import io.neba.core.util.OsgiBeanSource;
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
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.*;

/**
 * @author Olaf Otto
 */
@RunWith(MockitoJUnitRunner.class)
public class ResourceToModelAdapterUpdaterTest {
	//CHECKSTYLE:OFF
	private interface TestInterface {}
	private interface TestInterfaceExtended {}
	private static class TestModel implements TestInterface {}
	private static class TestModelDerived extends TestModel implements TestInterfaceExtended {}
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

	private List<OsgiBeanSource<?>> beanSources;
	private Dictionary<String, Object> updatedProperties;

	@InjectMocks
	private ResourceToModelAdapterUpdater testee;

	@Before
	@SuppressWarnings("unchecked")
	public void prepareTest() {
		this.beanSources = new LinkedList<>();
		
		when(this.registry.getBeanSources()).thenReturn(this.beanSources);

		when(this.context.registerService(eq(AdapterFactory.class.getName()), eq(this.adapter), isA(Dictionary.class)))
		.thenAnswer(invocation -> {
            updatedProperties = (Dictionary<String, Object>) invocation.getArguments()[2];
            return registration;
        });
		when(this.context.getBundle()).thenReturn(this.bundle);
		
		this.testee.registerModelAdapter();
	}

	@Test
    public void testUnregistrationOfAlreadyUnregisteredService() throws Exception {
	    signalIllegalStateWhenUnregisteringService();
	    withStartingBundle();
	    signalRegistryChange();
	    assertUpdaterUpdatesModelAdapter();
    }

	@Test
	public void testUpdaterPerformsNoUpdatesWhileBundleNotReady() throws Exception {
		signalRegistryChange();
		assertUpdaterDoesNotUpdateModelAdapter();
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
		assertAdaptersPropertyIs(TestModel.class.getName(), TestInterface.class.getName());
	}

	@Test
	public void testHierarchyResolutionOfModelWithInheritance() throws Exception {
		assertAdapterDoesNotHaveAnyAdapters();
		withModel(TestModelDerived.class);
		withActiveBundle();
		signalRegistryChange();
		assertAdaptersPropertyIs(TestModelDerived.class.getName(), TestModel.class.getName(), 
			TestInterface.class.getName(), TestInterfaceExtended.class.getName());
	}

	private void withActiveBundle() {
		when(this.bundle.getState()).thenReturn(Bundle.ACTIVE);
	}

	private void withStartingBundle() {
		when(this.bundle.getState()).thenReturn(Bundle.STARTING);
	}

	private void assertAdapterDoesNotHaveAnyAdapters() {
		assertAdaptersPropertyIs();
	}

	private void assertUpdaterUpdatesModelAdapter() {
		verify(this.registration).unregister();
		verify(this.context).registerService(anyString(), eq(this.adapter), eq(this.updatedProperties));
	}

	private void assertUpdaterDoesNotUpdateModelAdapter() {
		verify(this.registration, never()).unregister();
		verify(this.context, never()).registerService(anyString(), anyObject(), isA(Properties.class));
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

	@SuppressWarnings({ "rawtypes" })
	private void withModel(Class<?> modelType) {
		OsgiBeanSource source = mock(OsgiBeanSource.class);
		when(source.getBeanType()).thenReturn((Class) modelType);
		this.beanSources.add(source);
	}
	
    private void signalIllegalStateWhenUnregisteringService() {
        doThrow(new IllegalStateException("THIS IS AN EXPECTED TEST EXCEPTION")).when(this.registration).unregister();
    }
}
