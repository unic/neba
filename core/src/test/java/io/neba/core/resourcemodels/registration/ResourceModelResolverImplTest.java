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

package io.neba.core.resourcemodels.registration;

import io.neba.api.spi.ResourceModelFactory.ContentToModelMappingCallback;
import io.neba.core.resourcemodels.caching.RequestScopedResourceModelCache;
import io.neba.core.resourcemodels.mapping.ResourceToModelMapper;
import io.neba.core.util.Key;
import io.neba.core.util.OsgiModelSource;
import io.neba.core.util.ResolvedModelSource;
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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;

import static io.neba.api.Constants.SYNTHETIC_RESOURCETYPE_ROOT;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Olaf Otto
 */
@RunWith(MockitoJUnitRunner.class)
public class ResourceModelResolverImplTest {
    @Mock
    private ModelRegistry registry;
    @Mock
    private Resource resource;
    @Mock
    private ResourceResolver resourceResolver;
    @Mock
    private ResourceToModelMapper mapper;
    @Mock
    private ResolvedModelSource<Object> resolvedModelSource;
    @Mock
    private RequestScopedResourceModelCache cache;
    @Mock
    private OsgiModelSource<Object> osgiModelSource;
    @Mock
    private ContentToModelMappingCallback<Object> callback;

    private Map<Key, Optional<Object>> testCache = new HashMap<>();
    private Object resolutionResult;
    private final Object model = new Object();

    @InjectMocks
    private ResourceModelResolverImpl testee;

    @Before
    @SuppressWarnings("unchecked")
    public void prepareContainerAdapter() {
        Answer<?> storeInCache = invocation -> {
            Object model = invocation.getArguments()[2];
            testCache.put(buildCacheInvocationKey(invocation), Optional.ofNullable(model));
            return null;
        },

                lookupFromCache = invocation -> testCache.get(buildCacheInvocationKey(invocation));

        doAnswer(storeInCache)
                .when(this.cache)
                .put(isA(Resource.class), isA(Key.class), any());

        doAnswer(lookupFromCache)
                .when(this.cache)
                .get(isA(Resource.class), isA(Key.class));

        doReturn(this.resourceResolver)
                .when(this.resource)
                .getResourceResolver();

        when(this.mapper.map(isA(Resource.class), isA(ResolvedModelSource.class)))
                .thenAnswer(inv -> {
                    ResolvedModelSource<Object> resolvedModelSource = (ResolvedModelSource<Object>) inv.getArguments()[1];
                    return resolvedModelSource.getSource().getModel(this.callback);
                });
    }

