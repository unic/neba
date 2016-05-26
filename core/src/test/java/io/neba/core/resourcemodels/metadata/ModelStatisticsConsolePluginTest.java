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

package io.neba.core.resourcemodels.metadata;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * @author Olaf Otto
 */
@RunWith(MockitoJUnitRunner.class)
public class ModelStatisticsConsolePluginTest {
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private ResourceModelMetaDataRegistrar registrar;

    private List<ResourceModelMetaData> metadata;

    private URL resourceUrl;
    private Writer internalWriter;
    private String renderedResponse;

    @InjectMocks
    private ModelStatisticsConsolePlugin testee;

    @Before
    public void setUp() throws Exception {
        this.metadata = new ArrayList<>();
        this.internalWriter = new StringWriter();
        Writer writer = new PrintWriter(this.internalWriter);
        doReturn(writer).when(this.response).getWriter();
        doReturn(this.metadata).when(this.registrar).get();
    }

    @Test
    public void testRetrievalOfStaticJavascript() throws Exception {
        getResource("script.js");
        assertResourceContains("function");
    }

    @Test
    public void testHtmlRendering() throws Exception {
        withRequestPath("/system/console/modelstatistics");
        doGet();

        assertResponseContains("<button type=\"button\" id=\"resetStatistics\"");
        assertResponseContains("<button type=\"button\" id=\"helpWithExpressions\"");
        assertResponseContains("<div id=\"plotarea\">");
        assertResponseContains("<input type=\"text\" id=\"filter\" data-default-value=\"Begin typing to create a filter expression\" />");
        assertResponseContains("<div id=\"target\"></div>");
    }

    @Test
    public void testRetrievalOfAllStatistics() throws Exception {
        addStatistics("junit.test.type.NameOne", 123456L, 100L, 5, 0, 1000, 10, 20, new int[]{1, 2, 4, 8, 16}, new int[]{10, 20, 4, 1, 0});
        addStatistics("junit.test.type.NameTwo", 234567L, 200L, 10, 1, 1000, 20, 40, new int[]{2, 4, 8, 16, 23}, new int[]{20, 40, 8, 2, 0});
        withRequestPath("/system/console/modelstatistics/api/statistics");
        doGet();
        assertResponseIsEqualTo("[" +
                                    "{" +
                                    "\"type\":\"junit.test.type.NameOne\"," +
                                    "\"since\":123456," +
                                    "\"mappableFields\":0," +
                                    "\"lazyFields\":0," +
                                    "\"greedyFields\":0," +
                                    "\"instantiations\":0," +
                                    "\"mappings\":100," +
                                    "\"averageMappingDuration\":10," +
                                    "\"totalMappingDuration\":1000," +
                                    "\"maximumMappingDuration\":20," +
                                    "\"minimumMappingDuration\":0," +
                                    "\"mappingDurationMedian\":5," +
                                    "\"cacheHits\":0" +
                                    "}," +

                                    "{" +
                                    "\"type\":\"junit.test.type.NameTwo\"," +
                                    "\"since\":234567," +
                                    "\"mappableFields\":0," +
                                    "\"lazyFields\":0," +
                                    "\"greedyFields\":0," +
                                    "\"instantiations\":0," +
                                    "\"mappings\":200," +
                                    "\"averageMappingDuration\":20," +
                                    "\"totalMappingDuration\":1000," +
                                    "\"maximumMappingDuration\":40," +
                                    "\"minimumMappingDuration\":1," +
                                    "\"mappingDurationMedian\":10," +
                                    "\"cacheHits\":0" +
                                    "}" +
                                "]");
    }

