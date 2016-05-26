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

package io.neba.core.resourcemodels.registration;

import io.neba.core.util.OsgiBeanSource;
import org.apache.commons.io.IOUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URL;
import java.util.*;

import static java.lang.System.arraycopy;
import static org.apache.commons.io.IOUtils.toByteArray;
import static org.apache.commons.lang.StringUtils.substringAfterLast;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 * @author Olaf Otto
 */
@RunWith(MockitoJUnitRunner.class)
public class ModelRegistryConsolePluginTest {

    /**
     * @author Olaf Otto
     */
    private static class Model {}

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private ModelRegistry modelRegistry;
    @Mock
    private ResourceResolverFactory factory;
    @Mock
    private ResourceResolver resolver;
    @Mock
    private Resource iconResource;
    @Mock
    private Resource pathResource;
    @Mock
    private ServletOutputStream outputStream;
    @Mock
    private Bundle bundle;
    @Mock
    private Version version;

    private URL resourceUrl;
    private Writer internalWriter;
    private String renderedResponse;
    private Map<String, Collection<OsgiBeanSource<?>>> typeMappings;
    private Collection<OsgiBeanSource<?>> beanSources;

    @InjectMocks
    private ModelRegistryConsolePlugin testee;

    @Before
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        this.internalWriter = new StringWriter();
        Writer writer = new PrintWriter(this.internalWriter);
        this.typeMappings = new HashMap<>();
        this.beanSources = new ArrayList<>();