    @Before
    @SuppressWarnings("unchecked")
    public void provideMockResourceModel() {
        LinkedList<ResolvedModelSource<?>> resolvedModelSources = new LinkedList<>();
        resolvedModelSources.add(this.resolvedModelSource);

        doReturn(this.osgiModelSource)
                .when(this.resolvedModelSource)
                .getSource();

        when(this.osgiModelSource.getModel(isA(ContentToModelMappingCallback.class)))
                .thenReturn(this.model);

        when(this.registry.lookupMostSpecificModels(eq(this.resource)))
                .thenReturn(resolvedModelSources);

        when(this.registry.lookupMostSpecificModels(eq(this.resource), anyString()))
                .thenReturn(resolvedModelSources);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testResolveMostSpecificModelWithModelNameRequiresResource() {
        this.testee.resolveMostSpecificModelWithName(null, "modelName");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testResolveMostSpecificModelWithModelNameRequiresModelName() {
        this.testee.resolveMostSpecificModelWithName(mock(Resource.class), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testResolveMostSpecificModelRequiresResource() {
        this.testee.resolveMostSpecificModel(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testResolveMostSpecificModelsIncludingBaseTypesRequiresResource() {
        this.testee.resolveMostSpecificModelIncludingModelsForBaseTypes(null);
    }

    @Test
    public void testResolutionOfSpecificModelIncludingBaseTypes() {
        provideMostSpecificModelIncludingBaseTypes();
        verifyResourceIsMappedToModel();
        assertResolvedModelIsReturned();
    }

    @Test
    public void testResolutionOfSpecificModelWithModelForNtUnstructured() {
        withModelFoundForResourceType("nt:unstructured");
        resolveMostSpecificModel();
        assertResolvedModelIsNull();
    }

    @Test
    public void testResolutionOfSpecificModelWithModelForNtBase() {
        withModelFoundForResourceType("nt:base");
        resolveMostSpecificModel();
        assertResolvedModelIsNull();
    }

    @Test
    public void testResolutionOfSpecificModelWithModelForSyntheticResourceRoot() {
        withModelFoundForResourceType(SYNTHETIC_RESOURCETYPE_ROOT);
        resolveMostSpecificModel();
        assertResolvedModelIsNull();
    }

    @Test
    public void testResolutionOfAnyModelWithModelForNtUnstructured() {
        withModelFoundForResourceType("nt:unstructured");
        provideMostSpecificModelIncludingBaseTypes();
        verifyResourceIsMappedToModel();
        assertResolvedModelIsReturned();
    }

    @Test
    public void testResolutionOfAnyModelWithModelForNtBase() {
        withModelFoundForResourceType("nt:base");
        provideMostSpecificModelIncludingBaseTypes();
        verifyResourceIsMappedToModel();
        assertResolvedModelIsReturned();
    }

    @Test
    public void testResolutionOfAnyModelWithModelForSyntheticResourceRoot() {
        withModelFoundForResourceType(SYNTHETIC_RESOURCETYPE_ROOT);
        provideMostSpecificModelIncludingBaseTypes();
        verifyResourceIsMappedToModel();
        assertResolvedModelIsReturned();
    }

    @Test
    public void testResolutionOfAnyModelWithSpecificModelName() {
        provideMostSpecificModelWithModelName("unitTestModel");
        verifyRegistryWasQueriedOnceWithModelName();
        verifyResourceIsMappedToModel();
        assertResolvedModelIsReturned();
    }

    @Test
    public void testResolutionOfAnyModelWithSpecificModelNameDisregardsBasicTypes() {
        withModelFoundForResourceType("nt:base");
        provideMostSpecificModelWithModelName("unitTestModel");
        verifyRegistryWasQueriedOnceWithModelName();
        verifyResourceIsMappedToModel();
        assertResolvedModelIsReturned();
    }

    @Test
    public void testSubsequentResolutionOfSameResourceWithDifferentResourceTypeIsNotServedFromCache() {
        withResourcePath("/resource/path");
        withResourceType("resource/type/one");

        resolveMostSpecificModel();
        verifyResourceIsMappedToModel();

        withResourceType("resource/type/two");
        resolveMostSpecificModel();
        verifyResourceIsMappedToModelAgain();
    }

    @Test
    public void testSubsequentResolutionOfSameResourceWithSameResourceTypeIsServedFromCache() {
        withResourcePath("/resource/path");

        withResourceType("resource/type/one");

        resolveMostSpecificModel();
        verifyResourceIsMappedToModel();

        withResourceType("resource/type/one");
        resolveMostSpecificModel();
        verifyResourceIsMappedToModel();
    }

    @Test
    public void testNoModelIsResolvedIfNoModelIsAvailable() {
        withoutAnyModelInRegistry();
        resolveMostSpecificModel();
        assertResolvedModelIsNull();
    }

    @Test
    public void testNoModelIsResolvedIfMoreThanOneModelIsAvailable() {
        withTwoResolvedModels();
        resolveMostSpecificModel();
        assertResolvedModelIsNull();
    }

    @Test
    public void testRegistryIsNotQueriedWhenLookupFailureIsCached() {
        withoutAnyModelInRegistry();
        resolveMostSpecificModel();
        verifyRegistryWasQueriedOnceWithoutModelName();

        resolveMostSpecificModel();
        verifyRegistryWasQueriedOnceWithoutModelName();
    }

    @SuppressWarnings("unchecked")
    private void withTwoResolvedModels() {
        when(this.registry.lookupMostSpecificModels(eq(this.resource))).thenReturn(asList(mock(ResolvedModelSource.class), mock(ResolvedModelSource.class)));
    }

    private void withoutAnyModelInRegistry() {
        when(this.registry.lookupMostSpecificModels(eq(this.resource))).thenReturn(null);
    }

    private void withResourceType(String type) {
        doReturn(type).when(this.resource).getResourceType();
    }

    private void withResourcePath(String path) {
        doReturn(path).when(this.resource).getPath();
    }

    private void verifyRegistryWasQueriedOnceWithModelName() {
        verify(this.registry).lookupMostSpecificModels(eq(this.resource), anyString());
    }

    private void verifyRegistryWasQueriedOnceWithoutModelName() {
        verify(this.registry).lookupMostSpecificModels(eq(this.resource));
    }

    private void provideMostSpecificModelWithModelName(String name) {
        this.resolutionResult = this.testee.resolveMostSpecificModelWithName(this.resource, name);
    }

    private void provideMostSpecificModelIncludingBaseTypes() {
        this.resolutionResult = this.testee.resolveMostSpecificModelIncludingModelsForBaseTypes(this.resource);
    }

    private void resolveMostSpecificModel() {
        this.resolutionResult = this.testee.resolveMostSpecificModel(this.resource);
    }

    private void withModelFoundForResourceType(String type) {
        when(this.resolvedModelSource.getResolvedResourceType()).thenReturn(type);
    }

    private void verifyResourceIsMappedToModel() {
        verify(this.mapper).map(eq(this.resource), eq(this.resolvedModelSource));
    }

    private void verifyResourceIsMappedToModelAgain() {
        verify(this.mapper, times(2)).map(eq(this.resource), eq(this.resolvedModelSource));
    }

    private void assertResolvedModelIsNull() {
        assertThat(this.resolutionResult).isNull();
    }

    private void assertResolvedModelIsReturned() {
        assertThat(this.resolutionResult).isSameAs(this.model);
    }

    /**
     * Simulates the key calculation used by the ResourceModelCaches implementation.
     */
    private Key buildCacheInvocationKey(InvocationOnMock invocation) {
        Resource resource = (Resource) invocation.getArguments()[0];
        Key key = (Key) invocation.getArguments()[1];
        return new Key(resource.getPath(), key, resource.getResourceType(), resource.getResourceResolver().hashCode());
    }
}
