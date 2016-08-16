/**
 * Copyright 2013 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 the "License";
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/

package io.neba.core.logviewer;

import org.apache.felix.webconsole.AbstractWebConsolePlugin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static io.neba.core.util.ZipFileUtil.toZipFileEntryName;
import static java.lang.Thread.currentThread;
import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.apache.commons.io.IOUtils.copy;
import static org.apache.commons.lang.StringUtils.*;

/**
 * A web console plugin for tailing and downloading the CQ log files placed within the sling log directory as configured in the
 * Apache Sling Logging Configuration.
 *
 * @author Olaf Otto
 */
@Service
public class LogfileViewerConsolePlugin extends AbstractWebConsolePlugin {
    private static final String LABEL = "logviewer";
    private static final String RESOURCES_ROOT = "/META-INF/consoleplugin/logviewer";

    @Autowired
    private TailServlet tailServlet;

    @Autowired
    private LogFiles logFiles;


    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        final ClassLoader ccl = currentThread().getContextClassLoader();
        try {
            currentThread().setContextClassLoader(getClass().getClassLoader());
            this.tailServlet.init(config);
        } catch (RuntimeException e) {
            throw new ServletException("Unable to initialize the tail servlet - the log viewer will not be available", e);
        } finally {
            currentThread().setContextClassLoader(ccl);
        }
    }

    @Override
    public void destroy() {
        super.destroy();
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
