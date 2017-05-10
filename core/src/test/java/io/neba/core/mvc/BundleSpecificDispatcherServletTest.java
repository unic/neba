/*
  Copyright 2013 the original author or authors.

  Licensed under the Apache License, Version 2.0 the "License";
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
 */

package io.neba.core.mvc;

import io.neba.core.web.WebApplicationContextAdapter;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.ServletResolver;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.servlet.HandlerAdapter;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.handler.BeanNameUrlHandlerMapping;
import org.springframework.web.servlet.mvc.HttpRequestHandlerAdapter;
import org.springframework.web.servlet.mvc.annotation.ResponseStatusExceptionResolver;
import org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.*;
import static org.springframework.web.servlet.DispatcherServlet.MULTIPART_RESOLVER_BEAN_NAME;

/**
 * @author Olaf Otto
 */
@RunWith(MockitoJUnitRunner.class)
public class BundleSpecificDispatcherServletTest {
    @Mock
    private ConfigurableListableBeanFactory factory;
    @Mock
    private ApplicationContext applicationContext;
    @Mock
    private ContextRefreshedEvent event;
    @Mock
    private ServletConfig servletConfig;
    @Mock
    private ServletResolver servletResolver;
    @Mock
    private SlingMvcServletRequest request;
    @Mock
    private SlingHttpServletResponse response;

    private List<?> registeredArgumentResolvers = new ArrayList<>();
    private HandlerMapping handlerMapping;

