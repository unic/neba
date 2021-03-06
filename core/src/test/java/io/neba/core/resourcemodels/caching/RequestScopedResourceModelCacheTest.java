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

package io.neba.core.resourcemodels.caching;

import io.neba.core.resourcemodels.caching.RequestScopedResourceModelCache.Configuration;
import io.neba.core.resourcemodels.metadata.ResourceModelMetaData;
import io.neba.core.resourcemodels.metadata.ResourceModelMetaDataRegistrar;
import io.neba.core.resourcemodels.metadata.ResourceModelStatistics;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.servlet.FilterChain;
import javax.servlet.ServletResponse;
import java.util.Optional;

import static io.neba.core.util.Key.key;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Olaf Otto
 */
@RunWith(MockitoJUnitRunner.class)
public class RequestScopedResourceModelCacheTest {
    @Mock
    private Resource resource;
    @Mock
    private ResourceResolver resolver;
    @Mock
    private SlingHttpServletRequest request;
    @Mock
    private RequestPathInfo requestPathInfo;
    @Mock
    private ServletResponse response;
    @Mock
    private FilterChain chain;
    @Mock
    private Configuration configuration;
    @Mock
    private ResourceModelMetaDataRegistrar metaDataRegistrar;
    @Mock
    private ResourceModelMetaData resourceModelMetaData;
    @Mock
    private ResourceModelStatistics resourceModelStatistics;

    private Object model = new Object();
    private Class<?> modelType = Object.class;

    private Optional<Object> cachedModel;

    @InjectMocks
    private RequestScopedResourceModelCache testee;

    @Before
    public void setUp() {
        doReturn(this.resourceModelMetaData)
                .when(this.metaDataRegistrar)
                .get(this.modelType);

        doReturn(this.resourceModelStatistics)
                .when(this.resourceModelMetaData)
                .getStatistics();

        doReturn(this.requestPathInfo)
                .when(this.request)
                .getRequestPathInfo();
        doReturn(this.resolver).when(this.resource).getResourceResolver();

        doReturn(true).when(this.configuration).enabled();
        doReturn(false).when(this.configuration).safeMode();

        this.testee.activate(this.configuration);
    }

    @Test
    public void testLookupOfModel() throws Exception {
        request(() -> {
            withResourcePath("/junit/test/1");

            lookupModelFromCache();
            assertModelIsNotKnownToCache();
            putModelInCache();

            lookupModelFromCache();
            assertModelIsInCache();
        });
    }

    @Test
    public void testLookupOfDifferentResourcePaths() throws Exception {
        request(() -> {

            withResourcePath("/junit/test/1");
            putModelInCache();

            withResourcePath("/junit/test/2");
            lookupModelFromCache();
            assertModelIsNotKnownToCache();

            withResourcePath("/junit/test/1");
            lookupModelFromCache();
            assertModelIsInCache();
        });
    }

    @Test
    public void testLookupOfSameResourcePathsWithDifferentResourceTypes() throws Exception {
        request(() -> {

            withResourcePath("/junit/test/1");
            withResourceType("resource/type");
            putModelInCache();
            lookupModelFromCache();
            assertModelIsInCache();

            withResourceType("other/resource/type");
            lookupModelFromCache();
            assertModelIsNotKnownToCache();
        });
    }

    @Test
    public void testLookupOfSameResourcePathsWithDifferentResourceResolversWithoutUserIds() throws Exception {
        request(() -> {
            withResourcePath("/junit/test/1");
            withDifferentResourceResolver();
            putModelInCache();
            lookupModelFromCache();
            assertModelIsInCache();

            withDifferentResourceResolver();
            lookupModelFromCache();
            assertModelIsNotKnownToCache();
        });
    }

    @Test
    public void testLookupOfSameResourcePathsWithDifferentResourceResolversWithUserIds() throws Exception {
        request(() -> {
            withResourcePath("/junit/test/1");
            withResourceResolverUserId("anonymous");
            putModelInCache();
            lookupModelFromCache();
            assertModelIsInCache();

            withResourceResolverUserId("admin");
            lookupModelFromCache();
            assertModelIsNotKnownToCache();
        });
    }

