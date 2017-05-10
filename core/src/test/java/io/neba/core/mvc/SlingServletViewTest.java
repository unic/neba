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
package io.neba.core.mvc;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Olaf Otto
 */
@RunWith(MockitoJUnitRunner.class)
public class SlingServletViewTest {
    @Mock
    private SlingHttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private ResourceResolver resourceResolver;
    @Mock
    private Servlet script;

    private Resource resourceProvidedToScript;

    private SlingServletView testee;

    @Before
    public void setUp() throws Exception {
        when(this.request.getResourceResolver()).thenReturn(this.resourceResolver);
        when(this.request.getPathInfo()).thenReturn("/bin/mvc.do/controller/path");

        doAnswer(
                invocation ->
                        resourceProvidedToScript = ((SlingHttpServletRequest) invocation.getArguments()[0]).getResource())
                .when(this.script)
                .service(isA(SlingServletView.MvcResourceRequest.class), eq(this.response));

        this.testee = new SlingServletView("some/resource/type", this.script);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testContstructorRequireNonNullResourceType() throws Exception {
        new SlingServletView(null, this.script);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testContstructorRequireNonNullServlet() throws Exception {
        new SlingServletView("some/resource/type", null);
    }

    @Test
    public void testViewToleratesNullModel() throws Exception {
        renderViewWithModel(null);
        verifyScriptIsInvoked();
    }

    @Test
    public void testViewDoesNotDefineContentTypeInAdvance() throws Exception {
        assertThat(this.testee.getContentType()).isNull();
    }

    @Test
    public void testViewProvidesModelEntriesAsRequestAttributes() throws Exception {
        Map<String, Object> model = new HashMap<>();
        model.put("key", "value");

        renderViewWithModel(model);

        verifyRequestAttributeIsSet("key", "value");
        verifyScriptIsInvoked();
    }

    @Test
    public void testViewProvidesScriptWithResourceRepresentingViewResourceType() throws Exception {
        renderViewWithModel(null);

        assertResourceIsProvidedToScript();
        assertResourceTypeIs("some/resource/type");
        assertResourceResolverIs(this.resourceResolver);
        assertResourcePathIs("/bin/mvc.do/controller/path");
    }

    private void assertResourcePathIs(String path) {
        assertThat(this.resourceProvidedToScript.getPath()).isEqualTo(path);
    }

    private void assertResourceResolverIs(ResourceResolver resourceResolver) {
        assertThat(this.resourceProvidedToScript.getResourceResolver()).isEqualTo(resourceResolver);
    }

    private void assertResourceTypeIs(String type) {
        assertThat(this.resourceProvidedToScript.getResourceType()).isEqualTo(type);
    }

    private void assertResourceIsProvidedToScript() {
        assertThat(this.resourceProvidedToScript).isNotNull();
    }

    private void verifyRequestAttributeIsSet(String key, String value) {
        verify(this.request).setAttribute(key, value);
    }

    private void verifyScriptIsInvoked() throws ServletException, IOException {
        verify(this.script).service(isA(SlingServletView.MvcResourceRequest.class), eq(this.response));
    }

    private void renderViewWithModel(Map<String, ?> model) throws Exception {
        this.testee.render(model, this.request, this.response);
    }
}