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

package io.neba.core.resourcemodels.registration;

import io.neba.core.util.OsgiModelSource;
import io.neba.core.util.ResolvedModelSource;
import org.apache.commons.io.IOUtils;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Workspace;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static java.lang.System.arraycopy;
import static org.apache.commons.io.IOUtils.toByteArray;
import static org.apache.commons.lang3.StringUtils.substringAfterLast;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * @author Olaf Otto
 */
@RunWith(MockitoJUnitRunner.class)
public class ModelRegistryConsolePluginTest {

    /**
     * @author Olaf Otto
     */
    private static class Model {
    }

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
    private Session session;
    @Mock
    private Workspace workspace;
    @Mock
    private NodeTypeManager nodeTypeManager;
    @Mock
    private NodeType nodeType;
    @Mock
    private Resource iconResource;
    @Mock
    private Resource pathResource;
    @Mock
    private ServletOutputStream outputStream;
    @Mock
    private ServletContext servletContext;
    @Mock
    private ServletConfig servletConfig;
    @Mock
    private Bundle bundle;
    @Mock
    private BundleContext bundleContext;
    @Mock
    private Version version;

    private URL resourceUrl;
    private Writer internalWriter;
    private String renderedResponse;
    private Map<String, Collection<OsgiModelSource<?>>> typeMappings;
    private Collection<OsgiModelSource<?>> modelSources;

    @InjectMocks
    private ModelRegistryConsolePlugin testee;

    @Before
    public void setUp() throws Exception {
        this.internalWriter = new StringWriter();
        Writer writer = new PrintWriter(this.internalWriter);
        this.typeMappings = new HashMap<>();
        this.modelSources = new ArrayList<>();

        doReturn(writer)
                .when(this.response)
                .getWriter();

        doReturn(this.typeMappings)
                .when(this.modelRegistry)
                .getTypeMappings();

        doReturn(this.modelSources)
                .when(this.modelRegistry)
                .getModelSources();

        doReturn(this.resolver)
                .when(this.factory)
                .getResourceResolver(any());

        doReturn(this.servletContext)
                .when(this.servletConfig)
                .getServletContext();

        doReturn("")
                .when(this.servletContext)
                .getContextPath();

        doReturn(new String[]{""})
                .when(this.resolver)
                .getSearchPath();

        doReturn("")
                .when(this.request)
                .getContextPath();

        doReturn("/system/console")
                .when(this.request)
                .getServletPath();

        doReturn(this.outputStream)
                .when(this.response)
                .getOutputStream();

        doReturn("JUnit test bundle")
                .when(this.bundle)
                .getSymbolicName();

        doReturn(this.version)
                .when(this.bundle)
                .getVersion();

        doReturn("1.0.0")
                .when(this.version)
                .toString();

        doReturn(this.session)
                .when(this.resolver)
                .adaptTo(Session.class);

        doReturn(this.workspace)
                .when(this.session)
                .getWorkspace();

        doReturn(this.nodeTypeManager)
                .when(this.workspace)
                .getNodeTypeManager();

        doReturn(this.nodeType)
                .when(this.nodeTypeManager)
                .getNodeType("nt:unstructured");

        doReturn(new Bundle[]{})
                .when(this.bundleContext)
                .getBundles();

        doThrow(new LoginException("THIS IS AN EXPECTED TEST EXCEPTION"))
                .when(this.factory)
                .getServiceResourceResolver(any());

        this.testee.activate(this.bundleContext);
        this.testee.init(this.servletConfig);
    }

    @Test
    public void testRenderingOfRegisteredModelsTable() throws Exception {
        withRegisteredModel("cq:Page", Model.class, 123L, "modelName");

        renderContent();

        assertResponseContainsTableHead();
        assertResponseContainsNumberOfModelsText(1);
        assertResponseContainsModel("<span class=\"unresolved\">cq:Page</span>", Model.class, 123L, "modelName");
    }

    @Test
    public void testRenderingOfExistingNodeTypeWithoutSlingResourceType() throws Exception {
        withRegisteredModel("nt:unstructured", Model.class, 123L, "modelName");

        renderContent();

        assertResponseContainsModel(
                "<img class=\"componentIcon\" src=\"modelregistry/api/componenticon\"/> nt:unstructured", Model.class, 123L, "modelName");
    }

    @Test
    public void testHandlingOfRepositoryExceptionDuringNodeTypeRetrieval() throws Exception {
        withRepositoryExceptionDuringNodeTypeManagerRetrieval();
        withRegisteredModel("nt:unstructured", Model.class, 123L, "modelName");

        renderContent();

        assertResponseContainsModel(
                "<span class=\"unresolved\">nt:unstructured</span>", Model.class, 123L, "modelName");

    }

