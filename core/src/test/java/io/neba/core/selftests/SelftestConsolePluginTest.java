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

package io.neba.core.selftests;

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
import java.util.ArrayList;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Olaf Otto
 */
@RunWith(MockitoJUnitRunner.class)
public class SelftestConsolePluginTest {
    @Mock
    private SelftestRegistrar registrar;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;

    private List<SelftestReference> selftests = new ArrayList<SelftestReference>();
    private SelftestReference reference;
    private StringWriter stringWriter;

    @InjectMocks
    private SelftestConsolePlugin testee;

    @Before
    public void prepareConsolePlugin() throws IOException {
        when(this.registrar.getSelftestReferences()).thenReturn(this.selftests);
        when(this.request.getServletPath()).thenReturn("/system/console");
        this.stringWriter = new StringWriter();
        when(this.response.getWriter()).thenReturn(new PrintWriter(this.stringWriter));
    }

    @Test
    public void testSuccessfulSelftestExecution() throws Exception {
        withSelftestReference("/testid");
        withRequestPath("/run/testid");
        callPlugin();
        verifySelftestReferenceIsExecuted();
        assertPluginResponseIs("{\"failed\":false,\"trace\":null,\"errorMsg\":\"failure\",\"successMsg\":\"success\"}");
    }

    @Test
    public void testFailedSelftestExecution() throws Exception {
        withSelftestReference("/testid");
        withFailingSelftest();
        withRequestPath("/run/testid");
        callPlugin();
        verifySelftestReferenceIsExecuted();
        assertPluginResponseContains("{\"failed\":true,\"trace\":\"java.lang.RuntimeException: JUNIT TEST");
    }

    @Test
    public void testRenderingOfRegisteredSelfTestAsHtmlTable() throws Exception {
        withSelftestReference("/testid");
        withTestDescription("Test description");
        withRequestPath("/");
        callPlugin();
        assertPluginResponseContains("<table id=\"plugin_table\" class=\"nicetable tablesorter noauto\">" +
                                        "<thead><tr><th>Name</th><th>Source bundle</th><th>Action</th><th>Status</th></tr></thead>" +
                                        "<tbody>" +
                                        "<tr id=\"row0\">" +
                                            "<td>Test description</td><td><a href=\"bundles/0\">0</a></td>" +
                                            "<td><a href=\"#\" class=\"runlink\" onclick=\"run('/testid', this, 0);return false;\">run</a></td>" +
                                            "<td><div id=\"signal0\" class=\"signal result0\" " +
                                                      "style=\"width:14px;height:14px;background-color:gray;margin:2px 0 0 0;\">" +
                                                 "</div>" +
                                            "</td>" +
                                        "</tr>" +
                                        "</tbody>" +
                                      "</table>");
    }

    @Test
    public void testRenderingOfSuccessfulTestResultsAsXML() throws Exception {
        withSelftestReference("/testid");
        withTestDescription("Test description");
        withRequestPath("/run.xml");
        callPlugin();

        assertPluginResponseIs("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                                "<tests>" +
                                    "<test id=\"/testid\" " +
                                          "description=\"Test description\" " +
                                          "successMessage=\"success\" " +
                                          "failureMessage=\"failure\" " +
                                          "failed=\"false\">" +
                                    "</test>" +
                                 "</tests>");
    }

    @Test
    public void testRenderingOfFailedTestResultsAsXML() throws Exception {
        withSelftestReference("/testid");
        withTestDescription("Test description");
        withFailingSelftest("JUnit test failed and message contains a <[CDATA[[]]> section");
        withRequestPath("/run.xml");
        callPlugin();

        assertPluginResponseStartsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                                        "<tests>" +
                                            "<test id=\"/testid\" " +
                                                  "description=\"Test description\" " +
                                                  "successMessage=\"success\" " +
                                                  "failureMessage=\"failure\" " +
                                                  "failed=\"true\">" +
                                              "<![CDATA[java.lang.RuntimeException: " +
                                                "JUnit test failed and message contains a " +
                                                "<[CDATA[[]]]]><![CDATA[> section");
        assertPluginResponseEndsWith("]]></test></tests>");
    }

    private void assertPluginResponseStartsWith(String s) {
        assertThat(this.stringWriter.toString()).startsWith(s);
    }

    private void assertPluginResponseEndsWith(String s) {
        assertThat(this.stringWriter.toString()).endsWith(s);
    }

    private void withFailingSelftest(String message) {
        doThrow(new RuntimeException(message)).when(this.reference).execute();
    }

    private String withTestDescription(String description) {
        return doReturn(description).when(this.reference).getDescription();
    }

    private void withFailingSelftest() {
        doThrow(new RuntimeException("JUNIT TEST")).when(this.reference).execute();
    }

    private void assertPluginResponseContains(String string) {
        assertThat(this.stringWriter.toString()).contains(string);
    }

    private void assertPluginResponseIs(String string) {
        assertThat(this.stringWriter.toString()).isEqualTo(string);
    }

    private void verifySelftestReferenceIsExecuted() {
        verify(this.reference, times(1)).execute();
    }

    private void withSelftestReference(String string) {
        this.reference = mock(SelftestReference.class);
        when(reference.getId()).thenReturn(string);
        when(reference.getDescription()).thenReturn("description");
        when(reference.getSuccess()).thenReturn("success");
        when(reference.getFailure()).thenReturn("failure");
        this.selftests.add(reference);
    }

    private void callPlugin() throws ServletException, IOException {
        this.testee.doGet(this.request, this.response);
    }

    private void withRequestPath(String path) {
        String requestPath = "/system/console/" + SelftestConsolePlugin.LABEL + path;
        when(this.request.getRequestURI()).thenReturn(requestPath);
        when(this.request.getPathInfo()).thenReturn(requestPath);
    }
}
