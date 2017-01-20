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

package io.neba.core.resourcemodels.tagsupport;

import io.neba.core.resourcemodels.caching.ResourceModelCaches;
import io.neba.core.resourcemodels.mapping.ResourceToModelMapper;
import io.neba.core.resourcemodels.registration.LookupResult;
import io.neba.core.resourcemodels.registration.ModelRegistry;
import io.neba.core.util.Key;
import io.neba.core.util.OsgiBeanSource;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import static io.neba.api.Constants.SYNTHETIC_RESOURCETYPE_ROOT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
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
public class ResourceModelProviderImplTest {
    @Mock
    private ModelRegistry registry;
    @Mock
    private Resource resource;
    @Mock
    private ResourceResolver resourceResolver;
    @Mock
    private ResourceToModelMapper mapper;
    @Mock
    private LookupResult lookupResult;
    @Mock
    private ResourceModelCaches caches;
    private Map<Key, Object> testCache = new HashMap<>();

    private OsgiBeanSource<Object> osgiBeanSource;
    private Object resolutionResult;
    private final Object model = new Object();

    @InjectMocks
    private ResourceModelProviderImpl testee;

    @Before
    @SuppressWarnings("unchecked")
    public void prepareContainerAdapter() {
        Answer storeInCache = invocation -> {
            testCache.put(buildCacheInvocationKey(invocation), invocation.getArguments()[2]);
            return null;
        },

        lookupFromCache = invocation -> testCache.get(buildCacheInvocationKey(invocation));

        doAnswer(storeInCache).when(this.caches).store(isA(Resource.class), isA(OsgiBeanSource.class), any());
        doAnswer(lookupFromCache).when(this.caches).lookup(isA(Resource.class), isA(OsgiBeanSource.class));
        doReturn(this.resourceResolver).when(this.resource).getResourceResolver();

        when(this.mapper.map(isA(Resource.class), isA(OsgiBeanSource.class))).thenAnswer(invocation -> {
            OsgiBeanSource<Object> source = (OsgiBeanSource<Object>) invocation.getArguments()[1];
            return source.getBean();
        });
    }

