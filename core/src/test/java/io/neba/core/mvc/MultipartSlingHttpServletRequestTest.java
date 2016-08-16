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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.util.MultiValueMap;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.Map.Entry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Olaf Otto
 */
@RunWith(MockitoJUnitRunner.class)
public class MultipartSlingHttpServletRequestTest {

	@Mock
	private SlingHttpServletRequest wrappedRequest;

	@Mock
	private RequestParameterMap parameterMap;

	private Set<Entry<String, RequestParameter[]>> parameterMapEntries;

	private MultipartSlingHttpServletRequest testee;

	@Before
	public void prepareMultipartRequest() {
		this.parameterMapEntries = new HashSet<>();
		when(this.wrappedRequest.getRequestParameterMap()).thenReturn(this.parameterMap);
		this.testee = new MultipartSlingHttpServletRequest(this.wrappedRequest);
	}

	@Test
	public void testFilenameExtraction() throws Exception {
		mockFileField("test", 1);
		mockFileField("junit", 2);
		assertExtractedFileNamesAre("test", "junit");
	}

	@Test
	public void testFileRetrieval() throws Exception {
		mockFileField("test", 1);
		assertRequestContainsFile("test");
		assertRequestDoesNotContainFile("test1");
	}

	@Test
	public void testRetrievalOfMultipleFiles() throws Exception {
		mockFileField("test", 4);
		assertRequestHasFiles("test", 4);
	}

	@Test
	public void testRetrievalOfFileMapWithSingleFiles() throws Exception {
		mockFileField("test1", 1);
		mockFileField("test2", 1);
		mockFileField("test3", 1);
		assertFileMapHasEntries("test1", "test2", "test3");
	}

	@Test
	public void testRetrievalOfFileMapWithMultipleFiles() throws Exception {
		mockFileField("test1", 1);
		mockFileField("test2", 1);
		mockFileField("test3", 1);
		assertMultiFileMapHasEntries("test1", "test2", "test3");
	}

	@Test
	public void testGetMultipartContentType() {
		String fileName = "test1";
		String contentType = "image/png";

		mockFileFieldWithContentType(fileName, contentType);

		String actual = this.testee.getMultipartContentType(fileName);
		assertThat(actual).isEqualTo(contentType);
	}

	@Test
	public void testGetMultipartHeaders() {
		String fileName = "test1";
		String contentType = "image/png";

		mockFileFieldWithContentType(fileName, contentType);

		HttpHeaders headers = this.testee.getMultipartHeaders(fileName);
		assertThat(headers.size()).isEqualTo(1);
		assertThat(headers.containsKey(MultipartSlingHttpServletRequest.CONTENT_TYPE)).isTrue();
		assertThat(headers.getFirst(MultipartSlingHttpServletRequest.CONTENT_TYPE)).isEqualTo(contentType);
	}

	@Test
	public void testGetRequestHeaders() {
		HttpHeaders expected = new HttpHeaders();
		expected.add("MyHeaderA", "value1a");
		expected.add("MyHeaderA", "value1b");
		expected.add("MyHeaderB", "value2");
		expected.add("MyHeaderC", "value3");

		mockHeaders(expected);
		HttpHeaders actual = this.testee.getRequestHeaders();

		assertThat(actual).isEqualTo(expected);
	}

	@Test
	public void testGetRequestMethod() {
		String expected = "GET";
		when(this.wrappedRequest.getMethod()).thenReturn(expected);

		HttpMethod actual = this.testee.getRequestMethod();
		assertThat(actual.name()).isEqualTo(expected);
	}

	private void assertMultiFileMapHasEntries(String... fileNames) {
		MultiValueMap<String, MultipartFile> multiFileMap = this.testee.getMultiFileMap();
		assertThat(multiFileMap).isNotNull();
		assertThat(multiFileMap.keySet()).containsOnly(fileNames);
		assertThat((Object) null).isNotIn(multiFileMap.values());
	}

	private void assertFileMapHasEntries(String... fileNames) {
		Map<String, MultipartFile> fileMap = this.testee.getFileMap();
		assertThat(fileMap).isNotNull();
		assertThat(fileMap.keySet()).containsOnly(fileNames);
		assertThat((Object) null).isNotIn(fileMap.values());
	}

	private void assertRequestHasFiles(String name, int expected) {
		List<MultipartFile> files = this.testee.getFiles(name);
		assertThat(files).isNotNull();
		assertThat(files).hasSize(expected);
	}

	private void assertRequestDoesNotContainFile(String name) {
		assertThat(this.testee.getFile(name)).isNull();
	}

	private void assertRequestContainsFile(String name) {
		MultipartFile file = this.testee.getFile(name);
		assertThat(file).isNotNull();
	}

	private void assertExtractedFileNamesAre(String... names) {
		assertThat(this.testee.getFileNames()).containsOnly(names);
	}

	@SuppressWarnings("unchecked")
	private void mockFileField(String fileFieldName, int fileParameters) {
		RequestParameter[] value = new RequestParameter[fileParameters];
		for (int i = 0; i < fileParameters; ++i) {
			value[i] = mock(RequestParameter.class);
			when(value[i].isFormField()).thenReturn(false);
		}
		Entry<String, RequestParameter[]> entry = mock(Entry.class);
		when(entry.getKey()).thenReturn(fileFieldName);
		when(entry.getValue()).thenReturn(value);
		this.parameterMapEntries.add(entry);
		when(this.parameterMap.entrySet()).thenReturn(this.parameterMapEntries);
		when(this.wrappedRequest.getRequestParameter(eq(fileFieldName))).thenReturn(value[0]);
		when(this.wrappedRequest.getRequestParameters(eq(fileFieldName))).thenReturn(value);
	}

	private void mockFileFieldWithContentType(String fileName, String contentType) {
		RequestParameter value = mock(RequestParameter.class);
		when(value.isFormField()).thenReturn(false);
		when(value.getContentType()).thenReturn(contentType);
		when(this.wrappedRequest.getRequestParameter(eq(fileName))).thenReturn(value);
	}

	@SuppressWarnings("rawtypes")
	private void mockHeaders(final HttpHeaders headers) {
		Enumeration headerNames = fromIterator(headers.keySet().iterator());

		when(this.wrappedRequest.getHeaderNames()).thenReturn(headerNames);

		for (Entry<String, List<String>> entry : headers.entrySet()) {
			Enumeration headerValues = fromIterator(entry.getValue().iterator());
			when(this.wrappedRequest.getHeaders(entry.getKey())).thenReturn(headerValues);
		}
	}

	@SuppressWarnings("rawtypes")
	private Enumeration fromIterator(final Iterator iterator) {
		return new Enumeration() {
			@Override
			public boolean hasMoreElements() {
				return iterator.hasNext();
			}

			@Override
			public Object nextElement() {
				return iterator.next();
			}
		};
	}
}
