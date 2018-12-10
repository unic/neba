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
import io.neba.core.util.Key;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.servlet.FilterChain;
import javax.servlet.ServletResponse;

import static io.neba.core.resourcemodels.caching.ResourceModelCaches.key;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

/**
 * @author Olaf Otto
 */
@RunWith(MockitoJUnitRunner.class)
public class RequestScopedResourceModelCacheTest {
    @Mock
    private Resource resource;
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

    private Object model = new Object();
    private Class<?> modelType = Object.class;

    private Object cachedModel;

    private RequestScopedResourceModelCache testee;

    @Before
    public void setUp() {
        this.testee = new RequestScopedResourceModelCache();

        doReturn(this.requestPathInfo)
                .when(this.request)
                .getRequestPathInfo();

        doReturn(true).when(this.configuration).enabled();
        doReturn(false).when(this.configuration).safeMode();

        this.testee.activate(this.configuration);
    }

    @Test
    public void testLookupOfModel() throws Exception {
        request(() -> {
            withResourcePath("/junit/test/1");

            lookupModelFromCache();
            assertModelIsNotInCache();
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
            assertModelIsNotInCache();

            withResourcePath("/junit/test/1");
            lookupModelFromCache();
            assertModelIsInCache();
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

            withPath("/old/path");

            withResourcePath("/junit/test/1");
            putModelInCache();
            lookupModelFromCache();
            assertModelIsInCache();

            withPath("/new/path");

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
            assertModelIsNotInCache();
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
            assertModelIsNotInCache();
        });
    }

    @Test
    public void testCacheIsPagePathSensitiveInSafeMode() throws Exception {
        request(() -> {

            withSafeMode();
            withPath("/old/path");

            withResourcePath("/junit/test/1");
            putModelInCache();
            lookupModelFromCache();
            assertModelIsInCache();

            withPath("/new/path");

            lookupModelFromCache();
            assertModelIsNotInCache();
        });
    }

    @Test
    public void testCacheIsNotSensitiveToPathChangesBelowCurrentPageInSafeMode() throws Exception {
        request(() -> {

            withSafeMode();
            withPath("/a/page");

            withResourcePath("/junit/test/1");
            putModelInCache();
            lookupModelFromCache();
            assertModelIsInCache();

            withPath("/a/page/jcr:content/parsys");

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
            assertModelIsNotInCache();
        });
    }

    @Test
    public void testCacheGracefullyHandlesMissingRequestContextDuringCacheWrite() {
        putModelInCache();
    }

    @Test
    public void testCacheGracefullyHandlesMissingRequestContextDuringCacheRead() {
        lookupModelFromCache();
        assertModelIsNotInCache();
    }

    @Test
    public void testNoModelsAreStoredWhenCacheIsDisabled() throws Exception {
        request(() -> {
            withDisabledCache();
            withResourcePath("/junit/test/1");
            putModelInCache();
            lookupModelFromCache();
            assertModelIsNotInCache();
        });
    }

    @Test
    public void testCacheToleratesNullModelWrite() throws Exception {
        request(() -> {
            testee.put(this.resource, null, key());
            putModelInCache();
        });
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCacheDoesNotTolerateNullResourceWrite() throws Exception {
        request(() -> {
            testee.put(null, this.model, key());
            putModelInCache();
        });
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCacheDoesNotTolerateNullKeyWrite() throws Exception {
        request(() -> {
            testee.put(this.resource, this.model, null);
        });
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

    private void withPath(String path) {
        when(this.requestPathInfo.getResourcePath()).thenReturn(path);
    }

    private void withSuffix(String suffix) {
        when(this.requestPathInfo.getSuffix()).thenReturn(suffix);
    }

    private void withSelector(String selectorString) {
        when(this.requestPathInfo.getSelectorString()).thenReturn(selectorString);
    }

    private void putModelInCache() {
        testee.put(this.resource, this.model, new Key(this.resource.getPath(), this.modelType));
    }

    private void assertModelIsInCache() {
        assertThat(this.cachedModel).isEqualTo(this.model);
    }

    private void assertModelIsNotInCache() {
        assertThat(this.cachedModel).isNull();
    }

    private void lookupModelFromCache() {
        cachedModel = testee.get(new Key(this.resource.getPath(), this.modelType));
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