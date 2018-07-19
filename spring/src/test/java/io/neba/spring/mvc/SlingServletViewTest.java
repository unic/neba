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
package io.neba.spring.mvc;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.adapter.AdapterManager;
import org.apache.sling.api.adapter.SlingAdaptable;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.junit.After;
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
import static org.assertj.core.api.Assertions.entry;
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
    @Mock
    private AdapterManager adapterManager;

    private Resource resourceProvidedToScript;

    private SlingServletView testee;

    @Before
    public void setUp() throws Exception {
        when(this.request.getResourceResolver())
                .thenReturn(this.resourceResolver);

        when(this.request.getPathInfo())
                .thenReturn("/bin/mvc.do/controller/path");

        doAnswer(
                inv -> resourceProvidedToScript = ((SlingHttpServletRequest) inv.getArguments()[0]).getResource())
                .when(this.script)
                .service(isA(SlingServletView.MvcResourceRequest.class), eq(this.response));

        SlingAdaptable.setAdapterManager(this.adapterManager);

        this.testee = new SlingServletView("some/resource/type", this.script);
    }

    @After
    public void tearDown() throws Exception {
        SlingAdaptable.unsetAdapterManager(this.adapterManager);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorRequireNonNullResourceType() throws Exception {
        new SlingServletView(null, this.script);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorRequireNonNullServlet() throws Exception {
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
    public void testViewProvidesModelEntriesAsResourcesValueMap() throws Exception {
        Map<String, Object> model = new HashMap<>();
        model.put("key", "value");

        renderViewWithModel(model);

        assertResourceHasConsistentValueMapRepresentation();
        assertResourceHasProperty("key", "value");
        verifyScriptIsInvoked();
    }

    @Test
    public void testControllerResourceCanBeAdaptedToMapRepresentation() throws Exception {
        Map<String, Object> model = new HashMap<>();
        model.put("key", "value");

        renderViewWithModel(model);

        assertResourceHasMapRepresentationWithProperty("key", "value");
        verifyScriptIsInvoked();
    }

    @Test
    public void testNullModelYieldsEmptyValueMapRepresentation() throws Exception {
        renderViewWithModel(null);

        assertResourceHasConsistentValueMapRepresentation();
        verifyScriptIsInvoked();
    }

    @Test
    public void testSpringControllerResourceAdaptationYieldsNull() throws Exception {
        renderViewWithModel(null);

        adaptResourceProvidedToScriptTo(getClass());

        verifyAdaptermanagerTriedToAdaptResourceProvidedToScriptTo(getClass());
    }

    @Test
    public void testViewProvidesScriptWithResourceRepresentingViewResourceType() throws Exception {
        renderViewWithModel(null);

        assertResourceIsProvidedToScript();
        assertResourceTypeIs("some/resource/type");
        assertResourceResolverIs(this.resourceResolver);
        assertResourcePathIs("/bin/mvc.do/controller/path");
    }

    @SuppressWarnings("unchecked")
    private void assertResourceHasMapRepresentationWithProperty(String key, String value) {
        assertThat(this.resourceProvidedToScript.adaptTo(Map.class)).contains(entry(key, value));
    }

    private void assertResourceHasProperty(String key, String value) {
        assertThat(this.resourceProvidedToScript.getValueMap()).contains(entry(key, value));
    }

    private void assertResourceHasConsistentValueMapRepresentation() {
        assertThat(this.resourceProvidedToScript).isNotNull();
        assertThat(this.resourceProvidedToScript.adaptTo(ValueMap.class)).isNotNull();
        assertThat(this.resourceProvidedToScript.getValueMap()).isNotNull();

        assertThat(this.resourceProvidedToScript.adaptTo(ValueMap.class)).containsAllEntriesOf(this.resourceProvidedToScript.getValueMap());
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

    private void verifyAdaptermanagerTriedToAdaptResourceProvidedToScriptTo(Class<?> type) {
        verify(this.adapterManager).getAdapter(this.resourceProvidedToScript, type);
    }

    private void adaptResourceProvidedToScriptTo(Class<?> type) {
        this.resourceProvidedToScript.adaptTo(type);
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