    private BundleSpecificDispatcherServlet testee;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws Exception {
        doThrow(new NoSuchBeanDefinitionException("THIS IS AN EXPECTED TEST EXCEPTION"))
                .when(this.applicationContext).getBean(anyString(), isA(Class.class));
        doReturn(this.applicationContext).when(this.event).getApplicationContext();
        doReturn(this.factory).when(this.applicationContext).getAutowireCapableBeanFactory();

        Answer<Object> createMock = invocation -> {
            final Class<?> beanType = (Class<?>) invocation.getArguments()[0];
            return mockExistingBean(beanType);
        };
        doAnswer(createMock).when(this.factory).createBean(isA(Class.class));

        this.testee = new BundleSpecificDispatcherServlet(this.servletConfig, this.servletResolver, this.factory);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testHasHandlerForRequiresNonNullRequest() throws Exception {
        this.testee.hasHandlerFor(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testHandlingOfNullFactoryInConstructor() throws Exception {
        new BundleSpecificDispatcherServlet(mock(ServletConfig.class), this.servletResolver, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testHandlingOfNullServletConfigInConstructor() throws Exception {
        new BundleSpecificDispatcherServlet(null, this.servletResolver, mock(ConfigurableListableBeanFactory.class));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testHandlingOfNullServletResolverInConstructor() throws Exception {
        new BundleSpecificDispatcherServlet(mock(ServletConfig.class), null, mock(ConfigurableListableBeanFactory.class));
    }

    @Test
    public void testApplicationContextIsProvidedAsWebApplicationContext() throws Exception {
        signalContextRefreshed();
        assertDispatcherServletIsInitializedWithWebApplicationContextAdapter();
    }

    @Test
    public void testWebApplicationContextDispatchesToOriginalApplicationContext() throws Exception {
        signalContextRefreshed();
        getBeanFromWebApplicationContext("anyBean");
        verifyBeanIsFetchedFromApplicationContext("anyBean");
    }

    private void verifyBeanIsFetchedFromApplicationContext(String beanName) {
        verify(this.applicationContext).getBean(beanName);
    }

    private void getBeanFromWebApplicationContext(String beanName) {
        this.testee.getWebApplicationContext().getBean(beanName);
    }

    @Test
    public void testProvisioningOfMvcInfrastructure() throws Exception {
        signalContextRefreshed();

        verifyMultipartResolverIsRegistered();
        verifyExceptionResolversAreRegistered();
        verifyHandlerAdaptersAreRegistered();
        verifyHandlerMappingsAreRegistered();
        verifyViewResolverIsRegistered();
    }

    @Test
    public void testInitializationIsIgnoredIfInfrastructureIsNotInitialized() throws Exception {
        verifyApplicationContextIsNotUsed();
    }

    @Test
    public void testInitializationIsPerformedWhenInfrastructureIsInitialized() throws Exception {
        signalContextRefreshed();
        verifyContextIsAskedFor(MULTIPART_RESOLVER_BEAN_NAME, MultipartResolver.class);
    }

    @Test
    public void testHandlingOfExistingMultipartResolver() throws Exception {
        withBeanAlreadyExistingInApplicationContext(MultipartResolver.class);

        signalContextRefreshed();
        verifyMultipartResolverIsNotRegistered();

        verifyExceptionResolversAreRegistered();
        verifyHandlerAdaptersAreRegistered();
        verifyHandlerMappingsAreRegistered();
        verifyViewResolverIsRegistered();
    }

    @Test
    public void testHandlingOfExistingExceptionResolverButNoDefaultResolver() throws Exception {
        withBeanAlreadyExistingInApplicationContext(HandlerExceptionResolver.class);

        signalContextRefreshed();
        verifyExceptionResolversAreNotRegistered();
        verifyDispatcherServletAttemptsToObtainDefaultResolver();

        verifyMultipartResolverIsRegistered();
        verifyHandlerAdaptersAreRegistered();
        verifyHandlerMappingsAreRegistered();
        verifyViewResolverIsRegistered();
    }

    @Test
    public void testHandlingOfExistingDefaultExceptionResolver() throws Exception {
        withBeanAlreadyExistingInApplicationContext(DefaultHandlerExceptionResolver.class);

        signalContextRefreshed();
        verifyExceptionResolversAreNotRegistered();
        verifyDispatcherServletAttemptsToObtainDefaultResolver();
        verifyDispatcherServletConfiguresWarnLogCategory("mvc");

        verifyMultipartResolverIsRegistered();
        verifyHandlerAdaptersAreRegistered();
        verifyHandlerMappingsAreRegistered();
        verifyViewResolverIsRegistered();
    }

    @Test
    public void testHandlingOfExistingRequestMappingHandlerAdapter() throws Exception {
        withBeanAlreadyExistingInApplicationContext(HandlerAdapter.class);
        withRequestMappingHandlerAlreadyExistingInContext();

        signalContextRefreshed();

        verifyHandlerAdaptersAreNotRegistered();
        verifyNebaArgumentResolversAreRegistered();
        verifyMultipartResolverIsRegistered();
        verifyExceptionResolversAreRegistered();
        verifyHandlerMappingsAreRegistered();
        verifyViewResolverIsRegistered();
    }

    @Test
    public void testHandlingOfExistingHandlerAdaptersWithoutRequestMappingHandlerAdapter() throws Exception {
        withBeanAlreadyExistingInApplicationContext(HandlerAdapter.class);

        signalContextRefreshed();

        verifyHandlerAdaptersAreNotRegistered();
        verifyNebaArgumentResolversAreNotRegistered();
        verifyMultipartResolverIsRegistered();
        verifyExceptionResolversAreRegistered();
        verifyHandlerMappingsAreRegistered();
        verifyViewResolverIsRegistered();
    }

    @Test
    public void testHandlingOfExistingHandlerMappings() throws Exception {
        withBeanAlreadyExistingInApplicationContext(HandlerMapping.class);

        signalContextRefreshed();

        verifyHandlerMappingsAreNotRegistered();

        verifyMultipartResolverIsRegistered();
        verifyExceptionResolversAreRegistered();
        verifyHandlerAdaptersAreRegistered();
        verifyViewResolverIsRegistered();
    }

    @Test
    public void testHandlingOfExistingViewResolver() throws Exception {
        withBeanAlreadyExistingInApplicationContext(ViewResolver.class);

        signalContextRefreshed();

        verifyViewResolverIsRegistered();

        verifyMultipartResolverIsRegistered();
        verifyExceptionResolversAreRegistered();
        verifyHandlerAdaptersAreRegistered();
        verifyHandlerMappingsAreRegistered();
    }

    @Test
    public void testRegistrationOfCustomArgumentResolvers() throws Exception {
        withRequestMappingHandlerCreatedOnDemand(mockRequestMappingHandler());

        signalContextRefreshed();

        verifyNebaArgumentResolversAreRegistered();
    }

    @Test(expected = IllegalStateException.class)
    public void testHandlingOfUninitializedArgumentsResolvers() throws Exception {
        RequestMappingHandlerAdapter adapter = mock(RequestMappingHandlerAdapter.class);
        doReturn(null).when(adapter).getArgumentResolvers();
        withRequestMappingHandlerCreatedOnDemand(adapter);

        signalContextRefreshed();
    }

    @Test
    public void testHandlingOfUnsupportedApplicationEvent() throws Exception {
        sendEvent(mock(ContextClosedEvent.class));
        verifyMvcContextIgnoresEvent();
    }

    @Test
    public void testOptionsRequestsArePassedToHandlers() throws Exception {
        withExistingHandlerMapping();
        signalContextRefreshed();

        withMethod("OPTIONS");
        service();

        verifyHandlerMappingIsUsedForRequest();
    }

    @Test
    public void testTraceRequestsArePassedToHandlers() throws Exception {
        withExistingHandlerMapping();
        signalContextRefreshed();

        withMethod("TRACE");
        // Mock expected response type to prevent the default trace behavior
        // from executing, which would require superfluous mocking
        withResponseContentType("message/http");
        service();

        verifyHandlerMappingIsUsedForRequest();
    }

    @Test
    public void testHasHandlerForIsAlwaysFalseWhenServletIsNotInitialized() throws Exception {
        withExistingHandlerMapping();

        assertServletHasNoHandlerForRequest();

        verifyHandlerMappingIsNotUsedForRequest();
    }

    @Test
    public void testHasHandlerForRequestChecksHandlerMappingsWhenServletIsInitialized() throws Exception {
        withExistingHandlerMapping();
        signalContextRefreshed();

        assertServletHasHandlerForRequest();

        verifyHandlerMappingIsUsedForRequest();
    }

    private void assertServletHasHandlerForRequest() {
        assertThat(this.testee.hasHandlerFor(this.request)).isTrue();
    }

    private void assertServletHasNoHandlerForRequest() {
        assertThat(this.testee.hasHandlerFor(this.request)).isFalse();
    }

    private void withResponseContentType(String type) {
        doReturn(type).when(this.response).getContentType();
    }

    private void verifyHandlerMappingIsUsedForRequest() throws Exception {
        verify(this.handlerMapping).getHandler(eq(this.request));
    }

    private void verifyHandlerMappingIsNotUsedForRequest() throws Exception {
        verify(this.handlerMapping, never()).getHandler(eq(this.request));
    }

    private void withExistingHandlerMapping() {
        this.handlerMapping = mockExistingBean(HandlerMapping.class);
    }

    private void service() throws ServletException, IOException {
        this.testee.service(this.request, this.response);
    }

    private void withMethod(String method) {
        doReturn(method).when(this.request).getMethod();
    }

    private void sendEvent(ApplicationEvent event) {
        this.testee.onApplicationEvent(event);
    }

    private void withRequestMappingHandlerAlreadyExistingInContext() {
        doReturn(mockRequestMappingHandler()).when(this.factory).getBean(eq(RequestMappingHandlerAdapter.class));
    }

    private void withRequestMappingHandlerCreatedOnDemand(final RequestMappingHandlerAdapter handler) {
        Answer<RequestMappingHandlerAdapter> mockBeanCreation = invocation -> {
            doReturn(handler).when(factory).getBean(eq(RequestMappingHandlerAdapter.class));
            return handler;
        };
        doAnswer(mockBeanCreation).when(this.factory).createBean(eq(RequestMappingHandlerAdapter.class));
    }

    @SuppressWarnings("unchecked")
    private RequestMappingHandlerAdapter mockRequestMappingHandler() {
        RequestMappingHandlerAdapter requestMappingHandlerAdapter = mock(RequestMappingHandlerAdapter.class);
        Answer<Object> verifyList = invocation -> {
            registeredArgumentResolvers = (List<?>) invocation.getArguments()[0];
            return null;
        };

        doAnswer(verifyList).when(requestMappingHandlerAdapter).setArgumentResolvers(anyList());

        return requestMappingHandlerAdapter;
    }

    private void verifyNebaArgumentResolversAreRegistered() {
        assertThat(this.registeredArgumentResolvers).describedAs("The list of registered NEBA argument resolvers").hasSize(3);
        assertThat(this.registeredArgumentResolvers.get(0)).isInstanceOf(RequestPathInfoArgumentResolver.class);
        assertThat(this.registeredArgumentResolvers.get(1)).isInstanceOf(ResourceResolverArgumentResolver.class);
        assertThat(this.registeredArgumentResolvers.get(2)).isInstanceOf(ResourceParamArgumentResolver.class);
    }

    private void verifyNebaArgumentResolversAreNotRegistered() {
        assertThat(this.registeredArgumentResolvers).describedAs("The list of registered NEBA argument resolvers").isEmpty();
    }

    private void verifyMvcContextIgnoresEvent() {
        verifyNoMoreInteractions(this.factory);
        verifyNoMoreInteractions(this.applicationContext);
    }

    private void withBeanAlreadyExistingInApplicationContext(Class<?> type) {
        mockExistingBean(type);
    }

    private void verifyContextIsAskedFor(String beanName, Class<MultipartResolver> beanType) {
        verify(this.applicationContext).getBean(eq(beanName), eq(beanType));
    }

    private void verifyViewResolverIsRegistered() {
        verify(this.factory).registerSingleton(anyString(), isA(NebaViewResolver.class));
    }

    private void verifyHandlerMappingsAreRegistered() {
        verifyContextDefinesBean(BeanNameUrlHandlerMapping.class);
        verifyContextDefinesBean(RequestMappingHandlerMapping.class);
    }

    private void verifyHandlerMappingsAreNotRegistered() {
        verifyBeanIsNeverCreatedInFactory(BeanNameUrlHandlerMapping.class);
        verifyBeanIsNeverCreatedInFactory(RequestMappingHandlerMapping.class);
    }

    private void verifyHandlerAdaptersAreRegistered() {
        verifyContextDefinesBean(HttpRequestHandlerAdapter.class);
        verifyContextDefinesBean(RequestMappingHandlerAdapter.class);
    }

    private void verifyHandlerAdaptersAreNotRegistered() {
        verifyBeanIsNeverCreatedInFactory(HttpRequestHandlerAdapter.class);
        verifyBeanIsNeverCreatedInFactory(RequestMappingHandlerAdapter.class);
    }

    private void verifyExceptionResolversAreRegistered() {
        verifyContextDefinesBean(ExceptionHandlerExceptionResolver.class);
        verifyContextDefinesBean(ResponseStatusExceptionResolver.class);
        verifyContextDefinesBean(DefaultHandlerExceptionResolver.class);
    }

    private void verifyExceptionResolversAreNotRegistered() {
        Class<?> type = ExceptionHandlerExceptionResolver.class;
        verifyBeanIsNeverCreatedInFactory(type);
        verifyBeanIsNeverCreatedInFactory(ResponseStatusExceptionResolver.class);
        verifyBeanIsNeverCreatedInFactory(DefaultHandlerExceptionResolver.class);
    }

    private void verifyMultipartResolverIsRegistered() {
        verifyContextDefinesBean(SlingMultipartResolver.class, MULTIPART_RESOLVER_BEAN_NAME);
    }

    private void verifyMultipartResolverIsNotRegistered() {
        verifyBeanIsNeverCreatedInFactory(SlingMultipartResolver.class);
    }

    private void verifyBeanIsNeverCreatedInFactory(Class<?> type) {
        verify(this.factory, never()).createBean(eq(type));
    }

    private void verifyContextDefinesBean(Class<?> type, String beanName) {
        verify(this.factory).createBean(type);
        verify(this.factory).registerSingleton(eq(beanName), isA(type));
    }

    private void verifyContextDefinesBean(Class<?> type) {
        verify(this.factory).createBean(type);
        verify(this.factory).registerSingleton(anyString(), isA(type));
    }

    private void verifyDispatcherServletAttemptsToObtainDefaultResolver() {
        verify(this.factory).getBean(eq(DefaultHandlerExceptionResolver.class));
    }

    private void verifyDispatcherServletConfiguresWarnLogCategory(String category) {
        verify(this.factory.getBean(DefaultHandlerExceptionResolver.class)).setWarnLogCategory(category);
    }

    private void verifyApplicationContextIsNotUsed() {
        verifyZeroInteractions(this.applicationContext);
    }

    private void signalContextRefreshed() {
        this.testee.onApplicationEvent((ApplicationEvent) this.event);
    }

    private void assertDispatcherServletIsInitializedWithWebApplicationContextAdapter() {
        assertThat(this.testee.getWebApplicationContext()).isInstanceOf(WebApplicationContextAdapter.class);
    }

    private <T> T mockExistingBean(final Class<T> beanType) {
        T bean = mock(beanType, Mockito.RETURNS_MOCKS);

        Map<String, Object> matchingBeans = new HashMap<>();
        matchingBeans.put("name", bean);

        ArgumentMatcher<Class<?>> isAssignableFromBeanType = new ArgumentMatcher<Class<?>>() {
            @Override
            public boolean matches(Object argument) {
                return ((Class<?>) argument).isAssignableFrom(beanType);
            }
        };

        doReturn(matchingBeans).when(applicationContext).getBeansOfType(argThat(isAssignableFromBeanType));
        doReturn(matchingBeans).when(applicationContext).getBeansOfType(argThat(isAssignableFromBeanType), anyBoolean(), anyBoolean());
        doReturn(bean).when(applicationContext).getBean(anyString(), argThat(isAssignableFromBeanType));

        @SuppressWarnings("unchecked")
        Map<String, Object> m = mock(Map.class);
        doReturn(false).when(m).isEmpty();
        doReturn(m).when(this.factory).getBeansOfType(argThat(isAssignableFromBeanType));
        doReturn(bean).when(this.factory).getBean(beanType);

        return bean;
    }
}
