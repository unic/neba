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

import org.eclipse.jetty.websocket.api.RemoteEndpoint;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import static java.lang.System.currentTimeMillis;
import static java.lang.Thread.*;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

/**
 * @author Olaf Otto
 */
@RunWith(MockitoJUnitRunner.class)
public abstract class TailTests {
    private File testLogfileDirectory;
    private StringBuilder receivedText;
    private Answer<?> recordText;

    @Mock
    private RemoteEndpoint remote;

    @Before
    public final void setUp() throws Exception {
        URL testLogfileUrl = getClass().getResource("/io/neba/core/logviewer/testlogfiles/");
        this.testLogfileDirectory = new File(testLogfileUrl.getFile());

        this.receivedText = new StringBuilder(4096);

        this.recordText = invocation -> {
            ByteBuffer buffer = (ByteBuffer) invocation.getArguments()[0];
            byte[] contents = new byte[buffer.limit()];
            buffer.get(contents, 0, contents.length);
            receivedText.append(new String(contents, "UTF-8"));
            return null;
        };

        doAnswer(recordText).when(remote).sendBytes(any());
    }

    public File getTestLogfileDirectory() {
        return testLogfileDirectory;
    }

    public StringBuilder getReceivedText() {
        return receivedText;
    }

    public RemoteEndpoint getRemote() {
        return remote;
    }

    public Object assertSendTextContains(String text) {
        return assertThat(normalizeLineBreaks(getReceivedText().toString()))
                .contains(normalizeLineBreaks(text));
    }

    public String pathOf(String relativePath) {
        return new File(getTestLogfileDirectory(), relativePath).getAbsolutePath();
    }

    public void verifyNoTextWasSent() throws IOException {
        verify(getRemote(), never()).sendBytes(any());
    }

    public void write(File file, String line) throws IOException {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(line);
        }
    }

    public void sleepUpTo(long amount, TimeUnit unit) {
        yield();
        try {
            sleep(unit.toMillis(amount));
        } catch (InterruptedException e) {
            // Continue.
        }
    }

    public static String normalizeLineBreaks(String s) {
        return s.replaceAll("[\r\n]+", "\n");
    }

    /**
     * Executes the callback as soon as {@link RemoteEndpoint#sendBytes(ByteBuffer) the mocked remote receives bytes}.
     * This allows reacting when {@link Tail} is picking up data from a log file.
     *
     * @param c must not be <code>null</code>.
     */
    public void uponWriteToRemoteDo(Callable c) throws IOException {
        Thread unitTest = currentThread();

        doAnswer(invocation -> {
            // Still track the received text
            recordText.answer(invocation);
            // As soon as bytes are received, call the callable and interrupt the test case, as it
            // may await this event.
            c.call();
            synchronized (unitTest) {
                unitTest.interrupt();
            }
            return null;
        }).when(getRemote()).sendBytes(isA(ByteBuffer.class));
    }

    /**
     * Expects the provided assertion not fail within ten seconds, trying every 100 milliseconds.
     *
     * @param runnable not null. Expected to throw an exception should the embodied exception fail.
     */
    public void eventually(Runnable runnable) {
        long max = SECONDS.toMillis(10),
                waited = 0,
                interval = 100;

        Throwable issue = null;

        while (waited < max) {
            try {
                runnable.run();
                return;
            } catch (Throwable t) {
                issue = t;
                long timeBeforeSleep = currentTimeMillis();
                try {
                    sleep(interval);
                } catch (InterruptedException e1) {
                    // continue
                }
                waited += (currentTimeMillis() - timeBeforeSleep);
            }
        }

        throw new AssertionError("Unable to satisfy within " + MILLISECONDS.toSeconds(max) + " seconds.", issue);
    }
}
