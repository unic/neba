/*
  Copyright 2013 the original author or authors.
  <p/>
  Licensed under the Apache License, Version 2.0 the "License";
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at
  <p/>
  http://www.apache.org/licenses/LICENSE-2.0
  <p/>
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
 */

package io.neba.core.resourcemodels.adaptation;

import io.neba.core.resourcemodels.caching.RequestScopedResourceModelCache;
import io.neba.core.resourcemodels.mapping.ResourceToModelMapper;
import io.neba.core.resourcemodels.registration.ModelRegistry;
import io.neba.core.util.Key;
import io.neba.core.util.ResolvedModelSource;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Olaf Otto
 */
@RunWith(MockitoJUnitRunner.class)
public class ResourceToModelAdapterTest {

    /**
     * @author Olaf Otto
     */
    private static class TestModel {
    }

    /**
     * @author Olaf Otto
     */
    private static class TestModelDerived extends TestModel {
    }

    @Mock
    private ModelRegistry registry;
    @Mock
    private ResourceToModelMapper mapper;
    @Mock
    private Resource resource;
    @Mock
    private ResourceResolver resourceResolver;
    @Mock
    private SlingHttpServletRequest request;

    @Mock
    private RequestScopedResourceModelCache cache;
    private Map<Key, Object> testCache = new HashMap<>();

    private Class<?> targetType;
    private Object adapted;
    private Object[] models;
    private List<ResolvedModelSource<?>> resolvedModelSources;

    @InjectMocks
    private ResourceToModelAdapter testee;

    @Before
    public void setUp() {
        Answer<?> storeInCache = invocation -> {
            Object model = invocation.getArguments()[2];
            testCache.put(buildCacheInvocationKey(invocation), model);
            return null;
        },

                lookupFromCache = invocation -> testCache.get(buildCacheInvocationKey(invocation));

        doAnswer(storeInCache)
                .when(this.cache)
                .put(isA(Resource.class), isA(Key.class), any());

        doAnswer(lookupFromCache)
                .when(this.cache)
                .get(isA(Resource.class), isA(Key.class));

        doReturn(this.resourceResolver).when(resource).getResourceResolver();
        doReturn("anonymous").when(resourceResolver).getUserID();
        doReturn(this.resource).when(this.request).getResource();
    }

    @Test
    public void testAdaptablesOtherThanResourceYieldNull() {
        assertThat(this.testee.getAdapter(new Object(), Object.class)).isNull();
    }

    @Test
    public void testAdaptationToExactModelType() {
        withTargetType(TestModel.class);
        withAvailableModels(new TestModel());
        adapt();
        verifyAdapterObtainsSourceFromRegistrar();
        verifyAdapterMapsResourceToModel();
        assertResourceWasAdaptedToModel();
    }

    @Test
    public void testAdaptationToModelViaRequest() {
        withTargetType(TestModel.class);
        withAvailableModels(new TestModel());
        adaptViaRequest();
        verifyAdapterObtainsSourceFromRegistrar();
        verifyAdapterMapsResourceToModel();
        assertResourceWasAdaptedToModel();
    }

    @Test
    public void testAdaptationToDerivedModelType() {
        withTargetType(TestModel.class);
        withAvailableModels(new TestModelDerived());
        adapt();
        verifyAdapterObtainsSourceFromRegistrar();
        verifyAdapterMapsResourceToModel();
        assertResourceWasAdaptedToModel();
    }

    @Test
    public void testHandlingOfNullModelSource() {
        withTargetType(TestModel.class);
        withNullReturnedAsModelSourceFromRegistrar();
        adapt();
        verifyAdapterObtainsSourceFromRegistrar();
        assertResourceWasNotAdaptedToModel();
        verifyAdapterDoesNotMapResourceToModel();
    }

    @Test(expected = AmbiguousModelAssociationException.class)
    public void testAdaptationToTypeWithMultipleMappings() {
        withTargetType(TestModel.class);
        withAvailableModels(new TestModel(), new TestModelDerived());
        adapt();
    }

