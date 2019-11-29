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
package io.neba.core.resourcemodels.views.json;


import com.fasterxml.jackson.core.JsonGenerator;
import io.neba.api.services.ResourceModelResolver;
import io.neba.core.resourcemodels.mapping.NestedMappingSupport;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.osgi.framework.Bundle;
import org.osgi.service.component.ComponentContext;

import javax.servlet.Servlet;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.util.Enumeration;
import java.util.Optional;

import static java.util.Arrays.stream;
import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static javax.servlet.http.HttpServletResponse.SC_NOT_MODIFIED;
import static javax.servlet.http.HttpServletResponse.SC_SERVICE_UNAVAILABLE;
import static org.apache.commons.io.IOUtils.toByteArray;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class JsonViewServletsTest {
    private static final long RESOURCE_MODIFICATION_TIMESTAMP = 123L;
    private static final String IF_NONE_MATCH = "If-None-Match";

    @Mock
    private ResourceModelResolver resourceModelResolver;
    @Mock
    private NestedMappingSupport nestedMappingSupport;
    @Mock
    private Resource resource;
    @Mock
    private ResourceMetadata resourceMetadata;
    @Mock
    private ComponentContext context;
    @Mock
    private JsonViewServlets.Configuration configuration;
    @Mock
    private SlingHttpServletRequest request;
    @Mock
    private SlingHttpServletResponse response;
    @Mock
    private RequestPathInfo requestPathInfo;
    @Mock
    private Bundle bundle;

    private PrintWriter writer;
    private StringWriter out;

    private String[] selectors;

    @InjectMocks
    private JsonViewServlets testee;

    @Before
    public void setUp() throws IOException {
        TestModel testModel = new TestModel();

        withSelectors("model");
        doReturn(testModel).when(this.resourceModelResolver).resolveMostSpecificModel(this.resource);
        String modelName = "modelName";
        doReturn(testModel).when(this.resourceModelResolver).resolveMostSpecificModelWithName(this.resource, modelName);

        doReturn("GET").when(request).getMethod();
        doReturn(this.requestPathInfo).when(this.request).getRequestPathInfo();
        doReturn(this.resource).when(this.request).getResource();
        doReturn(this.resourceMetadata).when(this.resource).getResourceMetadata();
        doReturn(RESOURCE_MODIFICATION_TIMESTAMP).when(this.resourceMetadata).getModificationTime();

        doReturn(mock(Enumeration.class)).when(this.request).getHeaders(IF_NONE_MATCH);

        doAnswer(inv -> this.selectors).when(this.requestPathInfo).getSelectors();
        doReturn("/some/resource/path").when(resource).getPath();

        doAnswer((inv) -> this.writer).when(this.response).getWriter();

        doReturn(this.bundle).when(this.context).getUsingBundle();

        doReturn("UTF-8").when(this.configuration).encoding();
        doReturn(new String[]{"SerializationFeature.WRITE_DATES_AS_TIMESTAMPS=true"})
                .when(this.configuration).jacksonSettings();

        this.testee.activate(this.context, this.configuration);
    }

    @Test
    public void testMissingJacksonLibraryOnClasspathYieldsServiceUnavailable() throws Exception {
        ClassLoader classLoaderWithoutDecoratedObjectFactory = new ClassLoader(getClass().getClassLoader()) {
            @Override
            public Class<?> loadClass(String name) throws ClassNotFoundException {
                if (JsonGenerator.class.getName().equals(name)) {
                    // This optional dependency is not present on the class path in this test scenario.
                    throw new ClassNotFoundException("THIS IS AN EXPECTED TEST EXCEPTION. The presence of " + JsonGenerator.class.getName() + " is optional.");
                }
                if (JsonViewServlets.class.getName().equals(name)) {
                    // Define the test subject's class class in this class loader, thus its dependencies -
                    // such as the DecoratedObjectFactory - are also loaded via this class loader.
                    try {
                        byte[] classFileData = toByteArray(getResourceAsStream(name.replace('.', '/').concat(".class")));
                        return defineClass(name, classFileData, 0, classFileData.length);
                    } catch (IOException e) {
                        throw new ClassNotFoundException("Unable to load " + name + ".", e);
                    }
                }

                return super.loadClass(name);
            }
        };

        @SuppressWarnings("unchecked")
        Class<? extends Servlet> cls = (Class<? extends Servlet>) classLoaderWithoutDecoratedObjectFactory.loadClass(JsonViewServlets.class.getName());
        Optional<Class<?>> configurationClass = stream(cls.getDeclaredClasses()).filter(c -> c.getName().equals(JsonViewServlets.Configuration.class.getName())).findFirst();
        if (!configurationClass.isPresent()) {
            fail("Unable to find " + JsonViewServlets.Configuration.class.getName() + " in class " + cls);
        }
        Servlet jsonViewServlets = cls.newInstance();
        Object configuration = mock(configurationClass.get());

        Method activate = cls.getDeclaredMethod("activate", ComponentContext.class, configurationClass.get());
        activate.setAccessible(true);
        activate.invoke(jsonViewServlets, this.context, configuration);

        jsonViewServlets.service(this.request, this.response);
        verify(this.response).sendError(SC_SERVICE_UNAVAILABLE, "The JSON view service is not available.");
    }

    @Test
    public void testHandlingOfBadResourceModelName() throws IOException {
        withSelectors("model", "<script>alert('bad name')</script>");
        serveRequest();
        verify(this.response).sendError(SC_BAD_REQUEST, "Invalid model name. The model name must match the pattern [A-z0-9_\\-#]+");
    }

    @Test
    public void testHandlingOfMissingModel() throws IOException {
        withMissingModel();
        serveRequest();
        verify(this.response).sendError(SC_NOT_FOUND, "No model could be resolved for resource /some/resource/path");
    }

    @Test
    public void testHandlingOfMissingNamedModel() throws IOException {
        withMissingModel();
        withSelectors("model", "modelName");
        serveRequest();
        verify(this.response).sendError(SC_NOT_FOUND, "No model with name modelName could be resolved for resource /some/resource/path");
    }

    @Test
    public void testLookupModelByName() throws IOException {
        withSelectors("model", "modelName");
        serveRequest();
        verifyServletAttemptsResolveModelWithName("modelName");
    }

    @Test
    public void testJsonRenderingWithoutTypeAttribute() throws IOException {
        serveRequest();
        assertCharacterEncodingIs("UTF-8");
        assertContentTypeIs("application/json");
        assertJsonIs("{\"test\":\"Test value\"}");
    }

    @Test
    public void testBeginAndEndRecordMappingsHappensBeforeModelResolutionAndAfterJsonSerialization() throws IOException {
        InOrder inOrder = inOrder(this.nestedMappingSupport, this.resourceModelResolver, this.response);

        serveRequest();

        inOrder.verify(nestedMappingSupport).beginRecordingMappings();
        inOrder.verify(resourceModelResolver).resolveMostSpecificModel(this.resource);
        inOrder.verify(this.response).getWriter();
        inOrder.verify(this.nestedMappingSupport).endRecordingMappings();
    }

    @Test
    public void testMappingRecordingIsAlwaysEnded() throws IOException {
        withExceptionDuringResponseAccess();
        Exception expectedException = null;
        try {
            serveRequest();
        } catch (Exception e) {
            expectedException = e;
        }

        assertThat(expectedException).describedAs("Expected error during response access").isNotNull();
        verify(this.nestedMappingSupport).endRecordingMappings();
    }

    @Test
    public void testHandlingOfInvalidSelectorFormat() throws IOException {
        withSelectors("model", "modelName", "nonsense");
        serveRequest();
        verify(this.response).sendError(SC_BAD_REQUEST, "Invalid selectors. The expected format is <json servlet selector>[.<optional model name>]");
    }

    @Test
    public void testEtagsAreNeitherGeneratedNotTestedWhenEtagsAreDisabled() throws IOException {
        withEtagsDisabled();
        withValidEtagInRequest();

        serveRequest();

        verifyNoEtagInResponse();
        verifyOriginalResponseStatusIsKept();
    }

    @Test
    public void testValidEtagInRequestWithEtagGenerationEnabledYieldsNotModifiedStatus() throws IOException {
        withEtagsEnabled();
        withValidEtagInRequest();

        serveRequest();

        verifyResponseStatusIsChangedToNotModified();
        verifyNoEtagInResponse();
    }

    @Test
    public void testEtagsAreAddedToResponseWhenEtagGenerationIsEnabled() throws IOException {
        withEtagsEnabled();

        serveRequest();

        verifyEtagIsAddedToResponse();
    }

    @Test
    public void testCacheControlHeaderIsAddedFromConfiguration() throws IOException {
        withConfiguredCacheControlHeader("private, max-age=0");

        serveRequest();

        verifyCacheControlHeaderInResponseIs("private, max-age=0");
    }

    private void verifyCacheControlHeaderInResponseIs(String value) {
        verify(this.response).setHeader("Cache-Control", value);
    }

    private void withConfiguredCacheControlHeader(String value) {
        doReturn(value).when(this.configuration).cacheControlHeader();
    }

    private void verifyResponseStatusIsChangedToNotModified() {
        verify(this.response).setStatus(SC_NOT_MODIFIED);
    }

    private void verifyEtagIsAddedToResponse() {
        verify(this.response).setHeader("Etag", getExpectedEtag());
    }

    private void withEtagsEnabled() {
        doReturn(true).when(this.configuration).generateEtag();
    }

    private void verifyOriginalResponseStatusIsKept() {
        verify(this.response, never()).setStatus(anyInt());
    }

    private void withValidEtagInRequest() {
        Enumeration etagHeaderValues = mock(Enumeration.class);
        doReturn(true, true, false).when(etagHeaderValues).hasMoreElements();
        doReturn(getExpectedEtag()).when(etagHeaderValues).nextElement();
        doReturn(etagHeaderValues).when(this.request).getHeaders(IF_NONE_MATCH);
    }

    private String getExpectedEtag() {
        return "W/\"" + RESOURCE_MODIFICATION_TIMESTAMP + '-' + resource.getPath() + "\"";
    }

    private void verifyNoEtagInResponse() {
        verify(this.response, never()).addHeader(eq("Etag"), any());
    }

    private void withEtagsDisabled() {
        doReturn(false).when(this.configuration).generateEtag();
    }

    private void withExceptionDuringResponseAccess() throws IOException {
        doThrow(new RuntimeException("THIS IS AN EXPECTED TEST EXCEPTION")).when(this.response).getWriter();
    }

    private void verifyServletAttemptsResolveModelWithName(String modelName) {
        verify(this.resourceModelResolver).resolveMostSpecificModelWithName(this.resource, modelName);
    }

    private void withMissingModel() {
        doReturn(null).when(this.resourceModelResolver).resolveMostSpecificModel(any());
        doReturn(null).when(this.resourceModelResolver).resolveMostSpecificModelWithName(any(), any());
    }

    private void withSelectors(String... selectors) {
        this.selectors = selectors;
    }

    private void assertContentTypeIs(String type) {
        verify(this.response).setContentType(type);
    }

    private void assertCharacterEncodingIs(String charset) {
        verify(this.response).setCharacterEncoding(charset);
    }

    private void assertJsonIs(String expected) {
        assertThat(this.out.toString()).isEqualTo(expected);
    }

    private void serveRequest() throws IOException {
        this.out = new StringWriter();
        this.writer = new PrintWriter(this.out);

        this.testee.doGet(this.request, this.response);
    }

    private static class TestModel {
        public String getTest() {
            return "Test value";
        }
    }
}