    @Before
    public void provideMockResourceModel() {
        LinkedList<LookupResult> lookupResults = new LinkedList<>();
        @SuppressWarnings("unchecked")
        OsgiBeanSource<Object> osgiBeanSource = mock(OsgiBeanSource.class);
        this.osgiBeanSource = osgiBeanSource;
        lookupResults.add(this.lookupResult);
        doReturn(this.osgiBeanSource).when(this.lookupResult).getSource();
        when(this.osgiBeanSource.getBean()).thenReturn(this.model);
        doReturn(this.model.getClass()).when(this.osgiBeanSource).getBeanType();
        when(this.registry.lookupMostSpecificModels(eq(this.resource))).thenReturn(lookupResults);
        when(this.registry.lookupMostSpecificModels(eq(this.resource), anyString())).thenReturn(lookupResults);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testResolveMostSpecificModelWithBeanNameRequiresResource() throws Exception {
        this.testee.resolveMostSpecificModelWithBeanName(null, "beanName");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testResolveMostSpecificModelWithBeanNameRequiresBeanName() throws Exception {
        this.testee.resolveMostSpecificModelWithBeanName(mock(Resource.class), null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testResolveMostSpecificModelRequiresResource() throws Exception {
        this.testee.resolveMostSpecificModel(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testResolveMostSpecificModelsIncludingBaseTypesRequiresResource() throws Exception {
        this.testee.resolveMostSpecificModelIncludingModelsForBaseTypes(null);
    }

    @Test
    public void testResolutionOfSpecificModelIncludingBaseTypes() throws Exception {
        provideMostSpecificModelIncludingBaseTypes();
        verifyResourceIsMappedToModel();
        assertResolvedModelIsReturned();
    }

    @Test
    public void testResolutionOfSpecificModelWithModelForNtUnstructured() throws Exception {
        withModelFoundForResourceType("nt:unstructured");
        provideMostSpecificModel();
        assertResolvedModelIsNull();
    }

    @Test
    public void testResolutionOfSpecificModelWithModelForNtBase() throws Exception {
        withModelFoundForResourceType("nt:base");
        provideMostSpecificModel();
        assertResolvedModelIsNull();
    }

    @Test
    public void testResolutionOfSpecificModelWithModelForSyntheticResourceRoot() throws Exception {
        withModelFoundForResourceType(SYNTHETIC_RESOURCETYPE_ROOT);
        provideMostSpecificModel();
        assertResolvedModelIsNull();
    }

    @Test
    public void testResolutionOfAnyModelWithModelForNtUnstructured() throws Exception {
        withModelFoundForResourceType("nt:unstructured");
        provideMostSpecificModelIncludingBaseTypes();
        verifyResourceIsMappedToModel();
        assertResolvedModelIsReturned();
    }

    @Test
    public void testResolutionOfAnyModelWithModelForNtBase() throws Exception {
        withModelFoundForResourceType("nt:base");
        provideMostSpecificModelIncludingBaseTypes();
        verifyResourceIsMappedToModel();
        assertResolvedModelIsReturned();
    }

    @Test
    public void testResolutionOfAnyModelWithModelForSyntheticResourceRoot() throws Exception {
        withModelFoundForResourceType(SYNTHETIC_RESOURCETYPE_ROOT);
        provideMostSpecificModelIncludingBaseTypes();
        verifyResourceIsMappedToModel();
        assertResolvedModelIsReturned();
    }

    @Test
    public void testResolutionOfAnyModelWithSpecificBeanName() throws Exception {
        provideMostSpecificModelWithBeanName("unitTestBean");
        verifyRegistryIsQueriedWithBeanName();
        verifyResourceIsMappedToModel();
        assertResolvedModelIsReturned();
    }

    @Test
    public void testResolutionOfAnyModelWithSpecificBeanNameDisregardsBasicTypes() throws Exception {
        withModelFoundForResourceType("nt:base");
        provideMostSpecificModelWithBeanName("unitTestBean");
        verifyRegistryIsQueriedWithBeanName();
        verifyResourceIsMappedToModel();
        assertResolvedModelIsReturned();
    }

    @Test
    public void testSubsequentResolutionOfSameResourceWithDifferentResourceTypeIsNotServedFromCache() throws Exception {
        withResourcePath("/resource/path");
        withResourceType("resource/type/one");

        provideMostSpecificModel();
        verifyResourceIsMappedToModel();

        withResourceType("resource/type/two");
        provideMostSpecificModel();
        verifyResourceIsMappedToModelAgain();
    }

    @Test
    public void testSubsequentResolutionOfSameResourceWithSameResourceTypeIsServedFromCache() throws Exception {
        withResourcePath("/resource/path");

        withResourceType("resource/type/one");

        provideMostSpecificModel();
        verifyResourceIsMappedToModel();

        withResourceType("resource/type/one");
        provideMostSpecificModel();
        verifyResourceIsMappedToModel();
    }

    private void withResourceType(String type) {
        doReturn(type).when(this.resource).getResourceType();
    }

    private void withResourcePath(String path) {
        doReturn(path).when(this.resource).getPath();
    }

    private void verifyRegistryIsQueriedWithBeanName() {
        verify(this.registry).lookupMostSpecificModels(eq(this.resource), anyString());
    }

    private void provideMostSpecificModelWithBeanName(String name) {
        this.resolutionResult = this.testee.resolveMostSpecificModelWithBeanName(this.resource, name);
    }

    private void provideMostSpecificModelIncludingBaseTypes() {
        this.resolutionResult = this.testee.resolveMostSpecificModelIncludingModelsForBaseTypes(this.resource);
    }

    private void provideMostSpecificModel() {
        this.resolutionResult = this.testee.resolveMostSpecificModel(this.resource);
    }

    private void withModelFoundForResourceType(String type) {
        when(this.lookupResult.getResourceType()).thenReturn(type);
    }

    private void verifyResourceIsMappedToModel() {
        verify(this.mapper).map(eq(this.resource), eq(this.osgiBeanSource));
    }

    private void verifyResourceIsMappedToModelAgain() {
        verify(this.mapper, times(2)).map(eq(this.resource), eq(this.osgiBeanSource));
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
        OsgiBeanSource<?> modelSource = (OsgiBeanSource<?>) invocation.getArguments()[1];
        return new Key(resource.getPath(), modelSource.getBeanType(), resource.getResourceType(), resource.getResourceResolver().hashCode());
    }
}
