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
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.core.MethodParameter;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

/**
 * @author Olaf Otto
 */
public abstract class AbstractArgumentResolverTest<T extends HandlerMethodArgumentResolver> {
    @Mock
    private MethodParameter parameter;
    @Mock
    private NativeWebRequest nativeWebRequest;
    @Mock
    private SlingHttpServletRequest request;

    private Object argument;

    @Before
    public void setUp() throws Exception {
        doReturn(ResourceResolver.class).when(this.parameter).getParameterType();
        doReturn(this.request).when(this.nativeWebRequest).getNativeRequest();
    }

    @Test
    public void testUnsupportedArgumentType() throws Exception {
        withUnsupportedParameterType();
        assertParameterIsUnsupported();
    }

    public void withParameterType(Class<?> type) {
        doReturn(type).when(this.parameter).getParameterType();
    }

    public void resolveArguments() throws Exception {
        this.argument = getTestee().resolveArgument(this.parameter, null, this.nativeWebRequest, null);
    }

    public void assertResolvedArgumentIs(Object value) {
        assertThat(this.argument).isSameAs(value);
    }

    public SlingHttpServletRequest getRequest() {
        return request;
    }

    public abstract T getTestee();

    public void assertResolverSupports(Class<?> parameterType) {
        withParameterType(parameterType);
        assertParameterIsSupported();
    }

    private void assertParameterIsUnsupported() {
        assertThat(getTestee().supportsParameter(this.parameter)).isFalse();
    }

    private void assertParameterIsSupported() {
        assertThat(getTestee().supportsParameter(this.parameter)).isTrue();
    }

    private void withUnsupportedParameterType() {
        @SuppressWarnings("rawtypes")
		Class<? extends AbstractArgumentResolverTest> type = getClass();
        withParameterType(type);
    }
}