    @Test
    public void testRetrievalOfStatisticsForSpecificType() throws Exception {
        addStatistics("junit.test.type.NameOne", 123456L, 100L, 5, 0, 1000, 10, 20, new int[]{1, 2, 4, 8, 16}, new int[]{10, 20, 4, 1, 0});
        withRequestPath("/system/console/modelstatistics/api/statistics/junit.test.type.NameOne");
        doGet();
        assertResponseIsEqualTo("{" +
                        "\"type\":\"junit.test.type.NameOne\"," +
                        "\"since\":123456," +
                        "\"mappableFields\":0," +
                        "\"lazyFields\":0," +
                        "\"greedyFields\":0," +
                        "\"instantiations\":0," +
                        "\"mappings\":100," +
                        "\"averageMappingDuration\":10," +
                        "\"totalMappingDuration\":1000," +
                        "\"maximumMappingDuration\":20," +
                        "\"minimumMappingDuration\":0," +
                        "\"mappingDurationMedian\":5," +
                        "\"cacheHits\":0," +
                        "\"mappingDurationFrequencies\":{" +
                            "\"[0, 1)\":10," +
                             "\"[1, 2)\":20," +
                             "\"[2, 4)\":4," +
                             "\"[4, 8)\":1," +
                             "\"[8, 16)\":0" +
                         "}" +
                       "}");
    }

    @Test
    public void testResetOfStatistics() throws Exception {
        addStatistics("junit.test.type.NameOne", 1, 1L, 1, 1, 1, 1, 1, new int[]{}, new int[]{});
        addStatistics("junit.test.type.NameTwo", 1, 1L, 1, 1, 1, 1, 1, new int[]{}, new int[]{});

        withRequestPath("/system/console/modelstatistics/api/reset");
        doGet();

        assertResponseIsEqualTo("{\"success\": true}");

        assertStatisticsAreReset();
    }

    private void assertResponseContains(String responseFragment) {
        assertThat(this.renderedResponse).contains(responseFragment);
    }

    private void assertResponseIsEqualTo(String response) {
        assertThat(this.renderedResponse).isEqualTo(response);
    }

    private void addStatistics(String typeName,
                               long since,
                               long mappings,
                               double mappingDurationMedian,
                               double minimumMappingDuration,
                               double totalMappingDuration,
                               double averageMappingDuration,
                               double maximumMappingDuration,
                               int[] mappingDurationIntervalBoundaries,
                               int[] mappingDurationFrequencies) {

        ResourceModelMetaData metaData = mock(ResourceModelMetaData.class);
        ResourceModelStatistics statistics = mock(ResourceModelStatistics.class);
        MappedFieldMetaData[] mappableFields = new MappedFieldMetaData[0];

        doReturn(mappingDurationMedian).when(statistics).getMappingDurationMedian();
        doReturn(minimumMappingDuration).when(statistics).getMinimumMappingDuration();
        doReturn(averageMappingDuration).when(statistics).getAverageMappingDuration();
        doReturn(totalMappingDuration).when(statistics).getTotalMappingDuration();
        doReturn(maximumMappingDuration).when(statistics).getMaximumMappingDuration();
        doReturn(mappableFields).when(metaData).getMappableFields();
        doReturn(statistics).when(metaData).getStatistics();
        doReturn(typeName).when(metaData).getTypeName();
        doReturn(since).when(statistics).getSince();
        doReturn(mappings).when(statistics).getNumberOfMappings();
        doReturn(mappingDurationIntervalBoundaries).when(statistics).getMappingDurationIntervalBoundaries();
        doReturn(mappingDurationFrequencies).when(statistics).getMappingDurationFrequencies();

        this.metadata.add(metaData);
    }

    private void withRequestPath(String requestPath) {
        when(this.request.getServletPath()).thenReturn("/system/console");
        when(this.request.getRequestURI()).thenReturn(requestPath);
        when(this.request.getPathInfo()).thenReturn(requestPath);
    }

    private void assertResourceContains(String resourceFragment) throws IOException {
        assertThat(this.resourceUrl).isNotNull();
        assertThat(IOUtils.toString(this.resourceUrl.openStream())).contains(resourceFragment);
    }

    private void assertStatisticsAreReset() {
        for (ResourceModelMetaData metaData : this.metadata) {
            verify(metaData.getStatistics()).reset();
        }
    }

    private void doGet() throws ServletException, IOException {
        this.testee.doGet(this.request, this.response);
        // Remove platform-dependent line endings.
        this.renderedResponse = this.internalWriter.toString().replaceAll("[\\n\\r]", "");
    }

    private void getResource(String resource) {
        String resourcePath = "/" + ModelStatisticsConsolePlugin.LABEL + "/static/" + resource;
        this.resourceUrl = this.testee.getResource(resourcePath);
    }
}
