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


import io.neba.api.services.ResourceModelResolver;
import io.neba.core.resourcemodels.mapping.NestedMappingSupport;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.osgi.service.component.ComponentContext;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class JsonViewServletsTest {
    @Mock
    private ResourceModelResolver resourceModelResolver;
    @Mock
    private NestedMappingSupport nestedMappingSupport;
    @Mock
    private Resource resource;
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

        doReturn(this.requestPathInfo).when(this.request).getRequestPathInfo();
        doReturn(this.resource).when(this.request).getResource();
        doAnswer(inv -> this.selectors).when(this.requestPathInfo).getSelectors();
        doReturn("/some/resource/path").when(resource).getPath();

        doAnswer((inv) -> this.writer).when(this.response).getWriter();

        doReturn("UTF-8").when(this.configuration).encoding();
        doReturn(new String[]{"SerializationFeature.WRITE_DATES_AS_TIMESTAMPS=true"})
                .when(this.configuration).jacksonSettings();

        this.testee.activate(this.context, this.configuration);
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