    @Test
    public void testLinkToComposumConsoleWhenComposumIsAvailable() throws Exception {
        withComposumConsoleBundlePresent();

        withRegisteredModel("some/resource/type", Model.class, 123L, "modelName");
        withResolution("some/resource/type", "/apps/some/resource/type");

        renderContent();

        assertResponseContainsModel("<a href=\"/bin/browser.html" +
                "/apps/some/resource/type\" class=\"consoleLink\">" +
                "<img class=\"componentIcon\" src=\"modelregistry/api/componenticon\"/>some/resource/type</a>", Model.class, 123L, "modelName");

    }

    @Test
    public void testRenderingOfLinkToCrxDe() throws Exception {
        withRegisteredModel("cq:Page", Model.class, 123L, "modelName");
        withResolution("cq/Page", "/libs/foundation/components/primary/cq/Page");

        renderContent();

        assertResponseContainsModel("<a href=\"/crx/de/#" +
                "/libs/foundation/components/primary/cq/Page\" class=\"consoleLink\">" +
                "<img class=\"componentIcon\" src=\"modelregistry/api/componenticon\"/>cq:Page</a>", Model.class, 123L, "modelName");
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
        withRegisteredModel("cq:Page", Model.class, 123L, "modelName");

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
        withRegisteredModel("cq:Page", Model.class, 123L, "modelName");
        withParameter("modelTypeName", "io.neba");

        get("/system/console/modelregistry/api/filter");

        verifyResponseHasContentType("application/json;charset=UTF-8");
        assertResponseIs("[\"io.neba.core.resourcemodels.registration.ModelRegistryConsolePluginTest$Model\"]");
    }

    @Test
    public void testFilterModelTypesByTypeName() throws Exception {
        withRegisteredModel("cq:Page", Model.class, 123L, "modelName");
        withParameter("modelTypeName", "io.neba.core.resourcemodels.registration.ModelRegistryConsolePluginTest$Model");

        get("/system/console/modelregistry/api/filter");

        verifyResponseHasContentType("application/json;charset=UTF-8");
        assertResponseIs("[\"io.neba.core.resourcemodels.registration.ModelRegistryConsolePluginTest$Model\"]");
    }

    @Test
    public void testFilterModelTypesByResource() throws Exception {
        withRegisteredModel("cq:Page", Model.class, 123L, "modelName");
        withPathResource("/junit/test");
        withPathResourceLookedUp();
        withParameter("path", "/junit/test");

        get("/system/console/modelregistry/api/filter");

        verifyResponseHasContentType("application/json;charset=UTF-8");
        assertResponseIs("[\"io.neba.core.resourcemodels.registration.ModelRegistryConsolePluginTest$Model\"]");
    }

    @Test
    public void testFilterModelTypesByResourceAndModelType() throws Exception {
        withRegisteredModel("cq:Page", Model.class, 123L, "modelName");
        withPathResource("/junit/test");
        withPathResourceLookedUp();

        withParameter("path", "/junit/test");
        withParameter("modelTypeName", "io.neba.core.resourcemodels.registration.ModelRegistryConsolePluginTest$Model");

        get("/system/console/modelregistry/api/filter");

        verifyResponseHasContentType("application/json;charset=UTF-8");
        assertResponseIs("[\"io.neba.core.resourcemodels.registration.ModelRegistryConsolePluginTest$Model\"]");
    }

    @Test
    public void testUsageOfConfiguredServiceUser() throws Exception {
        withServiceUserAmendment();
        withIconResource("/apps/project/components/myComponent/icon.png");

        get("/system/console/modelregistry/api/componenticon/apps/project/components/myComponent");

        verifyServiceResourceResolverIsObtained();
        verifyPluginResolvesResource("/apps/project/components/myComponent/icon.png");
        verifyResponseHasContentType("image/png");
        verifyIconResourceIsAdaptedToInputStream();
    }

    @Test
    public void testInvalidServiceUserConfigurationIsHandledGraceful() throws Exception {
        withFailureWhenRetrievingAdminUser();
        withIconResource("/apps/project/components/myComponent/icon.png");

        get("/system/console/modelregistry/api/componenticon/apps/project/components/myComponent");

        verifyServiceResourceResolverIsObtained();
        verifyResponseHasContentType("image/png");
        verifyNoIconResourceIsResolved();
        verifyDefaultComponentIconIsWritten();
    }

    @Test
    public void testNotificationForMissingServiceUserConfiguration() throws IOException, LoginException {
        withFailureWhenRetrievingAdminUser();
        renderContent();
        assertResponseContains(
                "Warning: No amendment mapping for io.neba.neba-core:");
    }

    @Test
    public void testNoNotificationForMissingServiceUserConfigurationWhenConfigurationIsPresent() throws IOException, LoginException {
        withServiceUserAmendment();
        renderContent();
        assertResponseDoesNotContainContain("Warning: No amendment mapping for io.neba.neba-core:");
    }

    @Test
    public void testResourceTypesAreStillListedWhenPluginCannotAccessToRepository() throws IOException, LoginException {
        withFailureWhenRetrievingAdminUser();
        withRegisteredModel("cq:Page", Model.class, 123L, "modelName");
        renderContent();
        assertResponseContains("cq:Page");
    }

