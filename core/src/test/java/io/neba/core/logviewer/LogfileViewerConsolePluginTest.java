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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

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
import java.net.URL;
import java.util.Dictionary;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.apache.commons.lang.StringUtils.substringAfter;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Olaf Otto
 */
@RunWith(MockitoJUnitRunner.class)
public class LogfileViewerConsolePluginTest {
    private static final String ORG_APACHE_SLING_COMMONS_LOG_FILE = "org.apache.sling.commons.log.file";
    @Mock
    private ConfigurationAdmin configurationAdmin;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private BundleContext bundleContext;
    @Mock
    private Configuration logConfiguration;
    @Mock
    private Dictionary logConfigurationProperties;
    @Mock
    private ServletOutputStream outputStream;

    private File testLogfileDirectory;
    private StringWriter internalWriter;
    private String htmlResponse;
    private ByteArrayOutputStream internalOutputStream;
    private ZipInputStream zippedFiles;

    @InjectMocks
    private LogfileViewerConsolePlugin testee;

    @Before
    public void setUp() throws Exception {
        this.internalWriter = new StringWriter();
        this.internalOutputStream = new ByteArrayOutputStream(8192);
        PrintWriter writer = new PrintWriter(this.internalWriter);

        URL testLogfileUrl = getClass().getResource("/io/neba/core/logviewer/");
        this.testLogfileDirectory = new File(testLogfileUrl.getFile());

        when(this.bundleContext.getProperty(eq("sling.home"))).thenReturn(this.testLogfileDirectory.getAbsolutePath());
        when(this.request.getServletPath()).thenReturn("/system/console");
        when(this.request.getServerName()).thenReturn("servername");
        when(this.response.getWriter()).thenReturn(writer);
        when(this.response.getOutputStream()).thenReturn(this.outputStream);
        when(this.configurationAdmin.getConfiguration(eq("org.apache.sling.commons.log.LogManager"))).thenReturn(this.logConfiguration);
        when(this.logConfiguration.getProperties()).thenReturn(this.logConfigurationProperties);
        when(this.logConfigurationProperties.get(eq(ORG_APACHE_SLING_COMMONS_LOG_FILE))).thenReturn("logs/error.log");

        Answer writeIntToByteArrayOutputStream = new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                internalOutputStream.write((Integer) invocation.getArguments()[0]);
                return null;
            }
        };
        Answer writeBytesToByteArrayOutputStream = new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                byte[] b = (byte[]) invocation.getArguments()[0];
                int off = (Integer) invocation.getArguments()[1];
                int len = (Integer) invocation.getArguments()[2];
                internalOutputStream.write(b, off, len);
                return null;
            }
        };
        doAnswer(writeIntToByteArrayOutputStream).when(this.outputStream).write(anyInt());
        doAnswer(writeBytesToByteArrayOutputStream).when(this.outputStream).write(isA(byte[].class), anyInt(), anyInt());

        this.testee.determineSlingHomeDirectory();
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
    public void testTailErrorLogIsFullyRead() throws Exception {
        withRequestPath("/system/console/logviewer/tail/200/" + pathOf("logs/error.log"));
        doGet();
        assertHtmlResponseContains("-- test logs/error.log first line --");
        assertHtmlResponseContains("-- test logs/error.log last line --");
    }

    @Test
    public void testTailCrxErrorLogIsFullyRead() throws Exception {
        withRequestPath("/system/console/logviewer/tail/200/" + pathOf("logs/crx/error.log"));
        doGet();
        assertHtmlResponseContains("-- test crx/error.log first line --");
        assertHtmlResponseContains("-- test crx/error.log last line --");
    }

    @Test
    public void testHighlightingOfErrorMessages() throws Exception {
        withRequestPath("/system/console/logviewer/tail/200/" + pathOf("logs/error.log"));
        doGet();
        assertNormalizedHtmlResponseContains("<div class=\"ERROR\">* ERROR*: error message</div>");
    }

    @Test
    public void testHighlightingOfWarnMessages() throws Exception {
        withRequestPath("/system/console/logviewer/tail/200/" + pathOf("logs/error.log"));
        doGet();
        assertNormalizedHtmlResponseContains("<div class=\"WARN\">* WARN *: warn message</div>");
    }

    @Test
    public void testHighlightingOfInfoMessages() throws Exception {
        withRequestPath("/system/console/logviewer/tail/200/" + pathOf("logs/error.log"));
        doGet();
        assertNormalizedHtmlResponseContains("<div class=\"INFO\">*INFO*: info message</div>");
    }

    @Test
    public void testHighlightingOfDebugMessages() throws Exception {
        withRequestPath("/system/console/logviewer/tail/200/" + pathOf("logs/error.log"));
        doGet();
        assertNormalizedHtmlResponseContains("<div class=\"DEBUG\">*DEBUG *: debug message</div>");
    }

    @Test
    public void testHighlightingOfTraceMessageIncludesSubsequentMessageWithoutLevelAsStackTrace() throws Exception {
        withRequestPath("/system/console/logviewer/tail/200/" + pathOf("logs/error.log"));
        doGet();
        assertNormalizedHtmlResponseContains("<div class=\"TRACE\">*TRACE *: trace message<br />message without level</div>");
    }

    @Test
    public void testPreservationOfWhiteSpaces() throws Exception {
        withRequestPath("/system/console/logviewer/tail/200/" + pathOf("logs/error-withwhitespaces.log"));
        doGet();
        assertHtmlResponseContains("06.09.2013 15:03:50.719 *ERROR* error message with stacktrace<br />" +
                "  at org.apache.sling.jcr.resource.internal.JcrResourceResolverFactoryImpl.getDefaultWorkspaceName(JcrResourceResolverFactoryImpl.java:398)<br />" +
                "        at org.apache.sling.jcr.resource.internal.JcrResourceResolver.getResource(JcrResourceResolver.java:817)<br />");
    }

    @Test
    public void testStacktraceInclusionInMessages() throws Exception {
        withRequestPath("/system/console/logviewer/tail/200/" + pathOf("logs/error.log"));
        doGet();
        assertNormalizedHtmlResponseContains("<div class=\"ERROR\">06.09.2013 15:03:50.719 *ERROR* error message with stacktrace<br />" +
                "at org.apache.sling.jcr.resource.internal.JcrResourceResolverFactoryImpl.getDefaultWorkspaceName(JcrResourceResolverFactoryImpl.java:398)<br />" +
                "at org.apache.sling.jcr.resource.internal.JcrResourceResolver.getResource(JcrResourceResolver.java:817)<br />" +
                "at ResourceResolvingRowIterable$ResourceResolvingRowIterator.createNext(ResourceResolvingRowIterable.java:44)<br />" +
                "at ResourceResolvingRowIterable$ResourceResolvingRowIterator.hasNext(ResourceResolvingRowIterable.java:36)<br />" +
                "at ResourceUtil$AdaptingIterator.createNext(ResourceUtil.java:43)<br />" +
                "at ResourceUtil$AdaptingIterator.hasNext(ResourceUtil.java:38)<br />" +
                "at MarketingUrlServiceImpl.collectRules(MarketingUrlServiceImpl.java:162)<br />" +
                "at MarketingUrlServiceImpl.getRule(MarketingUrlServiceImpl.java:81)<br />" +
                "at MarketingUrlFilter.findRule(MarketingUrlFilter.java:205)<br />" +
                "at MarketingUrlFilter.doFilter(MarketingUrlFilter.java:72)<br />" +
                "at org.apache.sling.engine.impl.filter.AbstractSlingFilterChain.doFilter(AbstractSlingFilterChain.java:60)<br />" +
                "at io.neba.core.resourcemodels.caching.RequestScopedResourceModelCache.doFilter(RequestScopedResourceModelCache.java:107)<br />" +
                "at org.apache.sling.engine.impl.filter.AbstractSlingFilterChain.doFilter(AbstractSlingFilterChain.java:60)<br />" +
                "at org.springframework.web.filter.RequestContextFilter.doFilterInternal(RequestContextFilter.java:83)<br />" +
                "at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:76)<br />" +
                "at org.apache.sling.engine.impl.filter.AbstractSlingFilterChain.doFilter(AbstractSlingFilterChain.java:60)<br />" +
                "at org.apache.sling.bgservlets.impl.BackgroundServletStarterFilter.doFilter(BackgroundServletStarterFilter.java:135)<br />" +
                "at org.apache.sling.engine.impl.filter.AbstractSlingFilterChain.doFilter(AbstractSlingFilterChain.java:60)<br />" +
                "at org.apache.sling.engine.impl.SlingRequestProcessorImpl.processRequest(SlingRequestProcessorImpl.java:171)<br />" +
                "at org.apache.sling.engine.impl.SlingMainServlet.service(SlingMainServlet.java:199)<br />" +
                "at org.apache.felix.http.base.internal.handler.ServletHandler.doHandle(ServletHandler.java:96)<br />" +
                "at org.apache.felix.http.base.internal.handler.ServletHandler.handle(ServletHandler.java:79)<br />" +
                "at org.apache.felix.http.base.internal.dispatch.ServletPipeline.handle(ServletPipeline.java:42)<br />" +
                "at org.apache.felix.http.base.internal.dispatch.InvocationFilterChain.doFilter(InvocationFilterChain.java:49)<br />" +
                "at org.apache.felix.http.base.internal.dispatch.HttpFilterChain.doFilter(HttpFilterChain.java:33)<br />" +
                "at org.apache.sling.security.impl.ReferrerFilter.doFilter(ReferrerFilter.java:249)<br />" +
                "at org.apache.felix.http.base.internal.handler.FilterHandler.doHandle(FilterHandler.java:88)<br />" +
                "at org.apache.felix.http.base.internal.handler.FilterHandler.handle(FilterHandler.java:76)<br />" +
                "at org.apache.felix.http.base.internal.dispatch.InvocationFilterChain.doFilter(InvocationFilterChain.java:47)<br />" +
                "at org.apache.felix.http.base.internal.dispatch.HttpFilterChain.doFilter(HttpFilterChain.java:33)<br />" +
                "at org.apache.felix.http.base.internal.dispatch.FilterPipeline.dispatch(FilterPipeline.java:48)<br />" +
                "at org.apache.felix.http.base.internal.dispatch.Dispatcher.dispatch(Dispatcher.java:39)<br />" +
                "at org.apache.felix.http.base.internal.DispatcherServlet.service(DispatcherServlet.java:67)<br />" +
                "at javax.servlet.http.HttpServlet.service(HttpServlet.java:802)<br />" +
                "at org.apache.felix.http.proxy.ProxyServlet.service(ProxyServlet.java:60)<br />" +
                "at javax.servlet.http.HttpServlet.service(HttpServlet.java:802)<br />" +
                "at org.apache.sling.launchpad.base.webapp.SlingServletDelegate.service(SlingServletDelegate.java:277)<br />" +
                "at org.apache.sling.launchpad.webapp.SlingServlet.service(SlingServlet.java:150)<br />" +
                "at com.day.j2ee.servletengine.ServletRuntimeEnvironment.service(ServletRuntimeEnvironment.java:228)<br />" +
                "at com.day.j2ee.servletengine.RequestDispatcherImpl.doFilter(RequestDispatcherImpl.java:315)<br />" +
                "at com.day.j2ee.servletengine.FilterChainImpl.doFilter(FilterChainImpl.java:74)<br />" +
                "at com.day.crx.launchpad.filters.CRXLaunchpadLicenseFilter.doFilter(CRXLaunchpadLicenseFilter.java:96)<br />" +
                "at com.day.j2ee.servletengine.FilterChainImpl.doFilter(FilterChainImpl.java:72)<br />" +
                "at com.day.j2ee.servletengine.RequestDispatcherImpl.service(RequestDispatcherImpl.java:334)<br />" +
                "at com.day.j2ee.servletengine.RequestDispatcherImpl.service(RequestDispatcherImpl.java:378)<br />" +
                "at com.day.j2ee.servletengine.ServletHandlerImpl.execute(ServletHandlerImpl.java:315)<br />" +
                "at com.day.j2ee.servletengine.DefaultThreadPool$DequeueThread.run(DefaultThreadPool.java:134)<br />" +
                "at java.lang.Thread.run(Thread.java:662)");
    }

    @Test
    public void testDownloadLogFilesAsZip() throws Exception {
        getZippedLogfiles();
        assertNextZipEntryIs(pathOf("logs/crx/error.log"));
        assertNextZipEntryIs(pathOf("logs/crx/error.log.0"));
        assertNextZipEntryIs(pathOf("logs/crx/error.log.2020-11-23"));
        assertNextZipEntryIs(pathOf("logs/error-withwhitespaces.log"));
        assertNextZipEntryIs(pathOf("logs/error.log"));
        assertNextZipEntryIs(pathOf("logs/error.log.1"));
        assertNextZipEntryIs(pathOf("logs/error.log.2020-01-01"));
    }

    @Test
    public void testAdditionalLogFilesInDropdown() throws Exception {
        withAdditionalLogfile("remote-logs/error.log");
        renderContent();
        assertHtmlResponseContains("value=\"" + pathOf("remote-logs/error.log") + "\"");
    }

    @Test
    public void testTailAdditionalLogfile() throws Exception {
        withAdditionalLogfile("remote-logs/error.log");
        withRequestPath("/system/console/logviewer/tail/200/" + pathOf("remote-logs/error.log"));
        doGet();
        assertHtmlResponseContains("-- test remote-logs/error.log first line --");
    }

    @Test
    public void testDownloadAdditionalLogFilesAsZip() throws Exception {
        withAdditionalLogfile("remote-logs/error.log");
        getZippedLogfiles();
        verifyLogFilesAreSendAs("logfiles-servername.zip");
        assertNextZipEntryIs(pathOf("logs/crx/error.log"));
        assertNextZipEntryIs(pathOf("logs/crx/error.log.0"));
        assertNextZipEntryIs(pathOf("logs/crx/error.log.2020-11-23"));
        assertNextZipEntryIs(pathOf("logs/error-withwhitespaces.log"));
        assertNextZipEntryIs(pathOf("logs/error.log"));
        assertNextZipEntryIs(pathOf("logs/error.log.1"));
        assertNextZipEntryIs(pathOf("logs/error.log.2020-01-01"));
        assertNextZipEntryIs(pathOf("remote-logs/error.log"));
        assertNextZipEntryIs(pathOf("remote-logs/error.log.2020-01-01"));
    }

    private void verifyLogFilesAreSendAs(String filename) {
        verify(this.response).setHeader(eq("Content-Disposition"), eq("attachment;filename=" + filename));
    }

    private void withAdditionalLogfile(String additionalLogfile) throws IOException, InvalidSyntaxException {
        Configuration configuration = mock(Configuration.class);
        Dictionary properties = mock(Dictionary.class);
        String absoluteLogfilePath = this.testLogfileDirectory.getAbsolutePath() + File.separator + additionalLogfile;
        when(properties.get(eq(ORG_APACHE_SLING_COMMONS_LOG_FILE))).thenReturn(absoluteLogfilePath);
        when(configuration.getProperties()).thenReturn(properties);
        Configuration[] configurations = new Configuration[]{ configuration };
        when(this.configurationAdmin.listConfigurations(anyString())).thenReturn(configurations);
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

    private void assertNormalizedHtmlResponseContains(String expected) {
        assertThat(this.htmlResponse.replaceAll("[\n\r\t]", "")).contains(expected);
    }

    private void renderContent() throws ServletException, IOException {
        this.testee.renderContent(this.request, this.response);
        this.htmlResponse = this.internalWriter.getBuffer().toString();
    }

    private String pathOf(String relativePath) {
        String basePath = substringAfter(this.testLogfileDirectory.getAbsolutePath(), File.separator)
                                    .replaceAll("[\\\\]+", "/");
        return basePath + "/" + relativePath;
    }
}