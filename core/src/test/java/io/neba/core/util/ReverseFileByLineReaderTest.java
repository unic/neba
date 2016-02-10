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

package io.neba.core.util;

import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.lang.StringUtils.trim;
import static org.fest.assertions.Assertions.assertThat;

/**
 * @author Olaf Otto
 */
public class ReverseFileByLineReaderTest {
    // The last two lines of the logfile, in inverse order.
    private static final String[] EXPECTED_LINES = new String[]{
            "10.10.2012 13:05:26.785 *INFO* [OsgiInstallerImpl] com.day.crx.sling.crx-auth-token Service " +
                    "[com.day.crx.security.token.impl.impl.TokenAuthenticationHandler,107] ServiceEvent REGISTERED",
            "10.10.2012 13:05:26.784 *INFO* [OsgiInstallerImpl] com.day.crx.sling.crx-auth-token Service " +
                    "[com.day.crx.security.token.impl.TokenCleanupTask,106] ServiceEvent REGISTERED"};

    private File unixLogfile;
    private File windowsLogfile;
    private int estimatedBytesPerLine;
    private List<String> lines;

    private ReverseFileByLineReader testee;

    @Before
    public void prepareTest() {
        this.unixLogfile = getFile("logFileWithUnixLineBreaks.log");
        this.windowsLogfile = getFile("logFileWithWindowsLineBreaks.log");
    }

    @Test
    public void testRead1000PreviousLinesOfWindowsLogfile() throws Exception {
        withEstimatedBytesPerLine(1000);
        withWindowsLogfile();
        readLines(2);
        assertReadLinesAre(EXPECTED_LINES);
    }

    @Test
    public void testRead10PreviousLinesOfWindowsLogfile() throws IOException {
        withEstimatedBytesPerLine(10);
        withWindowsLogfile();
        readLines(2);

        assertReadLinesAre(EXPECTED_LINES);
    }

    @Test
    public void testRead100PreviousLinesOfWindowsLogfile() throws IOException {
        withEstimatedBytesPerLine(100);
        withWindowsLogfile();
        readLines(2);

        assertReadLinesAre(EXPECTED_LINES);
    }

    @Test
    public void testRead1000PreviousLinesOfUnixLogfile() throws Exception {
        withEstimatedBytesPerLine(1000);
        withUnixLogfile();
        readLines(2);

        assertReadLinesAre(EXPECTED_LINES);
    }

    @Test
    public void testRead10PreviousLinesOfUnixLogfile() throws IOException {
        withEstimatedBytesPerLine(10);
        withUnixLogfile();
        readLines(2);

        assertReadLinesAre(EXPECTED_LINES);
    }

    @Test
    public void testRead100PreviousLinesOfUnixLogfile() throws IOException {
        withEstimatedBytesPerLine(100);
        withUnixLogfile();
        readLines(2);

        assertReadLinesAre(EXPECTED_LINES);
    }

    @Test(expected = IOException.class)
    public void testCloseWindowsLogfile() throws Exception {
        withWindowsLogfile();
        closeReader();
        readLines(1);
    }

    @Test(expected = IOException.class)
    public void testCloseUnixLogfile() throws Exception {
        withUnixLogfile();
        closeReader();
        readLines(1);
    }

    private void closeReader() throws IOException {
        this.testee.close();
    }

    private void assertReadLinesAre(String... lines) {
        assertThat(this.lines)
                .hasSize(lines.length)
                .containsExactly((Object[]) lines);
    }

    private void readLines(int numberOfLines) throws IOException {
        List<String> lines = new ArrayList<>(500);
        for (int i = 0; i < numberOfLines; ++i) {
            String line = this.testee.readPreviousLine();
            lines.add(trim(line));
        }
        this.lines = lines;
    }

    private File getFile(String resourcePath) {
        URL resource = getClass().getClassLoader().getResource("io/neba/core/logviewer/" + resourcePath);
        if (resource != null) {
            return new File(resource.getFile());
        }
        return null;
    }

    private void withEstimatedBytesPerLine(int bytes) {
        this.estimatedBytesPerLine = bytes;
    }

    private void withWindowsLogfile() throws IOException {
        this.testee = new ReverseFileByLineReader(this.windowsLogfile, this.estimatedBytesPerLine);
    }

    private void withUnixLogfile() throws IOException {
        this.testee = new ReverseFileByLineReader(this.unixLogfile, this.estimatedBytesPerLine);
    }
}
