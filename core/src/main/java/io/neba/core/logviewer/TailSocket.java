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

import org.eclipse.jetty.websocket.api.WebSocketAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Math.round;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static java.util.regex.Pattern.compile;
import static org.apache.commons.lang3.math.NumberUtils.toFloat;

/**
 * Implements the tailing of logfiles provided by the {@link LogFiles}.
 *
 * @author Olaf Otto
 */
public class TailSocket extends WebSocketAdapter {
    private static final Pattern TAIL_COMMAND = compile("tail:(([0-9]+\\.)?[0-9]+)mb:(.+)");
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private ExecutorService executorService = newSingleThreadExecutor();

    private final LogFiles logFiles;
    private Tail tail;

    /**
     * @param logFiles must not be <code>null</code>.
     */
    TailSocket(LogFiles logFiles) {
        if (logFiles == null) {
            throw new IllegalArgumentException("Method argument logFiles must not be null.");
        }
        this.logFiles = logFiles;
    }

    @Override
    public void onWebSocketClose(int statusCode, String reason) {
        stopTail();
        this.executorService.shutdownNow();
        super.onWebSocketClose(statusCode, reason);
    }

    /**
     * @param message a tail command as specified by {@link #TAIL_COMMAND}.
     *                immediately sends the last <code>n</code> bytes of a specified
     *                log file, if present, and begins tailing the logfile thereafter.
     */
    @Override
    public void onWebSocketText(String message) {
        if (isPing(message)) {
            sendPong();
            return;
        }

        Matcher m = TAIL_COMMAND.matcher(message);

        if (!m.matches()) {
            logger.warn("Unsupported command format '" + message + "', must match " + TAIL_COMMAND.pattern() + ", ignoring the command.");
            return;
        }

        try {
            float including = toFloat(m.group(1));
            String path = m.group(m.groupCount());
            File file = resolveLogFile(path);

            if (file == null) {
                return;
            }

            long bytesToTail = round(including * 1024L * 1024L);

            tail(file, bytesToTail);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendPong() {
        getRemote().sendStringByFuture("pong");
    }

    private boolean isPing(String message) {
        return "ping".equals(message);
    }

    private void tail(File file, long bytesToTail) throws IOException {
        stopTail();
        this.tail = new Tail(getRemote(), file, bytesToTail);
        this.executorService.execute(this.tail);
    }

    private void stopTail() {
        if (this.tail != null) {
            synchronized (this.tail) {
                this.tail.stop();
                this.tail.notify();
                this.tail = null;
            }
        }
    }

    private File resolveLogFile(String path) throws IOException {
        return this.logFiles.resolveLogFiles()
                .stream()
                .filter(f -> f.getAbsolutePath().equals(path))
                .findFirst()
                .orElse(null);
    }

}