        doReturn(writer).when(this.response).getWriter();
        doReturn(this.typeMappings).when(this.modelRegistry).getTypeMappings();
        doReturn(this.beanSources).when(this.modelRegistry).getBeanSources();
        doReturn(this.resolver).when(this.factory).getAdministrativeResourceResolver(any());
        doReturn(new String[]{""}).when(this.resolver).getSearchPath();
        doReturn("").when(this.request).getContextPath();
        doReturn("/system/console").when(this.request).getServletPath();
        doReturn(this.outputStream).when(this.response).getOutputStream();
        doReturn("JUnit test bundle").when(this.bundle).getSymbolicName();
        doReturn(this.version).when(this.bundle).getVersion();
        doReturn("1.0.0").when(this.version).toString();
    }

    @Test
    public void testRenderingOfRegisteredModelsTable() throws Exception {
        withRegisteredModel("cq:Page", Model.class, 123L, "beanName");
        renderContent();
        assertResponseContainsTableHead();
        assertResponseContainsNumberOfModelsText(1);
        assertResponseContains("<span class=\"unresolved\">cq:Page</span>", Model.class, 123L, "beanName");
    }

    @Test
    public void testRenderingOfLinkToCrxDe() throws Exception {
        withRegisteredModel("cq:Page", Model.class, 123L, "beanName");
        withResolution("cq/Page", "/libs/foundation/components/primary/cq/Page");
        renderContent();
        assertResponseContains("<a href=\"/crx/de/#" +
                "/libs/foundation/components/primary/cq/Page\" class=\"crxdelink\">" +
                "<img class=\"componentIcon\" src=\"modelregistry/api/componenticon\"/>cq:Page</a>", Model.class, 123L, "beanName");
    }

    @Test
    public void testRetrievalOfStaticJavascript() throws Exception {
        getResource("script.js");
        assertResourceContains("function toggleUnresolvedResourceTypes()");
    }

    @Test
    public void testRenderingOfExistingComponentIcon() throws Exception {
        withIconResource("/apps/project/components/myComponent/icon.png");
        get("/system/console/modelregistry/api/componenticon/apps/project/components/myComponent");
        verifyPluginResolvesResource("/apps/project/components/myComponent/icon.png");
        verifyResponseHasContentType("image/png");
        verifyIconResourceIsAdaptedToInputStream();
    }

    @Test
    public void testRenderingOfDefaultComponentIcon() throws Exception {
        get("/system/console/modelregistry/api/componenticon");
        verifyResponseHasContentType("image/png");
        verifyNoIconResourceIsResolved();
        verifyDefaultComponentIconIsWritten();
    }

    @Test
    public void testModelTypesApi() throws Exception {
        withRegisteredModel("cq:Page", Model.class, 123L, "beanName");
        get("/system/console/modelregistry/api/modeltypes");
        verifyResponseHasContentType("application/json;charset=UTF-8");
        assertResponseIs("[\"io.neba.core.resourcemodels.registration.ModelRegistryConsolePluginTest$Model\"]");
    }

    @Test
    public void testListChildrenViaResourcesApi() throws Exception {
        withPathResource("/junit/test");
        withPathResourceChildren("/junit/test/1", "/junit/test/2");
        withParameter("path", "/junit/test/");

        get("/system/console/modelregistry/api/resources");

        verifyResponseHasContentType("application/json;charset=UTF-8");
        assertResponseIs("[\"/junit/test/1\",\"/junit/test/2\"]");
    }

    @Test
    public void testListChildrenOfRootViaResourcesApi() throws Exception {
        withPathResource("/");
        withPathResourceChildren("/junit", "/test");
        withParameter("path", "/");

        get("/system/console/modelregistry/api/resources");

        verifyResponseHasContentType("application/json;charset=UTF-8");
        assertResponseIs("[\"/junit\",\"/test\"]");
    }

    @Test
    public void testFilterChildrenViaResourcesApi() throws Exception {
        withPathResource("/junit/test");
        withPathResourceChildren("/junit/test/1", "/junit/test/2");
        withParameter("path", "/junit/test/1");

        get("/system/console/modelregistry/api/resources");

        verifyResponseHasContentType("application/json;charset=UTF-8");
        assertResponseIs("[\"/junit/test/1\"]");
    }

    @Test
    public void testFilterModelTypesByPackageName() throws Exception {
        withRegisteredModel("cq:Page", Model.class, 123L, "beanName");
        withParameter("modelTypeName", "io.neba");

        get("/system/console/modelregistry/api/filter");

        verifyResponseHasContentType("application/json;charset=UTF-8");
        assertResponseIs("[\"io.neba.core.resourcemodels.registration.ModelRegistryConsolePluginTest$Model\"]");
    }

    @Test
    public void testFilterModelTypesByTypeName() throws Exception {
        withRegisteredModel("cq:Page", Model.class, 123L, "beanName");
        withParameter("modelTypeName", "io.neba.core.resourcemodels.registration.ModelRegistryConsolePluginTest$Model");

        get("/system/console/modelregistry/api/filter");

        verifyResponseHasContentType("application/json;charset=UTF-8");
        assertResponseIs("[\"io.neba.core.resourcemodels.registration.ModelRegistryConsolePluginTest$Model\"]");
    }

    @Test
    public void testFilterModelTypesByResource() throws Exception {
        withRegisteredModel("cq:Page", Model.class, 123L, "beanName");
        withPathResource("/junit/test", "cq:Page");
        withPathResourceLookedUp();
        withParameter("path", "/junit/test");

        get("/system/console/modelregistry/api/filter");

        verifyResponseHasContentType("application/json;charset=UTF-8");
        assertResponseIs("[\"io.neba.core.resourcemodels.registration.ModelRegistryConsolePluginTest$Model\"]");
    }

    @Test
    public void testFilterModelTypesByResourceAndModelType() throws Exception {
        withRegisteredModel("cq:Page", Model.class, 123L, "beanName");
        withPathResource("/junit/test", "cq:Page");
        withPathResourceLookedUp();

        withParameter("path", "/junit/test");
        withParameter("modelTypeName", "io.neba.core.resourcemodels.registration.ModelRegistryConsolePluginTest$Model");

        get("/system/console/modelregistry/api/filter");

        verifyResponseHasContentType("application/json;charset=UTF-8");
        assertResponseIs("[\"io.neba.core.resourcemodels.registration.ModelRegistryConsolePluginTest$Model\"]");
    }

    private void withPathResourceLookedUp() {
        Set<LookupResult> lookupResults = new HashSet<>();
        for (OsgiBeanSource<?> source : this.beanSources) {
            LookupResult result = mock(LookupResult.class);
            doReturn(source).when(result).getSource();
            doReturn(pathResource.getResourceType()).when(result).getResourceType();
            lookupResults.add(result);
        }
        doReturn(lookupResults).when(this.modelRegistry).lookupAllModels(eq(this.pathResource));
    }

    private void withPathResourceChildren(String... paths) {
        List<Resource> children = new ArrayList<>();
        for (String path : paths) {
            Resource child = mock(Resource.class);
            doReturn(path).when(child).getPath();
            doReturn(substringAfterLast(path, "/")).when(child).getName();
            children.add(child);

        }
        doReturn(children.iterator()).when(this.pathResource).listChildren();
    }

    private void withPathResource(String path) {
        withPathResource(path, null);
    }

    private void withPathResource(String path, String type) {
        doReturn(path).when(this.pathResource).getPath();
        doReturn(this.pathResource).when(this.resolver).getResource(eq(path));
        doReturn(type).when(this.pathResource).getResourceType();
    }


    private void withParameter(String name, String value) {
        doReturn(value).when(this.request).getParameter(eq(name));
    }

    private void withIconResource(String resourcePath) throws IOException {
        doReturn(this.iconResource).when(this.resolver).getResource(eq(resourcePath));
        InputStream in = mock(InputStream.class);
        doReturn(in).when(this.iconResource).adaptTo(eq(InputStream.class));
        doReturn(-1).when(in).read(any());
    }

    private void verifyPluginResolvesResource(String resourcePath) {
        verify(this.resolver).getResource(resourcePath);
    }

    private void verifyNoIconResourceIsResolved() {
        verify(this.resolver, never()).getResource(anyString());
    }

    private void verifyDefaultComponentIconIsWritten() throws IOException {
        byte[] buffer = new byte[4096];
        byte[] expected = toByteArray(getClass().getResourceAsStream("/META-INF/consoleplugin/modelregistry/static/noicon.png"));

        arraycopy(expected, 0, buffer, 0, expected.length);
        for (int i = expected.length; i < buffer.length; ++i) {
            buffer[i] = 0;
        }

        verify(this.outputStream).write(buffer, 0, expected.length);
    }

    private void get(String requestUri) throws ServletException, IOException {
        doReturn(requestUri).when(this.request).getRequestURI();
        this.testee.doGet(this.request, this.response);
        getResponseAsString();
    }

    private void assertResourceContains(String resourceFragment) throws IOException {
        assertThat(this.resourceUrl).isNotNull();
        assertThat(IOUtils.toString(this.resourceUrl.openStream())).contains(resourceFragment);
    }

    private void withResolution(String resourceTypePath, String resourcePath) {
        Resource mock = mock(Resource.class);
        doReturn(mock).when(this.resolver).getResource(eq(resourceTypePath));
        doReturn(resourcePath).when(mock).getPath();
    }

    private void assertResponseContains(String typeName, Class<Model> modelType, long bundleId, String beanName) {
        assertThat(this.renderedResponse).contains("<td>" + typeName + "</td><td>" + modelType.getName() +
                                                   "</td><td>" + beanName + "</td><td><a href=\"bundles/" + bundleId +
                                                   "\" title=\"JUnit test bundle 1.0.0\">" + bundleId + "</a></td>");
    }

    private void assertResponseContainsNumberOfModelsText(int numberOfTests) {
        assertThat(this.renderedResponse).contains(numberOfTests + " Model(s) registered.");
    }

    private void assertResponseIs(String expected) {
        assertThat(this.renderedResponse).isEqualTo(expected);
    }

    private void withRegisteredModel(String typeName, Class<Model> modelType, long bundleId, String beanName) {
        List<OsgiBeanSource<?>> sources = new ArrayList<>();
        OsgiBeanSource<?> source = mock(OsgiBeanSource.class);
        doReturn(modelType).when(source).getBeanType();
        doReturn(bundleId).when(source).getBundleId();
        doReturn(beanName).when(source).getBeanName();
        doReturn(this.bundle).when(source).getBundle();
        sources.add(source);
        this.beanSources.add(source);
        this.typeMappings.put(typeName, sources);
    }

    private void assertResponseContainsTableHead() {
        assertThat(this.renderedResponse).contains("<th>Type</th>");
        assertThat(this.renderedResponse).contains("<th>Model type</th>");
        assertThat(this.renderedResponse).contains("<th>Bean name</th>");
        assertThat(this.renderedResponse).contains("<th>Source bundle</th>");
    }

    private void renderContent() throws ServletException, IOException {
        this.testee.renderContent(this.request, this.response);
        // Remove platform-dependent line endings.
        getResponseAsString();
    }

    private void getResponseAsString() {
        this.renderedResponse = this.internalWriter.toString().replaceAll("[\\n\\r]", "");
    }

    private void getResource(String resource) {
        String resourcePath = "/" + ModelRegistryConsolePlugin.LABEL + "/static/" + resource;
        this.resourceUrl = this.testee.getResource(resourcePath);
    }

    private void verifyIconResourceIsAdaptedToInputStream() {
        verify(this.iconResource).adaptTo(eq(InputStream.class));
    }

    private void verifyResponseHasContentType(String type) {
        verify(this.response).setContentType(type);
    }
}
