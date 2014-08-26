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

import static org.apache.sling.api.resource.Resource.RESOURCE_TYPE_NON_EXISTING;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import io.neba.api.annotations.ResourceParam;

import javax.servlet.http.HttpServletRequest;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.ModelAndViewContainer;

/**
 * @author Olaf Otto
 */
@RunWith(MockitoJUnitRunner.class)
public class ResourceParamArgumentResolverTest {
    @Mock
    private HttpServletRequest nonSlingRequest;
    @Mock
    private SlingHttpServletRequest slingRequest;
    @Mock
    private ResourceResolver resolver;
    @Mock
    private Resource resource;
    @Mock
    private MethodParameter methodParameter;
    @Mock
    private ModelAndViewContainer container;
    @Mock
    private NativeWebRequest webRequest;
    @Mock
    private WebDataBinderFactory factory;
    @Mock
    private ResourceParam resourceParam;

    private Object resolvedArgument;

    @InjectMocks
    private ResourceParamArgumentResolver testee;


    @Before
    public void setUp() throws Exception {
        doReturn(this.resourceParam).when(this.methodParameter).getParameterAnnotation(eq(ResourceParam.class));
        doReturn(this.slingRequest).when(this.webRequest).getNativeRequest();
        doReturn(this.resolver).when(this.slingRequest).getResourceResolver();
        doReturn(getClass()).when(this.methodParameter).getParameterType();
    }

    @Test(expected = IllegalStateException.class)
    public void testHandlingOfUnexpectedRequestType() throws Exception {
        withNonSlingRequest();
        resolveArgument();
    }

    @Test(expected = MissingServletRequestParameterException.class)
    public void testHandlingOfMissingRequiredResourceParameter() throws Exception {
        withRequiredParameter();
        resolveArgument();
    }

    @Test
    public void testHandlingOfMissingOptionalResourceParameter() throws Exception {
        resolveArgument();
        assertResolvedArgumentIsNull();
    }

    @Test(expected = UnresolvableResourceException.class)
    public void testHandlingOfUnresolvableRequiredResource() throws Exception {
        withRequiredParameter();
        withResourceParameter("/junit/test/path");
        resolveArgument();
    }

    @Test(expected = UnresolvableResourceException.class)
    public void testHandlingOfNonExistingRequiredResource() throws Exception {
        withRequiredParameter();
        withResourceParameter("/junit/test/path");
        withResolvedResource();
        withResourceTypeNonExisting();
        resolveArgument();
    }

    @Test
    public void testHandlingOfUnresolvableOptionalResource() throws Exception {
        withResourceParameter("/junit/test/path");
        resolveArgument();
        assertResolvedArgumentIsNull();
    }

    @Test(expected = MissingAdapterException.class)
    public void testHandlingOfUnAdaptableRequiredType() throws Exception {
        withRequiredParameter();
        withResourceParameter("/junit/test/path");
        withResolvedResource();
        resolveArgument();
    }

    @Test
    public void testHandlingOfUnAdaptableOptionalType() throws Exception {
        withResourceParameter("/junit/test/path");
        withResolvedResource();
        resolveArgument();

        assertResolvedArgumentIsNull();
    }

    @Test
    public void testResolutionOfResourceParameter() throws Exception {
        withResourceParameter("/junit/test/path");
        withResolvedResource();
        withParameterType(Resource.class);
        resolveArgument();
        assertResourceIsResolvedAsArgument();
    }

    @Test
    public void testResolutionOfNonResourceParameter() throws Exception {
        withResourceParameter("/junit/test/path");
        withResolvedResource();
        withParameterType(ValueMap.class);
        withResourceAdaptingTo(ValueMap.class);
        resolveArgument();
        assertResolvedArgumentIsA(ValueMap.class);
    }

    @Test
    public void testResolverUsesParameterNameOfAnnotationByDefault() throws Exception {
        withResourceParamValue("parameterName");
        withDefaultParameterName("defaultName");
        resolveArgument();
        verifyResolverLooksUpParameter("parameterName");
    }

    @Test
    public void testResolverFallsBackToParameterNameIfAnnotationHasNoValue() throws Exception {
        withResourceParamValue("");
        withDefaultParameterName("defaultName");
        resolveArgument();
        verifyResolverLooksUpParameter("defaultName");
    }

    private void withDefaultParameterName(String parameterName) {
        doReturn(parameterName).when(this.methodParameter).getParameterName();
    }

    private void verifyResolverLooksUpParameter(String key) {
        verify(this.slingRequest).getParameter(eq(key));
    }

    private void withResourceParamValue(String parameterName) {
        doReturn(parameterName).when(this.resourceParam).value();
    }

    private void assertResolvedArgumentIsNull() {
        assertThat(this.resolvedArgument).isNull();
    }

    private void assertResolvedArgumentIsA(Class<?> type) {
        assertThat(this.resolvedArgument).isInstanceOf(type);
    }

    private void withResourceAdaptingTo(Class<?> type) {
        doReturn(mock(type)).when(this.resource).adaptTo(eq(type));
    }

    private void assertResourceIsResolvedAsArgument() {
        assertThat(this.resolvedArgument).isSameAs(this.resource);
    }

    private void withParameterType(Class<?> parameterType) {
        doReturn(parameterType).when(this.methodParameter).getParameterType();
    }

    private void withResourceParameter(String resourcePath) {
        doReturn(resourcePath).when(this.slingRequest).getParameter(anyString());
    }

    private void withResolvedResource() {
        doReturn(this.resource).when(this.resolver).resolve(any(HttpServletRequest.class), anyString());
    }

    private void withRequiredParameter() {
        doReturn(true).when(this.resourceParam).required();
    }

    private void resolveArgument() throws Exception {
        this.resolvedArgument = testee.resolveArgument(this.methodParameter, this.container, this.webRequest, this.factory);
    }

    private void withNonSlingRequest() {
        doReturn(this.nonSlingRequest).when(this.webRequest).getNativeRequest();
    }

    private void withResourceTypeNonExisting() {
        doReturn(RESOURCE_TYPE_NON_EXISTING).when(this.resource).getResourceType();
    }
}
