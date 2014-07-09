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

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import io.neba.core.resourcemodels.caching.ResourceModelCaches;
import io.neba.core.resourcemodels.registration.LookupResult;
import io.neba.core.resourcemodels.registration.ModelRegistry;
import org.apache.sling.api.resource.Resource;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import io.neba.core.resourcemodels.mapping.ResourceToModelMapper;
import io.neba.core.util.OsgiBeanSource;

/**
 * @author Olaf Otto
 */
@RunWith(MockitoJUnitRunner.class)
public class ResourceToModelAdapterTest {
	
	/**
	 * @author Olaf Otto
	 */
	private static class TestModel {}
	
	/**
	 * @author Olaf Otto
	 */
	private static class TestModelDerived extends TestModel {}

	@Mock
	private ModelRegistry registry;
	@Mock
	private ResourceToModelMapper mapper;
	@Mock
	private ResourceModelCaches caches;

	private Resource adaptable;
	private Class<?> targetType;
	private Object adapted;
	private Object[] models;
	private List<LookupResult> sources;

	@InjectMocks
	private ResourceToModelAdapter testee;

	@Test
	public void testAdaptationToExactModelType() throws Exception {
		withAdaptable(mock(Resource.class));
		withTargetType(TestModel.class);
		withAvailableModels(new TestModel());
		adapt();
		verifyAdapterObtainsSourceFromRegistrar();
		verifyAdapterMapsResoureToModel();
		assertAdaptableWasAdaptedToModel();
	}

	@Test
	public void testAdaptationToDerivedModelType() throws Exception {
		withAdaptable(mock(Resource.class));
		withTargetType(TestModel.class);
		withAvailableModels(new TestModelDerived());
		adapt();
		verifyAdapterObtainsSourceFromRegistrar();
		verifyAdapterMapsResoureToModel();
		assertAdaptableWasAdaptedToModel();
	}

	@Test
	public void testHandlingOfNullBeanSource() throws Exception {
		withAdaptable(mock(Resource.class));
		withTargetType(TestModel.class);
		withNullReturnedAsBeanSourceFromRegistrar();
		adapt();
		verifyAdapterObtainsSourceFromRegistrar();
		assertAdaptableWasNotAdaptedToModel();
		verifyAdapterDoesNotMapResourceToModel();
	}
	
	@Test(expected = AmbiguousModelAssociationException.class)
	public void testAdaptationToTypeWithMultipleMappings() throws Exception {
		withAdaptable(mock(Resource.class));
		withTargetType(TestModel.class);
		withAvailableModels(new TestModel(), new TestModelDerived());
		adapt();
	}

	@SuppressWarnings("unchecked")
    private void verifyAdapterDoesNotMapResourceToModel() {
		verify(this.mapper, never()).map(isA(Resource.class), isA(OsgiBeanSource.class));
	}

	private void withNullReturnedAsBeanSourceFromRegistrar() {
		when(this.registry.lookupMostSpecificModels(eq(this.adaptable))).thenReturn(null);
		when(this.registry.lookupMostSpecificModels(eq(this.adaptable), eq(this.targetType))).thenReturn(null);
	}
	
	private void assertAdaptableWasNotAdaptedToModel() {
		assertThat(this.adapted).isNull();
	}
	
	private void assertAdaptableWasAdaptedToModel() {
		assertAdaptableWasAdaptedToModel(0);
	}
	
	private void assertAdaptableWasAdaptedToModel(int i) {
		assertThat(this.adapted).isSameAs(this.models[i]);
	}

	private void verifyAdapterObtainsSourceFromRegistrar() {
		verify(this.registry, times(1)).lookupMostSpecificModels(eq(this.adaptable), eq(this.targetType));
	}

	private void verifyAdapterMapsResoureToModel() {
		verifyAdapterMapsResoureToModel(0);
	}
	
	private void verifyAdapterMapsResoureToModel(int i) {
        OsgiBeanSource<?> source = this.sources.get(i).getSource();
        verify(this.mapper).map(eq(this.adaptable), eq(source));
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void withAvailableModels(Object... models) {
		this.models = models;
		this.sources = new ArrayList<LookupResult>();
		when(this.registry.lookupMostSpecificModels(isA(Resource.class), isA(Class.class))).thenReturn(this.sources);
		
		for (Object model: models) {
			OsgiBeanSource source = mock(OsgiBeanSource.class);
            LookupResult result = mock(LookupResult.class);
            when(result.getSource()).thenReturn(source);
			this.sources.add(result);
			when(source.getBeanType()).thenReturn((Class) model.getClass());
			when(source.getBean()).thenReturn(model);
			when(this.mapper.map(eq(this.adaptable), eq(source))).thenReturn(model);
		}
	}

	private void adapt() {
		this.adapted = this.testee.getAdapter(this.adaptable, this.targetType);
	}

	private void withTargetType(Class<TestModel> targetType) {
		this.targetType = targetType;
	}

	private void withAdaptable(Resource mock) {
		this.adaptable = mock;
	}
}
