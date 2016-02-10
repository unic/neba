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

import io.neba.core.util.ReverseFileByLineReader;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.felix.webconsole.AbstractWebConsolePlugin;
import org.eclipse.gemini.blueprint.context.BundleContextAware;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static java.util.regex.Pattern.compile;
import static org.apache.commons.io.FileUtils.listFiles;
import static org.apache.commons.lang.StringEscapeUtils.escapeHtml;
import static org.apache.commons.lang.StringUtils.*;

/**
 * A web console plugin for tailing and downloading the CQ logfiles placed within the sling log directory as configured in the
 * Apache Sling Logging Configuration.
 *
 * @author Olaf Otto
 */
@Service
public class LogfileViewerConsolePlugin extends AbstractWebConsolePlugin implements BundleContextAware {
    private static final String LABEL = "logviewer";
    private static final String RESOURCES_ROOT = "/META-INF/consoleplugin/logviewer";
    private static final Pattern MESSAGE_LINE_WITH_LEVEL = compile("(.*\\*[ \t]*(ERROR|INFO|WARN|DEBUG|TRACE)[ \t]*\\*.*)");
    private static final Pattern BREAKS = compile("[\n\r]");

    // Obtained from the felix console configuration for the log manager.
    private static final String LOG_FILE_PROPERTY = "org.apache.sling.commons.log.file";
    private static final String LOG_MANAGER_PID = "org.apache.sling.commons.log.LogManager";
    private static final String LOG_FACTORY_PID = "org.apache.sling.commons.log.LogManager.factory.config";
    // Estimated using the error log of a vanilla CQ 5.4 instance
    private static final int APPROX_BYTES_PER_LINE_IN_ERRORLOG = 256;

    private static final IOFileFilter LOGFILE_FILTER = new IOFileFilter() {
        @Override
        public boolean accept(File file) {
            return file.canRead() && acceptFileName(file.getName());
        }

        @Override
        public boolean accept(File dir, String name) {
            return acceptFileName(name);
        }

        private boolean acceptFileName(String fileName) {
            return fileName.endsWith(".log") || fileName.contains(".log.");
        }
    };

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private BundleContext context;
    private File slingHomeDirectory;

    @Autowired
    private ConfigurationAdmin configurationAdmin;

    @PostConstruct
    public void determineSlingHomeDirectory() {
        this.slingHomeDirectory = new File(this.context.getProperty("sling.home"));
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
        writeScriptIncludes(req, res);
        writeHead(res);
    }

