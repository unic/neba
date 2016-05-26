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

import org.eclipse.jetty.websocket.api.RemoteEndpoint;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;

import static java.io.File.createTempFile;
import static java.nio.file.Files.move;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

/**
 * @author Olaf Otto
 */
@RunWith(MockitoJUnitRunner.class)
public class TailTest extends TailTests {
    private final ExecutorService executorService = newSingleThreadExecutor();

    private Tail testee;

    @After
    public void tearDown() throws Exception {
        if (this.testee != null) {
            this.testee.stop();
        }
        this.executorService.shutdownNow();
    }

    @Test
    public void testHandlingOfFileNotFoundExceptionDuringLogFileRotation() throws Exception {
        File logFile = createTempFile("tailsocket-test-", ".log", this.getTestLogfileDirectory().getParentFile());

        tailAsynchronously(logFile);

        // Wait for the tail to commence
        doSleep(500);

        write(logFile, "first line");

        // Wait for the tail to pickup the line
        doSleep(1000);

        rotate(logFile);

        // Provoke a file not found in the tailer by delaying the re-creation
        // of the tailed log file
        doSleep(800);

        createFile(logFile.getAbsolutePath());

        doSleep(1000);

        assertErrorMessageIsSent("file rotated");
    }

    @Test
    public void testHandlingOfFileRotation() throws Exception {
        File logFile = createTempFile("tailsocket-test-", ".log", this.getTestLogfileDirectory().getParentFile());

        tailAsynchronously(logFile);

        // Wait for the tail to commence
        doSleep(500);

        write(logFile, "first line");

        // Wait for the tail to pickup the change

        doSleep(1000);

        assertSendTextContains("first line");

        rotate(logFile);
        createFile(logFile.getAbsolutePath());

        doSleep(2000);

        assertErrorMessageIsSent("file rotated");
    }


    @Test
    public void testHandlingOfRemovedLogFile() throws Exception {
        File logFile = createTempFile("tailsocket-test-", ".log", this.getTestLogfileDirectory().getParentFile());

        tailAsynchronously(logFile);

        // Wait for the tail to commence
        doSleep(500);

        rotate(logFile);

        // Wait until the tolerance interval has passed
        doSleep(2000);

        assertErrorMessageIsSent("file not found");
    }

    @Test
    public void testHandlingOfIoExceptionWhenSendingLineToClient() throws Exception {
        doThrow(new IOException("THIS IS AN EXPECTED TEST EXCEPTION"))
                .when(this.getRemote())
                .sendBytes(any());

        tailSynchronously("logs/error.log");
    }

    @Test
    public void testPreservationOfWhiteSpaces() throws Exception {
        tailAsynchronously("logs/error-withwhitespaces.log");

        doSleep(1000);

        assertSendTextContains("06.09.2013 15:03:50.719 *ERROR* error message with stacktrace\r\n" +
                "  at org.apache.sling.jcr.resource.internal.JcrResourceResolverFactoryImpl.getDefaultWorkspaceName(JcrResourceResolverFactoryImpl.java:398)\r\n" +
                "        at org.apache.sling.jcr.resource.internal.JcrResourceResolver.getResource(JcrResourceResolver.java:817)");
    }

    @Test
    public void testTailErrorLogIsFullyRead() throws Exception {
        tailAsynchronously("logs/error.log");

        doSleep(1000);

        assertSendTextStartsWith("-- test logs/error.log first line --");
        assertSendTextEndsWith("-- test logs/error.log last line --");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testHandlingOfNullFileArgument() throws Exception {
        new Tail(mock(RemoteEndpoint.class), null, 1000);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testHandlingOfNullRemoteArgument() throws Exception {
        new Tail(null, mock(File.class), 1000);
    }

    private void assertSendTextEndsWith(String s) {
        assertThat(this.getReceivedText().toString()).endsWith(s);
    }

    private void assertSendTextStartsWith(String s) {
        assertThat(this.getReceivedText().toString()).startsWith(s);
    }

    private void assertErrorMessageIsSent(String message) throws IOException {
        verify(this.getRemote()).sendString(message);
    }

    private void tailSynchronously(String fileName) {
        this.testee = new Tail(getRemote(), new File(getTestLogfileDirectory(), fileName), 1024L * 1024L);
        this.testee.run();
    }

    private void tailAsynchronously(String fileName) {
        this.testee = new Tail(getRemote(), new File(getTestLogfileDirectory(), fileName), 1024L * 1024L);
        this.executorService.execute(this.testee);
    }

    private void tailAsynchronously(File logFile) {
        this.testee = new Tail(getRemote(), logFile, 1024);
        this.executorService.execute(this.testee);
    }

    private void rotate(File file) throws IOException {
        move(file.toPath(), new File(file.getAbsolutePath() + ".rotated").toPath());
    }

    private void createFile(String logFilePath) throws IOException {
        assertThat(new File(logFilePath).createNewFile())
                .describedAs("Re-creating the logfile was successful")
                .isTrue();
    }
}