    @Test
    public void testCacheIsNotSelectorSensitiveWithoutSafeMode() throws Exception {
        request(() -> {

            withResourcePath("/junit/test/1");
            putModelInCache();
            lookupModelFromCache();
            assertModelIsInCache();

            withSelector("new.selector");

            lookupModelFromCache();
            assertModelIsInCache();
        });
    }

    @Test
    public void testCacheIsNotSuffixSensitiveWithoutSafeMode() throws Exception {
        request(() -> {

            withResourcePath("/junit/test/1");
            putModelInCache();
            lookupModelFromCache();
            assertModelIsInCache();

            withSuffix("/newSuffix");

            lookupModelFromCache();
            assertModelIsInCache();
        });
    }

    @Test
    public void testCacheIsNotPagePathSensitiveWithoutSafeMode() throws Exception {
        request(() -> {

            withRequestedPagePath("/old/path");

            withResourcePath("/junit/test/1");
            putModelInCache();
            lookupModelFromCache();
            assertModelIsInCache();

            withRequestedPagePath("/new/path");

            lookupModelFromCache();
            assertModelIsInCache();
        });
    }

    @Test
    public void testCacheIsNotSensitiveToQueryStringWithoutSafeMode() throws Exception {
        request(() -> {

            withResourcePath("/junit/test/1");
            putModelInCache();
            lookupModelFromCache();
            assertModelIsInCache();

            withQueryString("new=parameter");

            lookupModelFromCache();
            assertModelIsInCache();
        });
    }

    @Test
    public void testCacheIsSelectorSensitiveInSafeMode() throws Exception {
        request(() -> {

            withSafeMode();
            withResourcePath("/junit/test/1");
            putModelInCache();
            lookupModelFromCache();
            assertModelIsInCache();

            withSelector("new.selector");

            lookupModelFromCache();
            assertModelIsNotKnownToCache();
        });
    }

    @Test
    public void testCacheIsSuffixSensitiveInSafeMode() throws Exception {
        request(() -> {

            withSafeMode();
            withResourcePath("/junit/test/1");
            putModelInCache();
            lookupModelFromCache();
            assertModelIsInCache();

            withSuffix("/newSuffix");

            lookupModelFromCache();
            assertModelIsNotKnownToCache();
        });
    }

    @Test
    public void testCacheIsPagePathSensitiveInSafeMode() throws Exception {
        request(() -> {

            withSafeMode();
            withRequestedPagePath("/old/path");

            withResourcePath("/junit/test/1");
            putModelInCache();
            lookupModelFromCache();
            assertModelIsInCache();

            withRequestedPagePath("/new/path");

            lookupModelFromCache();
            assertModelIsNotKnownToCache();
        });
    }

    @Test
    public void testCacheIsNotSensitiveToPathChangesBelowCurrentPageInSafeMode() throws Exception {
        request(() -> {

            withSafeMode();
            withRequestedPagePath("/a/page");

            withResourcePath("/junit/test/1");
            putModelInCache();
            lookupModelFromCache();
            assertModelIsInCache();

            withRequestedPagePath("/a/page/jcr:content/parsys");

            lookupModelFromCache();
            assertModelIsInCache();
        });
    }

    @Test
    public void testCacheIsSensitiveToQueryStringInSafeMode() throws Exception {
        request(() -> {

            withSafeMode();
            withResourcePath("/junit/test/1");
            putModelInCache();
            lookupModelFromCache();
            assertModelIsInCache();

            withQueryString("new=parameter");

            lookupModelFromCache();
            assertModelIsNotKnownToCache();
        });
    }

    @Test
    public void testCacheGracefullyHandlesMissingRequestContextDuringCacheWrite() {
        putModelInCache();
    }

    @Test
    public void testCacheGracefullyHandlesMissingRequestContextDuringCacheRead() {
        lookupModelFromCache();
        assertModelIsNotKnownToCache();
    }