    private void writeHead(HttpServletResponse res) throws IOException {
        StringBuilder options = new StringBuilder(1024);
        for (File logFile : resolveLogFiles()) {
            String fileIdentifier = getNormalizedFilePath(logFile);
            options.append("<option value=\"").append(fileIdentifier).append("\" ")
                    .append("title=\"").append(fileIdentifier).append("\">")
                    .append(logFile.getParentFile().getName()).append('/').append(logFile.getName())
                    .append("</option>");
        }
        writeFromTemplate(res, "head.html", options.toString());
        writeFromTemplate(res, "body.html");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        String suffix = substringAfter(req.getRequestURI(), req.getServletPath() + "/" + getLabel());
        if (!isBlank(suffix) && suffix.startsWith("/tail/")) {
            tail(suffix.substring(5), res);
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
        res.setHeader("Content-Disposition", "attachment;filename=logfiles-" + req.getServerName()  + ".zip");
        ZipOutputStream zos = new ZipOutputStream(res.getOutputStream());
        try {

            for (File file : resolveLogFiles()) {
                String fileIdentifier = getNormalizedFilePath(file);
                ZipEntry ze = new ZipEntry(fileIdentifier);
                zos.putNextEntry(ze);
                FileInputStream in = new FileInputStream(file);
                try {
                    IOUtils.copy(in, zos);
                    zos.closeEntry();
                } finally {
                    IOUtils.closeQuietly(in);
                }
            }
            zos.finish();
        } finally {
            IOUtils.closeQuietly(zos);
        }
    }

    private String getNormalizedFilePath(File logFile) {
        String filePath = substringAfter(logFile.getAbsolutePath(), File.separator);
        // Strip any further separator chars, e.g. from network paths (//).
        while (filePath.startsWith(File.separator)) {
            filePath = filePath.substring(1);
        }
        return normalizePath(filePath);
    }

    /**
     * @param tailCommand a tail command in the form /numberOfLines/fileIdentifier, e.g.
     *                    <code>/200/logs/error.log</code>.
     */
    private void tail(String tailCommand, HttpServletResponse res) throws IOException {
        res.setContentType("text/html;charset=UTF-8");
        String relativeTailCommand = tailCommand.substring(1);
        final int numberOfLines = NumberUtils.toInt(substringBefore(relativeTailCommand, "/"), 200);
        final String normalizedFilePath = substringAfter(relativeTailCommand, "/");

        File file = null;
        // Security: only allow access to known logfiles.
        // Rationale: otherwise, one may pass paths to any other file readable by the system user.
        for (File logFile : resolveLogFiles()) {
            String path = getNormalizedFilePath(logFile);
            if (path.equals(normalizedFilePath)) {
                file = logFile;
                break;
            }
        }
        if (file != null) {
            try {
                PrintWriter writer = res.getWriter();
                writer.write("<div id=\"tail\">");
                Queue<String> lines = readLastLinesOf(numberOfLines, file);
                boolean tagOpened = false;
                while (!lines.isEmpty()) {
                    String line = prepareForHtml(lines.remove());
                    Matcher m = MESSAGE_LINE_WITH_LEVEL.matcher(line);
                    if (m.matches()) {
                        if (tagOpened) {
                            writer.write("</div>");
                        }
                        writer.write("<div class=\"");
                        writer.write(m.group(2));
                        writer.write("\">");
                        writer.write(line);
                        tagOpened = true;
                    } else {
                        if (tagOpened) {
                            writer.write("<br />");
                            writer.write(line);
                        } else {
                            writer.write("<div class=\"INFO\">");
                            writer.write(line);
                            tagOpened = true;
                        }
                    }
                }
                if (tagOpened) {
                    writer.write("</div>");
                }
                writer.write("</div>");
            } catch (IOException e) {
                this.logger.error("Unable to tail the logfile " + file.getAbsolutePath() + ".", e);
            }
        }
    }

    private String prepareForHtml(String line) {
        return BREAKS.matcher(escapeHtml(line)).replaceAll("");
    }

    private Queue<String> readLastLinesOf(int numberOfLines, File file) throws IOException {
        // Reverse line order using LIFO: The file is read bottom-up but the lines shall be displayed
        // in their correct order (top down).
        Queue<String> readLines = Collections.asLifoQueue(new LinkedList<>());

        ReverseFileByLineReader lineReader = new ReverseFileByLineReader(file, APPROX_BYTES_PER_LINE_IN_ERRORLOG);
        try {
            String line = lineReader.readPreviousLine();
            for (int i = 0; i < numberOfLines && line != null; ++i) {
                readLines.add(line);
                line = lineReader.readPreviousLine();
            }
        } finally {
            lineReader.close();
        }
        return readLines;
    }

    @SuppressWarnings("unchecked")
    private Collection<File> resolveLogFiles() throws IOException {
        File logDir = getLogfileDirectory();
        Collection<File> logFiles = new TreeSet<>((o1, o2) -> {
            return o1.getPath().compareToIgnoreCase(o2.getPath());
        });

        if (logDir == null) {
            // No configured log file directory exists, assume the default
            logDir = new File(this.slingHomeDirectory, "logs");
        }

        // The log directory may be removed during runtime - always check access.
        if (logDir.exists() && logDir.isDirectory()) {
            logFiles.addAll(listFiles(logDir, LOGFILE_FILTER, TrueFileFilter.INSTANCE));
        }

        for (File logFile : resolveFactoryConfiguredLogFiles()) {
            if (!logFile.getParentFile().getAbsolutePath().startsWith(logDir.getAbsolutePath())) {
                logFiles.addAll(listFiles(logFile.getParentFile(), LOGFILE_FILTER, TrueFileFilter.INSTANCE));
            }
        }
        return logFiles;
    }

    private Collection<File> resolveFactoryConfiguredLogFiles() throws IOException {
        Collection<File> logFiles = new ArrayList<>();
        Configuration[] configurations;
        try {
            configurations = this.configurationAdmin.listConfigurations("(service.factoryPid=" + LOG_FACTORY_PID + ")");
        } catch (InvalidSyntaxException e) {
            throw new IllegalStateException("Unable to obtain the log files with factory pid " + LOG_FACTORY_PID + ".", e);
        }
        if (configurations != null) {
            for (Configuration logConfiguration : configurations) {
                File logFile = getConfiguredLogfile(logConfiguration);
                if (logFile != null && logFile.exists() && logFile.canRead()) {
                    logFiles.add(logFile);
                }
            }
        }
        return logFiles;
    }

    private File getLogfileDirectory() throws IOException {
        Configuration logConfiguration = getCommonsLogConfiguration();
        File defaultLogFile = getConfiguredLogfile(logConfiguration);
        if (defaultLogFile != null && defaultLogFile.exists() && defaultLogFile.canRead()) {
            return defaultLogFile.getParentFile();
        } else {
            return null;
        }
    }

    private File getConfiguredLogfile(Configuration logConfiguration) throws IOException {
        Dictionary properties = logConfiguration.getProperties();
        if (properties == null) {
            return null;
        }

        String logFilePath = (String) properties.get(LOG_FILE_PROPERTY);
        if (isEmpty(logFilePath)) {
            return null;
        }

        File logFile = new File(logFilePath);
        if (!logFile.isAbsolute()) {
            logFile = new File(this.slingHomeDirectory, logFilePath);
        }

        return logFile.getCanonicalFile();
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

    private void writeScriptIncludes(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.getWriter().write("<script src=\"" + getLabel() + "/static/script.js\"></script>");
    }

    /**
     * Replace windows file separator(s) (\) with linux file separator(s) (/).
     */
    private String normalizePath(String path) {
        return path.replaceAll("[\\\\]+", "/");
    }

    private Configuration getCommonsLogConfiguration() throws IOException {
        return this.configurationAdmin.getConfiguration(LOG_MANAGER_PID);
    }

    @Override
    public void setBundleContext(BundleContext bundleContext) {
        this.context = bundleContext;
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
