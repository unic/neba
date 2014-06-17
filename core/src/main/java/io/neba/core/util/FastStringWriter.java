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

import java.io.IOException;
import java.io.Writer;

/**
 * Unlike {@link java.io.StringWriter}, this {@link Writer} does not rely on a synchronized
 * {@link StringBuffer} but on the efficient {@link StringBuilder}.<br />
 * It is important to {@link #FastStringWriter(int)} initialize this writer with
 * the suitable capacity (=amount of expected characters) in order to maintain performance.
 * 
 * @author Olaf Otto
 */
public class FastStringWriter extends Writer {
	private final StringBuilder builder;
	
	public FastStringWriter() {
		this(512);
	}
	
	public FastStringWriter(int initialCapacity) {
		builder =  new StringBuilder(initialCapacity);
		lock = builder;
	}

	@Override
	public Writer append(char c) throws IOException {
		builder.append(c);
		return this;
	}

	@Override
	public Writer append(CharSequence csq, int start, int end)
			throws IOException {
		builder.append(csq, start, end);
		return this;
	}

	@Override
	public Writer append(CharSequence csq) throws IOException {
		builder.append(csq);
		return this;
	}

	@Override
	public void write(char[] cbuf) throws IOException {
		builder.append(cbuf);
	}

	@Override
	public void write(int c) throws IOException {
		builder.append((char) c);
	}

	@Override
	public void write(String str, int off, int len) throws IOException {
		builder.append(str, off, len);
	}

	@Override
	public void write(String str) throws IOException {
		builder.append(str);
	}

	@Override
	public void close() throws IOException {
	}

	@Override
	public void flush() throws IOException {
	}

	@Override
	public void write(char[] cbuf, int off, int len) throws IOException {
		builder.append(cbuf, off, len);
	}

	public String toString() {
		return builder.toString();
	}
	
	/**
	 * @return The underlying builder of this writer, never <code>null</code>.
	 */
	public StringBuilder getBuilder() {
	    return this.builder;
	}
}