    @Test
    public void testAdaptationOfSameResourceWithDifferentResourceTypeYieldsDifferentModel() {
        withResourceType("resource/type/one");
        withResourcePath("/resource/path");
        withTargetType(TestModel.class);
        withAvailableModels(new TestModel());

        adapt();
        assertResourceWasAdaptedToModel();

        withResourceType("resource/type/two");
        withAvailableModels(new TestModelDerived());

        adapt();
        assertResourceWasAdaptedToModel();
    }

    @Test
    public void testSubsequentAdaptationOfSameResourceWithSameResourceTypeIsServedFromCache() {
        withResourceType("resource/type/one");
        withResourcePath("/resource/path");
        withTargetType(TestModel.class);
        withAvailableModels(new TestModel());

        adapt();
        verifyAdapterMapsResourceToModel();
        assertResourceWasAdaptedToModel();

        withResourceType("resource/type/one");

        adapt();
        verifyAdapterMapsResourceToModel();
        assertResourceWasAdaptedToModel();
    }

    @Test
    public void testAdaptationsResultingInNullAreCached() {
        withTargetType(TestModel.class);
        adapt();

        verifyAdapterObtainsSourceFromRegistrar();
        assertResourceWasNotAdaptedToModel();

        adapt();
        // This asserts that the source was still obtained exactly once
        // during the entire test execution, i.e. lookup was not re-attempted during the
        // second adapt() invocation.
        verifyAdapterObtainsSourceFromRegistrar();
    }

    @SuppressWarnings("unchecked")
    private void verifyAdapterDoesNotMapResourceToModel() {
        verify(this.mapper, never()).map(isA(Resource.class), isA(ResolvedModelSource.class));
    }

    private void withNullReturnedAsModelSourceFromRegistrar() {
        when(this.registry.lookupMostSpecificModels(eq(this.resource), eq(this.targetType))).thenReturn(null);
    }

    private void assertResourceWasNotAdaptedToModel() {
        assertThat(this.adapted).isNull();
    }

    private void assertResourceWasAdaptedToModel() {
        assertResourceWasAdaptedToModel(0);
    }

    private void assertResourceWasAdaptedToModel(int i) {
        assertThat(this.adapted).isSameAs(this.models[i]);
    }

    private void verifyAdapterObtainsSourceFromRegistrar() {
        verify(this.registry, times(1)).lookupMostSpecificModels(eq(this.resource), eq(this.targetType));
    }

    private void verifyAdapterMapsResourceToModel() {
        verifyAdapterMapsResourceToModel(0);
    }

    private void verifyAdapterMapsResourceToModel(int i) {
        verify(this.mapper).map(eq(this.resource), eq(this.resolvedModelSources.get(i)));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void withAvailableModels(Object... models) {
        this.models = models;
        this.resolvedModelSources = new ArrayList<>();
        when(this.registry.lookupMostSpecificModels(isA(Resource.class), isA(Class.class))).thenReturn(this.resolvedModelSources);

        for (Object model : models) {
            ResolvedModelSource resolvedModelSource = mock(ResolvedModelSource.class);
            this.resolvedModelSources.add(resolvedModelSource);
            when(this.mapper.map(eq(this.resource), eq(resolvedModelSource))).thenReturn(model);
        }
    }

    private void adapt() {
        this.adapted = this.testee.getAdapter(this.resource, this.targetType);
    }

    private void adaptViaRequest() {
        this.adapted = this.testee.getAdapter(this.request, this.targetType);
    }

    private void withTargetType(Class<TestModel> targetType) {
        this.targetType = targetType;
    }

    private void withResourcePath(String path) {
        doReturn(path).when(resource).getPath();
    }

    private void withResourceType(String type) {
        doReturn(type).when(this.resource).getResourceType();
    }

    /**
     * Simulates the key calculation used by the ResourceModelCaches implementation.
     */
    private Key buildCacheInvocationKey(InvocationOnMock invocation) {
        Resource resource = (Resource) invocation.getArguments()[0];
        Key key = (Key) invocation.getArguments()[1];
        return new Key(resource.getPath(), key, resource.getResourceType(), resource.getResourceResolver().getUserID());
    }
}
