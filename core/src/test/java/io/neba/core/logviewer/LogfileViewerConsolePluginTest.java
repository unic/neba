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

package io.neba.core.logviewer;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.util.thread.ThreadPool;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.osgi.framework.BundleContext;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URL;
import java.util.Collection;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static io.neba.core.util.ZipFileUtil.toZipFileEntryName;
import static org.apache.commons.io.FileUtils.listFiles;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.*;

/**
 * @author Olaf Otto
 */
@RunWith(MockitoJUnitRunner.class)
public class LogfileViewerConsolePluginTest {
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private BundleContext bundleContext;
    @Mock
    private ServletOutputStream outputStream;
    @Mock
    private ServletConfig config;
    @Mock
    private ContextHandler.Context context;
    @Mock
    private ContextHandler contextHandler;
    @Mock
    private Server server;
    @Mock
    private LogFiles logFiles;
    @Mock
    private TailServlet tailServlet;

    private File testLogfileDirectory;
    private StringWriter internalWriter;
    private String htmlResponse;
    private ByteArrayOutputStream internalOutputStream;
    private ZipInputStream zippedFiles;
    private Collection<File> availableLogFiles;

    @InjectMocks
    private LogfileViewerConsolePlugin testee;

    @Before
    public void setUp() throws Exception {
        this.internalWriter = new StringWriter();
        this.internalOutputStream = new ByteArrayOutputStream(8192);
        PrintWriter writer = new PrintWriter(this.internalWriter);

        URL testLogfileUrl = getClass().getResource("/io/neba/core/logviewer/testlogfiles/");
        this.testLogfileDirectory = new File(testLogfileUrl.getFile());
        this.availableLogFiles = listFiles(this.testLogfileDirectory, null, true);

        when(this.context.getContextHandler()).thenReturn(contextHandler);
        when(this.contextHandler.getServer()).thenReturn(this.server);
        when(this.server.getThreadPool()).thenReturn(mock(ThreadPool.class));

        when(this.request.getServletPath()).thenReturn("/system/console");
        when(this.request.getServerName()).thenReturn("servername");
        when(this.request.getMethod()).thenReturn("GET");
        when(this.request.getProtocol()).thenReturn("HTTP");
        when(this.response.getWriter()).thenReturn(writer);
        when(this.response.getOutputStream()).thenReturn(this.outputStream);

        Answer<Object> writeIntToByteArrayOutputStream = invocation -> {
            internalOutputStream.write((Integer) invocation.getArguments()[0]);
            return null;
        };
        Answer<Object> writeBytesToByteArrayOutputStream = invocation -> {
            byte[] b = (byte[]) invocation.getArguments()[0];
            int off = (Integer) invocation.getArguments()[1];
            int len = (Integer) invocation.getArguments()[2];
            internalOutputStream.write(b, off, len);
            return null;
        };
        doAnswer(writeIntToByteArrayOutputStream).when(this.outputStream).write(anyInt());
        doAnswer(writeBytesToByteArrayOutputStream).when(this.outputStream).write(isA(byte[].class), anyInt(), anyInt());

        when(this.config.getServletContext()).thenReturn(this.context);

        when(this.logFiles.resolveLogFiles()).thenReturn(availableLogFiles);

        this.testee.init(config);
    }

    @Test
    public void testGetResources() throws Exception {
        URL resource = this.testee.getResource("/logviewer/static/testresource.txt");
        assertThat(resource).isNotNull();
    }

    @Test
    public void testRenderContentContainsDropdownValuesForTestLogfiles() throws Exception {
        renderContent();
        assertHtmlResponseContains("value=\"" + pathOf("logs/error.log") + "\"");
        assertHtmlResponseContains("value=\"" + pathOf("logs/error.log.1") + "\"");
        assertHtmlResponseContains("value=\"" + pathOf("logs/error.log.2020-01-01") + "\"");
        assertHtmlResponseContains("value=\"" + pathOf("logs/crx/error.log") + "\"");
        assertHtmlResponseContains("value=\"" + pathOf("logs/crx/error.log.0") + "\"");
        assertHtmlResponseContains("value=\"" + pathOf("logs/crx/error.log.2020-11-23") + "\"");
    }

    @Test
    public void testDownloadLogFilesAsZip() throws Exception {
        getZippedLogfiles();
        verifyLogFilesAreSendAs("logfiles-servername.zip");
        for (File file  : this.availableLogFiles) {
            assertNextZipEntryIs(toZipFileEntryName(file));
        }
    }

    @Test
    public void testDestroy() throws Exception {
        destroy();
        verifyTailServletIsDestroyed();
    }

    @Test(expected = ServletException.class)
    public void testRuntimeExceptionsDuringTailServletInitializationAreConvertedToServletException() throws Exception {
        doThrow(new RuntimeException("THIS IS AN EXPECTED TEST EXCEPTION")).when(this.tailServlet).init(any());
        this.testee.init(this.config);
    }

    @Test
    public void testDelegationOfTailRequestsToTailServlet() throws Exception {
        withRequestPath("/system/console/logviewer/tail");
        doGet();
        verifyRequestIsDelegatedToTailServlet();
    }

    private void verifyRequestIsDelegatedToTailServlet() throws ServletException, IOException {
        verify(this.tailServlet).service(this.request, this.response);
    }

    private void verifyTailServletIsDestroyed() {
        verify(this.tailServlet).destroy();
    }

    private void destroy() {
        this.testee.destroy();
    }

    private void verifyLogFilesAreSendAs(String filename) {
        verify(this.response).setHeader(eq("Content-Disposition"), eq("attachment;filename=" + filename));
    }

    private void assertNextZipEntryIs(String expected) throws IOException {
        ZipEntry nextEntry = this.zippedFiles.getNextEntry();
        assertThat(nextEntry).isNotNull();
        assertThat(nextEntry.getName()).isEqualTo(expected);
    }

    private void getZippedLogfiles() throws ServletException, IOException {
        withRequestPath("/system/console/logviewer/download");
        doGet();
        this.zippedFiles = new ZipInputStream(new ByteArrayInputStream(this.internalOutputStream.toByteArray()));
    }

    private void doGet() throws ServletException, IOException {
        this.testee.doGet(this.request, this.response);
        this.htmlResponse = this.internalWriter.toString();
    }

    private void withRequestPath(String requestPath) {
        when(this.request.getRequestURI()).thenReturn(requestPath);
        when(this.request.getPathInfo()).thenReturn(requestPath);
    }

    private void assertHtmlResponseContains(String expected) {
        assertThat(this.htmlResponse).contains(expected);
    }

    private void renderContent() throws ServletException, IOException {
        this.testee.renderContent(this.request, this.response);
        this.htmlResponse = this.internalWriter.getBuffer().toString();
    }

    private String pathOf(String relativePath) {
        return new File(this.testLogfileDirectory, relativePath).getAbsolutePath();
    }
}