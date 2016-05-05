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
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.nio.ByteBuffer;

import static java.lang.Thread.sleep;
import static java.lang.Thread.yield;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

/**
 * @author Olaf Otto
 */
@RunWith(MockitoJUnitRunner.class)
public abstract class TailTests {
    private File testLogfileDirectory;
    private StringBuilder receivedText;

    @Mock
    private RemoteEndpoint remote;

    @Before
    public final void setUp() throws Exception {
        URL testLogfileUrl = getClass().getResource("/io/neba/core/logviewer/testlogfiles/");
        this.testLogfileDirectory = new File(testLogfileUrl.getFile());

        this.receivedText = new StringBuilder(4096);

        doAnswer(invocation -> {
            ByteBuffer buffer = (ByteBuffer) invocation.getArguments()[0];
            byte[] contents = new byte[buffer.limit()];
            buffer.get(contents, 0, contents.length);
            receivedText.append(new String(contents, "UTF-8"));
            return null;
        }).when(remote).sendBytes(any());
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

    public void assertSendTextContains(String text) {
        assertThat(normalizeLineBreaks(getReceivedText().toString()))
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

    public void doSleep(int millis) throws InterruptedException {
        yield();
        sleep(millis);
    }

    public static String normalizeLineBreaks(String s) {
        return s.replaceAll("[\r\n]+", "\n");
    }
}