    private void assertResponseDoesNotContainContain(String notExpected) {
        assertThat(this.renderedResponse).doesNotContain(notExpected);
    }

    private void withFailureWhenRetrievingAdminUser() throws LoginException {
        doThrow(new LoginException("THIS IS AN EXPECTED TEST EXCEPTION")).when(this.factory).getResourceResolver(any());
    }

    private void verifyServiceResourceResolverIsObtained() throws LoginException {
        verify(this.factory).getServiceResourceResolver(any());
    }

    private void withServiceUserAmendment() throws LoginException {
        doReturn(this.resolver).when(this.factory).getServiceResourceResolver(any());
    }

    private void withPathResourceLookedUp() {
        Set<ResolvedModelSource<?>> resolvedModelSources = new HashSet<>();
        for (OsgiModelSource<?> source : this.modelSources) {
            ResolvedModelSource<?> result = mock(ResolvedModelSource.class);
            doReturn(source).when(result).getSource();
            resolvedModelSources.add(result);
        }
        doReturn(resolvedModelSources).when(this.modelRegistry).lookupAllModels(eq(this.pathResource));
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

    private void withComposumConsoleBundlePresent() {
        Bundle composumConsoleBundle = mock(Bundle.class);
        doReturn("com.composum.core.console").when(composumConsoleBundle).getSymbolicName();
        doReturn(new Bundle[]{composumConsoleBundle}).when(this.bundleContext).getBundles();
        this.testee.init();
    }

    private void withPathResource(String path) {
        doReturn(this.pathResource).when(this.resolver).getResource(eq(path));
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
        byte[] buffer = new byte[8192];
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
        assertThat(IOUtils.toString(this.resourceUrl.openStream(), StandardCharsets.UTF_8)).contains(resourceFragment);
    }

    private void withResolution(String resourceTypePath, String resourcePath) {
        Resource mock = mock(Resource.class);
        doReturn(mock).when(this.resolver).getResource(eq(resourceTypePath));
        doReturn(resourcePath).when(mock).getPath();
    }

    private void assertResponseContainsModel(String linkToResourceType, Class<Model> modelType, long bundleId, String modelName) {
        assertThat(this.renderedResponse).contains("<td>" + linkToResourceType + "</td><td>" + modelType.getName() +
                "</td><td>" + modelName + "</td><td><a href=\"bundles/" + bundleId +
                "\" title=\"JUnit test bundle 1.0.0\">" + bundleId + "</a></td>");
    }

    private void assertResponseContains(String expected) {
        assertThat(this.renderedResponse).contains(expected);
    }

    private void assertResponseContainsNumberOfModelsText(int numberOfTests) {
        assertThat(this.renderedResponse).contains(numberOfTests + " Model(s) registered.");
    }

    private void assertResponseIs(String expected) {
        assertThat(this.renderedResponse).isEqualTo(expected);
    }

    private void withRegisteredModel(String typeName, Class<Model> modelType, long bundleId, String modelName) {
        List<OsgiModelSource<?>> sources = new ArrayList<>();
        OsgiModelSource<?> source = mock(OsgiModelSource.class);
        doReturn(modelType).when(source).getModelType();
        doReturn(bundleId).when(source).getBundleId();
        doReturn(modelName).when(source).getModelName();
        doReturn(this.bundle).when(source).getBundle();
        sources.add(source);
        this.modelSources.add(source);
        this.typeMappings.put(typeName, sources);
    }

    private void assertResponseContainsTableHead() {
        assertThat(this.renderedResponse).contains("<th>Type</th>");
        assertThat(this.renderedResponse).contains("<th>Model type</th>");
        assertThat(this.renderedResponse).contains("<th>Model name</th>");
        assertThat(this.renderedResponse).contains("<th>Source bundle</th>");
    }

    private void renderContent() throws IOException {
        this.testee.renderContent(this.request, this.response);
        // Remove platform-dependent line endings.
        getResponseAsString();
    }

    private void getResponseAsString() {
        this.renderedResponse = this.internalWriter.toString().replaceAll("[\\n\\r]", "");
    }

    private void getResource(String resource) {
        String resourcePath = "/" + "modelregistry" + "/static/" + resource;
        this.resourceUrl = this.testee.getResource(resourcePath);
    }

    private void verifyIconResourceIsAdaptedToInputStream() {
        verify(this.iconResource).adaptTo(eq(InputStream.class));
    }

    private void verifyResponseHasContentType(String type) {
        verify(this.response).setContentType(type);
    }

    private void withRepositoryExceptionDuringNodeTypeManagerRetrieval() throws RepositoryException {
        doThrow(new RepositoryException("THIS IS AN EXPECTED TEST EXCEPTION"))
                .when(this.workspace)
                .getNodeTypeManager();
    }
}
