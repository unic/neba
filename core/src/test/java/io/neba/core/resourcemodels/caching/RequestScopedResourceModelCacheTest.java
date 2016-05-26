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

package io.neba.core.resourcemodels.caching;

import io.neba.core.util.Key;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.springframework.web.context.request.RequestContextHolder;

import javax.servlet.FilterChain;
import javax.servlet.ServletResponse;
import java.util.concurrent.Callable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

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
    private Logger logger;

    private Object model = new Object();
    private Class<?> modelType = Object.class;

    private Object cachedModel;

    @InjectMocks
    private RequestScopedResourceModelCache testee;

    @Before
    public void setUp() throws Exception {
        doReturn("GET").when(this.request).getMethod();
        doReturn(this.requestPathInfo).when(this.request).getRequestPathInfo();
    }

    @Test
    public void testLookupOfModel() throws Exception {
        request(() -> {
            withResourcePath("/junit/test/1");

            lookupModelFromCache();
            assertModelIsNotInCache();
            putModelInCache();

            lookupModelFromCache();
            assertModelWasFoundInCache();
            return null;
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
            assertModelWasFoundInCache();
            return null;
        });
    }

    @Test
    public void testCacheIsNotSelectorSensitiveWithoutSafeMode() throws Exception {
        request(() -> {

            withResourcePath("/junit/test/1");
            putModelInCache();
            lookupModelFromCache();
            assertModelWasFoundInCache();

            withSelector("new.selector");

            lookupModelFromCache();
            assertModelWasFoundInCache();
            return null;
        });
    }

    @Test
    public void testCacheIsNotSuffixSensitiveWithoutSafeMode() throws Exception {
        request(() -> {

            withResourcePath("/junit/test/1");
            putModelInCache();
            lookupModelFromCache();
            assertModelWasFoundInCache();

            withSuffix("/newSuffix");

            lookupModelFromCache();
            assertModelWasFoundInCache();
            return null;
        });
    }

    @Test
    public void testCacheIsNotPagePathSensitiveWithoutSafeMode() throws Exception {
        request(() -> {

            withPath("/old/path");

            withResourcePath("/junit/test/1");
            putModelInCache();
            lookupModelFromCache();
            assertModelWasFoundInCache();

            withPath("/new/path");

            lookupModelFromCache();
            assertModelWasFoundInCache();
            return null;
        });
    }

    @Test
    public void testCacheIsNotSensitiveToQueryStringWithoutSafeMode() throws Exception {
        request(() -> {

            withResourcePath("/junit/test/1");
            putModelInCache();
            lookupModelFromCache();
            assertModelWasFoundInCache();

            withQueryString("new=parameter");

            lookupModelFromCache();
            assertModelWasFoundInCache();
            return null;
        });
    }

    @Test
    public void testCacheIsSelectorSensitiveInSafeMode() throws Exception {
        request(() -> {

            withSafeMode();
            withResourcePath("/junit/test/1");
            putModelInCache();
            lookupModelFromCache();
            assertModelWasFoundInCache();

            withSelector("new.selector");

            lookupModelFromCache();
            assertModelIsNotInCache();
            return null;
        });
    }

    @Test
    public void testCacheIsSuffixSensitiveInSafeMode() throws Exception {
        request(() -> {

            withSafeMode();
            withResourcePath("/junit/test/1");
            putModelInCache();
            lookupModelFromCache();
            assertModelWasFoundInCache();

            withSuffix("/newSuffix");

            lookupModelFromCache();
            assertModelIsNotInCache();
            return null;
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
            assertModelWasFoundInCache();

            withPath("/new/path");

            lookupModelFromCache();
            assertModelIsNotInCache();
            return null;
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
            assertModelWasFoundInCache();

            withPath("/a/page/jcr:content/parsys");

            lookupModelFromCache();
            assertModelWasFoundInCache();
            return null;
        });
    }

    @Test
    public void testCacheIsSensitiveToQueryStringInSafeMode() throws Exception {
        request(() -> {

            withSafeMode();
            withResourcePath("/junit/test/1");
            putModelInCache();
            lookupModelFromCache();
            assertModelWasFoundInCache();

            withQueryString("new=parameter");

            lookupModelFromCache();
            assertModelIsNotInCache();
            return null;
        });
    }

    @Test
    public void testCacheGracefullyHandlesMissingRequestContextDuringCacheWrite() throws Exception {
        withoutRequestAttributes();
        putModelInCache();
    }

    @Test
    public void testCacheGracefullyHandlesMissingRequestContextDuringCacheRead() throws Exception {
        withoutRequestAttributes();
        lookupModelFromCache();
        assertModelIsNotInCache();
    }

    @Test
    public void testStatisticsEnabledForAnyRequestUri() throws Exception {
        enableStatistics();
        withRequestUri("/arbitrary/request/uri.html");

        request(() -> {
            withResourcePath("/junit/test/1");
            putModelInCache();
            lookupModelFromCache();
            lookupModelFromCache();
            return null;
        });

        verifyStatisticsReportLogs("Request scoped cache report for GET /arbitrary/request/uri.html:\n" +
                "Hits: 2, misses: 0, writes: 1, total number of items: 1\n" +
                "Key {/junit/test/1, class java.lang.Object}: misses=0, hits=2, writes=1\n");
        verifyNothingElseIsReported();
    }

    @Test
    public void testStatisticsEnabledForSpecificRequestUri() throws Exception {
        withStatisticsRestrictedTo("/junit/test/2");
        withRequestUri("/junit/test/1.html");
        enableStatistics();

        request(() -> {
            withResourcePath("/junit/test/1");
            putModelInCache();
            return null;
        });

        verifyNothingIsReported();

        withRequestUri("/junit/test/2.html");
        request(() -> {
            withResourcePath("/junit/test/2");
            putModelInCache();
            return null;
        });

        verifyStatisticsReportLogs("Request scoped cache report for GET /junit/test/2.html:\n" +
                "Hits: 0, misses: 0, writes: 1, total number of items: 1\n" +
                "Key {/junit/test/2, class java.lang.Object}: misses=0, hits=0, writes=1\n");
        verifyNothingElseIsReported();
    }

    private void verifyNothingIsReported() {
        verify(this.logger, never()).info(anyString());
    }

    private void withStatisticsRestrictedTo(String urlFragment) {
        this.testee.setRestrictStatisticsTo(urlFragment);
    }

    private void verifyNothingElseIsReported() {
        verifyNoMoreInteractions(logger);
    }

    private void verifyStatisticsReportLogs(String msg) {
        verify(logger).info(msg);
    }

    private void enableStatistics() {
        this.testee.setEnableStatistics(true);
    }

    private void withSafeMode() {
        this.testee.setSafeMode(true);
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

    private void assertModelWasFoundInCache() {
        assertThat(this.cachedModel).isEqualTo(this.model);
    }

    private void putModelInCache() {
        testee.put(this.resource, this.model, new Key(this.resource.getPath(), this.modelType));
    }

    private void assertModelIsNotInCache() {
        assertThat(this.cachedModel).isNull();
    }

    private void lookupModelFromCache() throws Exception {
        cachedModel = testee.get(new Key(this.resource.getPath(), this.modelType));
    }

    private void withResourcePath(String path) {
        doReturn(path).when(this.resource).getPath();
    }

    private void withRequestUri(String uri) {
        doReturn(uri).when(this.request).getRequestURI();
    }

    private void request(final Callable<Object> callable) throws Exception {
        doAnswer(invocationOnMock -> callable.call()).when(this.chain).doFilter(eq(this.request), eq(this.response));
        this.testee.doFilter(this.request, this.response, this.chain);
    }

    private void withoutRequestAttributes() {
        RequestContextHolder.setRequestAttributes(null);
    }
}