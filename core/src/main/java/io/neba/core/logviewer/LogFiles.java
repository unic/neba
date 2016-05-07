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

import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.eclipse.gemini.blueprint.context.BundleContextAware;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Dictionary;
import java.util.TreeSet;

import static org.apache.commons.io.FileUtils.listFiles;
import static org.apache.commons.lang.StringUtils.isEmpty;

/**
 * Represents both the log files in the default logfile directory (${sling.home}/logs)
 * as well as any additional log files configured in the felix console.
 *
 * @author Olaf Otto
 */
@Service
public class LogFiles implements BundleContextAware {
    // Obtained from the felix console configuration for the log manager.
    private static final String LOG_FILE_PROPERTY = "org.apache.sling.commons.log.file";
    private static final String LOG_MANAGER_PID = "org.apache.sling.commons.log.LogManager";
    private static final String LOG_FACTORY_PID = "org.apache.sling.commons.log.LogManager.factory.config";

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

    @Autowired
    private ConfigurationAdmin configurationAdmin;

    private BundleContext context;
    private File slingHomeDirectory;

    @PostConstruct
    public void determineSlingHomeDirectory() {
        this.slingHomeDirectory = new File(this.context.getProperty("sling.home"));
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

    @SuppressWarnings("unchecked")
    public Collection<File> resolveLogFiles() throws IOException {
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

    private Configuration getCommonsLogConfiguration() throws IOException {
        return this.configurationAdmin.getConfiguration(LOG_MANAGER_PID);
    }

    @Override
    public void setBundleContext(BundleContext bundleContext) {
        this.context = bundleContext;
    }
}
