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
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Dictionary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 * @author Olaf Otto
 */
@RunWith(MockitoJUnitRunner.class)
public class LogFilesTest {
    private static final String ORG_APACHE_SLING_COMMONS_LOG_FILE = "org.apache.sling.commons.log.file";
    @Mock
    private ConfigurationAdmin configurationAdmin;
    @Mock
    private BundleContext bundleContext;
    @Mock
    private Configuration logConfiguration;
    @Mock
    private Dictionary<?, String> logConfigurationProperties;

    private File slingHomeDirectory;

    @InjectMocks
    private LogFiles testee;

    @Before
    public void setUp() throws Exception {
        URL slingHomeUrl = getClass().getResource("/io/neba/core/logviewer/testlogfiles");
        this.slingHomeDirectory = new File(slingHomeUrl.getFile());
        when(this.bundleContext.getProperty(eq("sling.home"))).thenReturn(this.slingHomeDirectory.getAbsolutePath());
        when(this.configurationAdmin.getConfiguration(eq("org.apache.sling.commons.log.LogManager"))).thenReturn(this.logConfiguration);
        when(this.logConfiguration.getProperties()).thenReturn(this.logConfigurationProperties);
        when(this.logConfigurationProperties.get(eq(ORG_APACHE_SLING_COMMONS_LOG_FILE))).thenReturn("logs/error.log");
        this.testee.determineSlingHomeDirectory();
    }

    @Test
    public void testAdditionalLogFilesConfiguration() throws Exception {
        withAdditionalLogfile("remote-logs/error.log");
        assertThat(resolveLogFiles())
                .contains(file("remote-logs/error.log"));
    }

    @Test
    public void testFallBackToDefaultLogsLocationIfNoConfigurationExists() throws Exception {
        withNonexistentLoggingConfiguration();
        assertThat(resolveLogFiles())
                .contains(
                       file("logs/crx/error.log"),
                       file("logs/crx/error.log.0"),
                       file("logs/crx/error.log.2020-11-23"),
                       file("logs/error-withwhitespaces.log"),
                       file("logs/error.log"),
                       file("logs/error.log.1"),
                       file("logs/error.log.2020-01-01"));
    }

    @Test(expected = IllegalStateException.class)
    public void testHandlingOfConfigurationRetrievalFailure() throws Exception {
        withInvalidConfigurationQuerySyntax();
        resolveLogFiles();
    }

    @Test
    public void testMissingSlingLogConfigurationIsTolerated() throws Exception {
        withAdditionalLogfile(null);
        assertThat(resolveLogFiles())
                .contains(
                        file("logs/crx/error.log"),
                        file("logs/crx/error.log.0"),
                        file("logs/crx/error.log.2020-11-23"),
                        file("logs/error-withwhitespaces.log"),
                        file("logs/error.log"),
                        file("logs/error.log.1"),
                        file("logs/error.log.2020-01-01"));
    }

    private Collection<File> resolveLogFiles() throws IOException {
        return this.testee.resolveLogFiles();
    }

    private void withInvalidConfigurationQuerySyntax() throws IOException, InvalidSyntaxException {
        doThrow(new InvalidSyntaxException("THIS IS AN EXPECTED TEST EXCEPTION", ""))
                .when(this.configurationAdmin).listConfigurations(anyString());
    }

    private void withNonexistentLoggingConfiguration() {
        doReturn(null).when(this.logConfiguration).getProperties();
    }

    private void withAdditionalLogfile(String additionalLogfile) throws IOException, InvalidSyntaxException {
        Configuration configuration = mock(Configuration.class);
        @SuppressWarnings("unchecked")
        Dictionary<?, String> properties = mock(Dictionary.class);
        String absolutePath = additionalLogfile == null ? null : file(additionalLogfile).getAbsolutePath();
        when(properties.get(eq(ORG_APACHE_SLING_COMMONS_LOG_FILE))).thenReturn(absolutePath);
        when(configuration.getProperties()).thenReturn(properties);
        Configuration[] configurations = new Configuration[]{configuration};
        when(this.configurationAdmin.listConfigurations(anyString())).thenReturn(configurations);
    }


    private File file(String relativePath) {
        return new File(this.slingHomeDirectory, relativePath);
    }
}