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

package io.neba.core.mvc;

import org.apache.sling.api.request.RequestParameter;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;

import static org.apache.commons.io.IOUtils.closeQuietly;
import static org.apache.commons.io.IOUtils.copy;

/**
 * Uses the {@link org.apache.sling.api.SlingHttpServletRequest} {@link RequestParameter} API
 * to provide a {@link MultipartFile}.
 *  
 * @author Olaf Otto
 */
public class SlingMultipartFile implements MultipartFile {
	private final RequestParameter source;
	private final String name;

	public SlingMultipartFile(String paramName, RequestParameter requestParameter) {
		this.source = requestParameter;
		this.name = paramName;
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public String getOriginalFilename() {
		return this.source.getFileName();
	}

	@Override
	public String getContentType() {
		return this.source.getContentType();
	}

	@Override
	public boolean isEmpty() {
		return this.source.getSize() == 0L;
	}

	@Override
	public long getSize() {
		return this.source.getSize();
	}

	@Override
	public byte[] getBytes() throws IOException {
		return this.source.get();
	}

	@Override
	public InputStream getInputStream() throws IOException {
		return this.source.getInputStream();
	}

	@Override
	public void transferTo(File dest) throws IOException, IllegalStateException {
		if (dest.exists() && !dest.delete()) {
			throw new IOException(
					"Destination file [" + dest.getAbsolutePath() + "] already exists and could not be deleted");
		}
		final InputStream in = getInputStream();
		final OutputStream out = new BufferedOutputStream(new FileOutputStream(dest));
		try {
            copy(in, out);
            out.flush();
		} finally {
            closeQuietly(out);
		    closeQuietly(in);
		}
	}
}
