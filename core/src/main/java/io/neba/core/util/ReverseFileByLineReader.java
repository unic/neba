/*
 * (C) Copyright 2013 the original author or authors. (http://www.neba.io/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Taschek Joerg (Original Author)
 *     Olaf Otto (Refactored, adapted to NEBA codestyle, added testcase)
 */
package io.neba.core.util;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Reads text files in reverse using an estimate of bytes per line.
 *
 * @author <a href="mailto:behaveu@gmail.com">Taschek Joerg</a>
 * @author Olaf Otto
 */
public class ReverseFileByLineReader {
    private RandomAccessFile file = null;
    private int approxBytesPerLine = 0;
    private long lastPosition = -1;
    private boolean fileStartReached = false;

    /**
     * @param file               must not be <code>null</code>.
     * @param approxBytesPerLine an estimate of the bytes per line.
     * @throws IOException if the file cannot be read.
     */
    public ReverseFileByLineReader(File file, int approxBytesPerLine) throws IOException {
        if (file == null) {
            throw new IllegalArgumentException("Method argument file must not be null.");
        }
        this.file = new RandomAccessFile(file, "r");
        this.approxBytesPerLine = approxBytesPerLine;
        this.lastPosition = this.file.length();
    }

    public String readPreviousLine() throws IOException {
        if (this.fileStartReached) {
            return null;
        }
        String line = null;
        boolean lineRead = false;
        int count = 0;
        while (!lineRead) {
            count++;
            int byteReads = approxBytesPerLine * count;
            while (this.lastPosition - byteReads < 0) {
                --byteReads;
            }
            this.file.seek(this.lastPosition - byteReads);
            byte buf[] = new byte[byteReads];
            int read = this.file.read(buf, 0, buf.length);
            String tmp = new String(buf, 0, read);
            int position = -1;

            if (tmp.contains("\r\n")) {
                // windows linebreak
                position = tmp.lastIndexOf("\r\n") + 2;
                lineRead = true;
            } else if (tmp.contains("\n")) {
                // linux, mac os X linebreak
                position = tmp.lastIndexOf('\n') + 1;
                lineRead = true;
            } else if (lastPosition - read <= 0) {
                line = tmp;
                lineRead = true;
                this.fileStartReached = true;
            }

            if (lineRead && position != -1) {
                this.lastPosition = lastPosition - (byteReads - position + 1);
                line = tmp.substring(position);
            }
        }
        return line;
    }

    public void close() throws IOException {
        file.close();
    }
}
