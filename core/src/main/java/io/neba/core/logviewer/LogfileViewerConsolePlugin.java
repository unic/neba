/*
  Copyright 2013 the original author or authors.
  <p>
  Licensed under the Apache License, Version 2.0 the "License";
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at
  <p>
  http://www.apache.org/licenses/LICENSE-2.0
  <p>
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
 */

package io.neba.core.logviewer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.felix.webconsole.AbstractWebConsolePlugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import static io.neba.core.util.ZipFileUtil.toZipFileEntryName;
import static java.lang.Class.forName;
import static java.lang.Thread.currentThread;
import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.apache.commons.io.IOUtils.copy;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.startsWith;
import static org.apache.commons.lang3.StringUtils.substringAfter;

/**
 * A web console plugin for tailing and downloading the CQ log files placed within the sling log directory as configured in the
 * Apache Sling Logging Configuration.
 *
 * @author Olaf Otto
 */
@Service(Servlet.class)
@Component
@Properties({
        @Property(name = "felix.webconsole.label", value = LogfileViewerConsolePlugin.LABEL),
        @Property(name = "service.description", value = "Provides a Felix console plugin for monitoring and downloading Sling logfiles."),
        @Property(name = "service.vendor", value = "neba.io")
})
public class LogfileViewerConsolePlugin extends AbstractWebConsolePlugin {
    static final String LABEL = "logviewer";
    private static final String RESOURCES_ROOT = "/META-INF/consoleplugin/logviewer";
    private static final String DECORATED_OBJECT_FACTORY = "org.eclipse.jetty.util.DecoratedObjectFactory";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private boolean isManagingDecoratedObjectFactory = false;

    @Reference
    private TailServlet tailServlet;

    @Reference
    private LogFiles logFiles;

    @Override
    public void init() throws ServletException {
        super.init();
        final ClassLoader ccl = currentThread().getContextClassLoader();
        try {
            injectDecoratorObjectFactoryIntoServletContext();
            currentThread().setContextClassLoader(getClass().getClassLoader());
            this.tailServlet.init(getServletConfig());
        } catch (Throwable t) {
            this.logger.error("Unable to initialize the tail servlet - the log viewer will not be available", t);
            // We have to catch an re-throw here, as Sling tends not to log exceptions thrown in servlet's init() methods.
            throw new ServletException("Unable to initialize the tail servlet - the log viewer will not be available", t);
        } finally {
            currentThread().setContextClassLoader(ccl);
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        removeDecoratorObjectFactoryFromServletContext();
        this.tailServlet.destroy();
    }

    @SuppressWarnings("unused")
    public String getCategory() {
        return "NEBA";
    }

    /**
     * This method follows a felix naming convention and is automatically used
     * by felix to retrieve resources for this plugin, e.g. when retrieving script resources.
     *
     * @param path must not be <code>null</code>.
     * @return the corresponding resource, or <code>null</code>.
     */
    public URL getResource(String path) {
        URL url = null;
        String internalPath = substringAfter(path, "/" + getLabel());
        if (startsWith(internalPath, "/static/")) {
            url = getClass().getResource(RESOURCES_ROOT + internalPath);
        }
        return url;
    }

    @Override
    protected void renderContent(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        writeScriptIncludes(res);
        writeHead(res);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        String suffix = substringAfter(req.getRequestURI(), req.getServletPath() + "/" + getLabel());

        if (!isBlank(suffix) && suffix.startsWith("/tail")) {
            this.tailServlet.service(req, res);
            return;
        }

        if (!isBlank(suffix) && suffix.equals("/download")) {
            download(res, req);
            return;
        }

        super.doGet(req, res);
    }

    private void injectDecoratorObjectFactoryIntoServletContext() throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        ServletContext servletContext = getServletContext();
        if (servletContext.getAttribute(DECORATED_OBJECT_FACTORY) != null || !isDecoratedObjectFactoryAvailable()) {
            return;
        }

        servletContext.setAttribute(DECORATED_OBJECT_FACTORY, forName(DECORATED_OBJECT_FACTORY).newInstance());
        this.isManagingDecoratedObjectFactory = true;
    }

    private boolean isDecoratedObjectFactoryAvailable() {
        try {
            forName(DECORATED_OBJECT_FACTORY, false, getClass().getClassLoader());
            return true;
        } catch (Throwable ex) {
            return false;
        }
    }

    private void removeDecoratorObjectFactoryFromServletContext() {
        if (this.isManagingDecoratedObjectFactory) {
            getServletContext().removeAttribute(DECORATED_OBJECT_FACTORY);
        }
    }

    private void writeHead(HttpServletResponse res) throws IOException {
        StringBuilder options = new StringBuilder(1024);
        this.logFiles.resolveLogFiles().forEach(file ->
                options.append("<option value=\"").append(file.getAbsolutePath()).append("\" ")
                        .append("title=\"").append(file.getAbsolutePath()).append("\">")
                        .append(file.getParentFile().getName()).append('/').append(file.getName())
                        .append("</option>"));
        writeFromTemplate(res, "head.html", options.toString());
        writeFromTemplate(res, "body.html");
    }

    /**
     * Streams the contents of the log directory as a zip file.
     */
    private void download(HttpServletResponse res, HttpServletRequest req) throws IOException {
        res.setContentType("application/zip");
        res.setHeader("Content-Disposition", "attachment;filename=logfiles-" + req.getServerName() + ".zip");
        ZipOutputStream zos = new ZipOutputStream(res.getOutputStream());
        try {
            for (File file : this.logFiles.resolveLogFiles()) {
                ZipEntry ze = new ZipEntry(toZipFileEntryName(file));
                zos.putNextEntry(ze);
                FileInputStream in = new FileInputStream(file);
                try {
                    copy(in, zos);
                    zos.closeEntry();
                } finally {
                    closeQuietly(in);
                }
            }
            zos.finish();
        } finally {
            closeQuietly(zos);
        }
    }

    private void writeFromTemplate(HttpServletResponse response, String templateName, Object... templateArgs) throws IOException {
        String template = readTemplate(templateName);
        response.getWriter().printf(template, templateArgs);
    }

    private void writeFromTemplate(HttpServletResponse response, String templateName) throws IOException {
        String template = readTemplate(templateName);
        response.getWriter().write(template);
    }

    private String readTemplate(String templateName) {
        return readTemplateFile(RESOURCES_ROOT + "/templates/" + templateName);
    }

    private void writeScriptIncludes(HttpServletResponse response) throws IOException {
        response.getWriter().write("<script src=\"" + getLabel() + "/static/script.js\"></script>");
        response.getWriter().write("<script src=\"" + getLabel() + "/static/encoding-indexes.js\"></script>");
        response.getWriter().write("<script src=\"" + getLabel() + "/static/encoding.js\"></script>");
    }

    @Override
    public String getTitle() {
        return "View logfiles";
    }

    @Override
    public String getLabel() {
        return LABEL;
    }
}
