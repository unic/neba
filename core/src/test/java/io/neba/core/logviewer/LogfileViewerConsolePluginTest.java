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

package io.neba.core.logviewer;

import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.util.DecoratedObjectFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Collection;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static io.neba.core.util.ReflectionUtil.findField;
import static io.neba.core.util.ZipFileUtil.toZipFileEntryName;
import static org.apache.commons.io.FileUtils.listFiles;
import static org.apache.commons.io.IOUtils.toByteArray;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    private ServletOutputStream outputStream;
    @Mock
    private ServletConfig config;
    @Mock
    private ContextHandler.Context context;
    @Mock
    private LogFiles logFiles;
    @Mock
    private TailServlet tailServlet;

    private File testLogfileDirectory;
    private StringWriter internalWriter;
    private String responseText;
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

        when(this.request.getServletPath())
                .thenReturn("/system/console");

        when(this.request.getServerName())
                .thenReturn("servername");

        when(this.response.getWriter())
                .thenReturn(writer);

        when(this.response.getOutputStream())
                .thenReturn(this.outputStream);

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

        doAnswer(writeIntToByteArrayOutputStream)
                .when(this.outputStream)
                .write(anyInt());

        doAnswer(writeBytesToByteArrayOutputStream)
                .when(this.outputStream)
                .write(isA(byte[].class), anyInt(), anyInt());

        when(this.config.getServletContext()).thenReturn(this.context);

        when(this.logFiles.resolveLogFiles()).thenReturn(availableLogFiles);
    }

    @Test
    public void testGetResources() {
        URL resource = this.testee.getResource("/logviewer/static/testresource.txt");
        assertThat(resource).isNotNull();
    }

    @Test
    public void testRenderContentContainsDropdownValuesForTestLogfiles() throws Exception {
        renderContent();
        assertResponseContains("value=\"" + pathOf("logs/error.log") + "\"");
        assertResponseContains("value=\"" + pathOf("logs/error.log.1") + "\"");
        assertResponseContains("value=\"" + pathOf("logs/error.log.2020-01-01") + "\"");
        assertResponseContains("value=\"" + pathOf("logs/crx/error.log") + "\"");
        assertResponseContains("value=\"" + pathOf("logs/crx/error.log.0") + "\"");
        assertResponseContains("value=\"" + pathOf("logs/crx/error.log.2020-11-23") + "\"");
    }

    @Test
    public void testRenderContentsContainsDropdownWithSelectedOptionWhenFileParameterIsPresent() throws IOException {
        withFileRequestParameter(pathOf("logs/error.log"));
        renderContent();
        assertResponseContains("value=\"" + pathOf("logs/error.log") + "\" selected");
    }

    @Test
    public void testDownloadAllLogFilesAsZip() throws Exception {
        getAllLogfilesAsZip();
        verifyLogFilesAreSendAs("logfiles-servername.zip");
        for (File file : this.availableLogFiles) {
            assertNextZipEntryIs(toZipFileEntryName(file));
        }
    }

    @Test
    public void testDownloadCurrentLogfileAsZip() throws Exception {
        getLogfileAsZip(pathOf("logs/error.log"));
        verifyLogFilesAreSendAs("logfiles-servername-error.log.zip");
        assertZipResponseHasOneFile();
        assertNextZipEntryIs(toZipFileEntryName(toFile("logs/error.log")));
    }

    @Test
    public void testDestroy() {
        destroy();
        verifyTailServletIsDestroyed();
    }

    @Test(expected = ServletException.class)
    public void testRuntimeExceptionsDuringTailServletInitializationAreConvertedToServletException() throws Exception {
        doThrow(new RuntimeException("THIS IS AN EXPECTED TEST EXCEPTION")).when(this.tailServlet).init(any());
        init();
    }

    @Test
    public void testDelegationOfTailRequestsToTailServlet() throws Exception {
        withRequestPath("/system/console/logviewer/tail");
        doGet();
        verifyRequestIsDelegatedToTailServlet();
    }

    @Test
    public void testLogViewerProvidesDecoratedObjectFactoryAsServletContextAttribute() throws Exception {
        init();
        verifyDecoratedObjectInstanceIsInjectedIntoServletContext();

        destroy();
        verifyDecoratorObjectFactoryIsRemovedFromServletContext();
    }

    @Test
    public void testLogViewerDoesNotOverrideExistingDecoratorObjectFactory() throws Exception {
        withExistingDecoratorObjectFactory();

        init();
        verifyDecoratorObjectFactoryIsNotInjectedIntoServletContext();

        destroy();
        verifyDecoratorObjectFactoryIsNotRemovedFromServletContext();
    }

    @Test
    public void testServerTimeRetrieval() throws ServletException, IOException {
        withRequestPath("/system/console/logviewer/serverTime");
        doGet();
        assertResponseMatches("\\{\"time\": \"[0-9]{1,2}.[0-9]{1,2}.[0-9]{4} [0-9]{1,2}:[0-9]{1,2}:[0-9]{1,2}.[0-9]+ \\(UTC \\+ [0-9]+\\)\"\\}");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testLogViewerToleratesMissingDecoratedObjectFactoryFactory() throws Exception {
        ClassLoader classLoaderWithoutDecoratedObjectFactory = new ClassLoader(getClass().getClassLoader()) {
            @Override
            public Class<?> loadClass(String name) throws ClassNotFoundException {
                if (DecoratedObjectFactory.class.getName().equals(name)) {
                    // This optional dependency is not present on the class path in this test scenario.
                    throw new ClassNotFoundException("THIS IS AN EXPECTED TEST EXCEPTION. The presence of " + DecoratedObjectFactory.class.getName() + " is optional.");
                }
                if (LogfileViewerConsolePlugin.class.getName().equals(name)) {
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

        Class<? extends Servlet> type = (Class<? extends Servlet>) classLoaderWithoutDecoratedObjectFactory.loadClass(LogfileViewerConsolePlugin.class.getName());
        Servlet logViewerInstance = type.newInstance();

        ServletConfig config = mock(ServletConfig.class);
        ServletContext context = mock(ServletContext.class);
        doReturn(context).when(config).getServletContext();
        injectTailServlet(logViewerInstance);
        invokeInit(logViewerInstance, config);
        invokeDestroy(logViewerInstance);

        verify(context, never()).setAttribute(any(), any());
        verify(context, never()).removeAttribute(any());
    }

    private void invokeInit(Servlet servlet, ServletConfig config) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        Method method = servlet.getClass().getMethod("init", ServletConfig.class);
        method.setAccessible(true);
        method.invoke(servlet, config);
    }

    private void invokeDestroy(Servlet servlet) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        Method method = servlet.getClass().getMethod("destroy");
        method.setAccessible(true);
        method.invoke(servlet);
    }

    private void injectTailServlet(Object o) throws IllegalAccessException {
        Field field = findField(o.getClass(), "tailServlet");
        field.setAccessible(true);
        field.set(o, this.tailServlet);
    }

    private void verifyDecoratorObjectFactoryIsNotRemovedFromServletContext() {
        verify(this.context, never()).removeAttribute(any());
    }

    private void verifyDecoratorObjectFactoryIsNotInjectedIntoServletContext() {
        verify(this.context, never()).setAttribute(any(), any());
    }

    private void withExistingDecoratorObjectFactory() {
        doReturn(mock(DecoratedObjectFactory.class)).when(this.context).getAttribute(DecoratedObjectFactory.class.getName());
    }

    private void verifyDecoratorObjectFactoryIsRemovedFromServletContext() {
        verify(this.context).removeAttribute(DecoratedObjectFactory.class.getName());
    }

    private void verifyDecoratedObjectInstanceIsInjectedIntoServletContext() {
        verify(this.context).setAttribute(eq(DecoratedObjectFactory.class.getName()), isA(DecoratedObjectFactory.class));
    }

    private void verifyRequestIsDelegatedToTailServlet() throws ServletException, IOException {
        verify(this.tailServlet).service(this.request, this.response);
    }

    private void verifyTailServletIsDestroyed() {
        verify(this.tailServlet).destroy();
    }

    private void init() throws ServletException {
        this.testee.init(this.config);
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

    private void getAllLogfilesAsZip() throws ServletException, IOException {
        withRequestPath("/system/console/logviewer/download");
        doGet();
        this.zippedFiles = new ZipInputStream(new ByteArrayInputStream(this.internalOutputStream.toByteArray()));
    }

    private void getLogfileAsZip(String filePath) throws ServletException, IOException {
        withRequestPath("/system/console/logviewer/download");
        withFileRequestParameter(filePath);
        doGet();
        this.zippedFiles = new ZipInputStream(new ByteArrayInputStream(this.internalOutputStream.toByteArray()));
    }

    private void doGet() throws ServletException, IOException {
        this.testee.doGet(this.request, this.response);
        this.responseText = this.internalWriter.toString();
    }

    private void renderContent() throws IOException {
        this.testee.renderContent(this.request, this.response);
        this.responseText = this.internalWriter.getBuffer().toString();
    }

    private void withRequestPath(String requestPath) {
        when(this.request.getRequestURI()).thenReturn(requestPath);
    }

    private void withFileRequestParameter(String value) {
        doReturn(value).when(this.request).getParameter("file");
    }

    private void assertResponseContains(String expected) {
        assertThat(this.responseText).contains(expected);
    }

    private void assertResponseMatches(String regex) {
        assertThat(this.responseText).matches(regex);
    }

    private void assertZipResponseHasOneFile() throws IOException {
        assertThat(this.zippedFiles.available()).isEqualTo(1);
    }

    private String pathOf(String relativePath) {
        return toFile(relativePath).getAbsolutePath();
    }

    private File toFile(String relativePath) {
        return new File(this.testLogfileDirectory, relativePath);
    }
}
