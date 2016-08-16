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

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.request.RequestParameterMap;
import org.apache.sling.api.wrappers.SlingHttpServletRequestWrapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import java.util.*;
import java.util.Map.Entry;

import static java.util.Collections.list;

/**
 * Translates sling's build-in support of multipart file posts
 * to Spring's {@link org.springframework.web.multipart.MultipartRequest}.
 * 
 * @author Olaf Otto
 */
public class MultipartSlingHttpServletRequest extends SlingHttpServletRequestWrapper implements
		MultipartHttpServletRequest {

	static final String CONTENT_TYPE = "Content-Type";

	public MultipartSlingHttpServletRequest(SlingHttpServletRequest wrappedRequest) {
		super(wrappedRequest);
	}

	@Override
	public Iterator<String> getFileNames() {
		RequestParameterMap requestParameterMap = getRequestParameterMap();
		List<String> names = new ArrayList<>(requestParameterMap.size());
		for (Entry<String, RequestParameter[]> entry : requestParameterMap.entrySet()) {
			RequestParameter[] params = entry.getValue();
			if (params != null && params.length > 0 && !params[0].isFormField()) {
				names.add(entry.getKey());
			}
		}
		return names.iterator();
	}

	@Override
	public MultipartFile getFile(String name) {
		final RequestParameter requestParameter = getRequestParameter(name);
		MultipartFile file = null;
		if (requestParameter != null && !requestParameter.isFormField()) {
			file = new SlingMultipartFile(name, requestParameter);
		}
		return file;
	}

	@Override
	public List<MultipartFile> getFiles(String name) {
		final RequestParameter[] requestParameters = getRequestParameters(name);
		List<MultipartFile> files = new ArrayList<>();
		if (requestParameters != null) {
			for (RequestParameter parameter : requestParameters) {
				if (!parameter.isFormField()) {
					files.add(new SlingMultipartFile(name, parameter));
				}
			}
		}
		return files;
	}

	@Override
	public Map<String, MultipartFile> getFileMap() {
		RequestParameterMap requestParameterMap = getRequestParameterMap();
		Map<String, MultipartFile> files = new HashMap<>(requestParameterMap.size());
		for (Entry<String, RequestParameter[]> entry : requestParameterMap.entrySet()) {
			RequestParameter[] params = entry.getValue();
			if (params != null && params.length > 0) {
				for (RequestParameter parameter : params) {
					if (!parameter.isFormField()) {
						files.put(entry.getKey(), new SlingMultipartFile(entry.getKey(), parameter));
					}
				}
			}
		}
		return files;
	}

	@Override
	public MultiValueMap<String, MultipartFile> getMultiFileMap() {
		RequestParameterMap requestParameterMap = getRequestParameterMap();
		MultiValueMap<String, MultipartFile> fileMap = new LinkedMultiValueMap<>(
				requestParameterMap.size());
		for (Entry<String, RequestParameter[]> entry : requestParameterMap.entrySet()) {
			RequestParameter[] params = entry.getValue();
			if (params != null && params.length > 0) {
				List<MultipartFile> files = new ArrayList<>(params.length);
				for (RequestParameter parameter : params) {
					if (!parameter.isFormField()) {
						files.add(new SlingMultipartFile(entry.getKey(), parameter));
					}
				}
				if (!files.isEmpty()) {
					fileMap.put(entry.getKey(), files);
				}
			}
		}
		return fileMap;
	}

	@Override
	public String getMultipartContentType(String name) {
		final RequestParameter requestParameter = getRequestParameter(name);
		String contentType = null;
		if (requestParameter != null && !requestParameter.isFormField()) {
			contentType = requestParameter.getContentType();
		}
		return contentType;
	}

	@Override
	public HttpHeaders getMultipartHeaders(String name) {
		String contentType = getMultipartContentType(name);
		if (contentType != null) {
			HttpHeaders headers = new HttpHeaders();
			headers.add(CONTENT_TYPE, contentType);
			return headers;
		} else {
			return null;
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public HttpHeaders getRequestHeaders() {
		HttpHeaders headers = new HttpHeaders();
		Enumeration<String> headerNames = getHeaderNames();
		while (headerNames.hasMoreElements()) {
			String headerName = headerNames.nextElement();
			headers.put(headerName, list(getHeaders(headerName)));
		}
		return headers;
	}

	@Override
	public HttpMethod getRequestMethod() {
		return HttpMethod.valueOf(getSlingRequest().getMethod());
	}
}