    @Test
    public void testNoModelsAreStoredWhenCacheIsDisabled() throws Exception {
        request(() -> {
            withDisabledCache();
            withResourcePath("/junit/test/1");
            putModelInCache();
            lookupModelFromCache();
            assertModelIsNotKnownToCache();
        });
    }

    @Test
    public void testCacheToleratesNullModelWrite() throws Exception {
        request(() -> {
            testee.put(this.resource, key(), null);
            putModelInCache();
        });
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCacheDoesNotTolerateNullResourceWrite() throws Exception {
        request(() -> {
            testee.put(null, key(), this.model);
            putModelInCache();
        });
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCacheDoesNotTolerateNullKeyWrite() throws Exception {
        request(() -> {
            testee.put(this.resource, null, this.model);
        });
    }

    @Test
    public void testLookupOfUnknownModelIsNotCountedAsCacheHit() throws Exception {
        request(() -> {
            lookupModelFromCache();
            assertModelIsNotKnownToCache();
            verifyNoCacheHitIsCounted();
        });
    }

    @Test
    public void testKnownLookupFailureIsNotCountedAsCacheHit() throws Exception {
        request(() -> {
            withNullModel();
            putModelInCache();
            lookupModelFromCache();
            assertModelIsKnownLookupFailure();
            verifyNoCacheHitIsCounted();
        });
    }

    @Test
    public void testSuccessfulModelLookupIsCountedAsCacheHit() throws Exception {
        request(() -> {
            putModelInCache();
            lookupModelFromCache();
            assertModelIsInCache();
            verifyCacheHitIsCounted();
        });
    }

    private void verifyCacheHitIsCounted() {
        verify(this.resourceModelStatistics).countCacheHit();
    }

    private void withNullModel() {
        this.model = null;
    }

    private void verifyNoCacheHitIsCounted() {
        verify(this.resourceModelStatistics, never()).countCacheHit();
    }

    private void withResourceType(String type) {
        doReturn(type).when(this.resource).getResourceType();
    }

    private void withDifferentResourceResolver() {
        doReturn(mock(ResourceResolver.class)).when(this.resource).getResourceResolver();
    }

    private void withResourceResolverUserId(String userId) {
        doReturn(userId).when(this.resolver).getUserID();
    }

    private void withDisabledCache() {
        doReturn(false).when(this.configuration).enabled();
    }

    private void withSafeMode() {
        doReturn(true).when(this.configuration).safeMode();
    }

    private void withQueryString(String queryString) {
        when(this.request.getQueryString()).thenReturn(queryString);
    }

    private void withRequestedPagePath(String path) {
        when(this.requestPathInfo.getResourcePath()).thenReturn(path);
    }

    private void withSuffix(String suffix) {
        when(this.requestPathInfo.getSuffix()).thenReturn(suffix);
    }

    private void withSelector(String selectorString) {
        when(this.requestPathInfo.getSelectorString()).thenReturn(selectorString);
    }

    private void putModelInCache() {
        testee.put(this.resource, key(this.modelType), this.model);
    }

    private void assertModelIsInCache() {
        assertThat(this.cachedModel).contains(this.model);
    }

    private void assertModelIsNotKnownToCache() {
        assertThat(this.cachedModel).isNull();
    }

    private void assertModelIsKnownLookupFailure() {
        assertThat(this.cachedModel).isEmpty();
    }

    private void lookupModelFromCache() {
        cachedModel = testee.get(this.resource, key(this.modelType));
    }

    private void withResourcePath(String path) {
        doReturn(path).when(this.resource).getPath();
    }

    private void request(final Request request) throws Exception {
        doAnswer(invocationOnMock -> {
            request.request();
            return null;
        }).when(this.chain).doFilter(eq(this.request), eq(this.response));
        this.testee.doFilter(this.request, this.response, this.chain);
    }

    private interface Request {
        void request() throws Exception;
    }
}
