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
import org.apache.sling.api.SlingHttpServletResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.*;

/**
 * @author Olaf Otto
 */
@RunWith(MockitoJUnitRunner.class)
public class MvcServletTest {
    @Mock
    private ConfigurableListableBeanFactory factory;
    @Mock
    private BundleContext context;
    @Mock
    private Bundle bundle;
    @Mock
    private MvcContext mvcContext;
    @Mock
    private SlingHttpServletRequest request;
    @Mock
    private SlingHttpServletResponse response;
    @Mock
    private ServletConfig servletConfig;

    private MvcContext injectedContext;

    @InjectMocks
    @Spy
    private MvcServlet testee;

    @Before
    public void setUp() throws Exception {
        Answer<Object> retainMvcContext = invocation -> {
            injectedContext = (MvcContext) invocation.getArguments()[1];
            return null;
        };
        doAnswer(retainMvcContext).when(this.factory).registerSingleton(anyString(), isA(MvcContext.class));
        doReturn(this.bundle).when(this.context).getBundle();
        doReturn(this.mvcContext).when(this.testee).createMvcContext(isA(ConfigurableListableBeanFactory.class));
    }

    @Test
    public void testEnableMvc() throws Exception {
        enableMvc();
        verifyMvcContextIsInjectedIntoFactory();
        assertInjectMvcContextIsNotNull();
    }

    @Test
    public void testOnlyResponsibleContextsServiceRequests() throws Exception {
        enableMvc();

        handleRequest();
        verifyMvcContextDoesNotServiceRequest();

        withMvcContextResponsible();

        handleRequest();
        verifyMvcContextServicedRequestOnce();
    }

    @Test
    public void testRemovalOfMvcCapabilities() throws Exception {
        enableMvc();
        withMvcContextResponsible();
        handleRequest();
        verifyMvcContextServicedRequestOnce();

        disableMvc();

        handleRequest();
        verifyMvcContextServicedRequestOnce();
    }

    @Test
    public void testLazyInitializationOfDispatcherServlet() throws Exception {
        enableMvc();
        withDispatcherServletInitializationPending();

        handleRequest();

        verifyDispatcherServletIsInitializedWithServletConfig();
    }

    @Test
    public void testDispatcherServletIsNotInitializedWhenAlreadyInitialized() throws Exception {
        enableMvc();

        handleRequest();

        verifyDispatcherServletIsNotInitializedWithServletConfig();
    }

    private void verifyDispatcherServletIsNotInitializedWithServletConfig() {
        verify(this.mvcContext, never()).initializeDispatcherServlet(this.servletConfig);
    }

    private void verifyDispatcherServletIsInitializedWithServletConfig() {
        verify(this.mvcContext).initializeDispatcherServlet(this.servletConfig);
    }

    private void withDispatcherServletInitializationPending() {
        doReturn(true).when(this.mvcContext).mustInitializeDispatcherServlet();
    }

    private void disableMvc() {
        this.testee.disableMvc(this.bundle);
    }

    private void verifyMvcContextServicedRequestOnce() throws ServletException, IOException {
        verify(this.mvcContext).service(isA(SlingMvcServletRequest.class), isA(SlingHttpServletResponse.class));
    }

    private void withMvcContextResponsible() {
        doReturn(true).when(this.mvcContext).isResponsibleFor(isA(HttpServletRequest.class));
    }

    private void verifyMvcContextDoesNotServiceRequest() throws ServletException, IOException {
        verify(this.mvcContext, never()).service(isA(SlingMvcServletRequest.class), isA(SlingHttpServletResponse.class));
    }

    private void handleRequest() throws ServletException, IOException {
        this.testee.handle(this.request, this.response);
    }

    private void assertInjectMvcContextIsNotNull() {
        assertThat(this.injectedContext).isNotNull();
    }

    private void enableMvc() {
        this.testee.enableMvc(this.factory, this.context);
    }

    private void verifyMvcContextIsInjectedIntoFactory() {
        verify(this.factory).registerSingleton(anyString(), isA(MvcContext.class));
    }
}
