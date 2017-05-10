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

import org.apache.sling.api.SlingException;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.ServletResolver;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.FlashMapManager;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.InternalResourceView;
import org.springframework.web.servlet.view.InternalResourceViewResolver;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;

import static java.util.Locale.ENGLISH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.web.context.request.RequestContextHolder.resetRequestAttributes;
import static org.springframework.web.context.request.RequestContextHolder.setRequestAttributes;
import static org.springframework.web.servlet.DispatcherServlet.FLASH_MAP_MANAGER_ATTRIBUTE;

/**
 * @author Olaf Otto
 */
@RunWith(MockitoJUnitRunner.class)
public class NebaViewResolverTest {
    @Mock
    private SlingHttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private ResourceResolver resourceResolver;
    @Mock
    private RequestDispatcher dispatcher;
    @Mock
    private FlashMapManager flashMapManager;
    @Mock
    private ServletResolver servletResolver;

    private View resolvedView;

    @InjectMocks
    private NebaViewResolver testee;

    @Before
    public void setUp() throws Exception {
        doReturn("").when(this.request).getContextPath();
        doReturn(this.dispatcher).when(this.request).getRequestDispatcher(anyString());
        doReturn(this.flashMapManager).when(this.request).getAttribute(FLASH_MAP_MANAGER_ATTRIBUTE);
        doReturn(this.resourceResolver).when(this.request).getResourceResolver();

        Answer<Object> original = invocation -> invocation.getArguments()[0];
        doAnswer(original).when(this.response).encodeRedirectURL(anyString());
        setRequestAttributes(new ServletRequestAttributes(this.request));
    }

    @After
    public void tearDown() throws Exception {
        resetRequestAttributes();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorRequiresNonNullServletResolver() throws Exception {
        new NebaViewResolver(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullViewNamesAreNotTolerated() throws Exception {
        this.testee.resolveViewName(null, ENGLISH);
    }

    @Test
    public void testNullLocaleIstTolerated() throws Exception {
        this.testee.resolveViewName("viewName", null);
    }

    @Test
    public void testResolutionOfRedirectView() throws Exception {
        resolve("redirect:/test/me");
        assertViewHasType(RedirectView.class);

        renderView();
        verifyViewRedirectsTo("/test/me");
    }

    @Test
    public void testResolutionOfForwardView() throws Exception {
        resolve("forward:/test/me");
        assertViewHasType(InternalResourceView.class);

        renderView();
        verifyViewForwardsTo("/test/me");
    }

    /**
     * When a {@link org.springframework.web.servlet.ViewResolver} cannot provide a view, it must return null
     * to allow other view resolvers to provide a view.
     */
    @Test
    public void testUnresolvableViewYieldsNull() throws Exception {
        resolve("nonexistent/view");
        assertViewIsNull();
    }

    /**
     * When a controller returns a resource type that has a default script, such as
     * app/components/myComponents/myComponent.jsp, the view resolver shall provide
     * a view for this script.
     */
    @Test
    public void testResolutionOfSlingScriptWithoutInheritance() throws Exception {
        withDefaultScriptForType("app/components/myComponent");

        resolve("app/components/myComponent");

        verifyViewResolverResolvesScript("app/components/myComponent/myComponent");
        assertViewHasType(SlingServletView.class);
    }

    /**
     * When there is no default sling script for the resource type returned
     * by a controller, and the resource type has a {@link ResourceResolver#getParentResourceType(String) parent resource type},
     * the view resolver must attempt to resolve the default script for the super type.s
     */
    @Test
    public void testResolutionOfInheritedSlingScript() throws Exception {
        withSuperType("app/components/myComponent", "app/components/superType");
        withDefaultScriptForType("app/components/superType");

        resolve("app/components/myComponent");

        verifyViewResolverResolvesScript("app/components/myComponent/myComponent");
        verifyViewResolverResolvesScript("app/components/superType/superType");
        assertViewHasType(SlingServletView.class);
    }

    /**
     * The {@link ServletResolver} may throw a {@link SlingException} when a script cannot be resolved,
     * which must be tolerated and result in no view.
     */
    @Test
    public void testHandlingOfSlingExceptionDuringServletResolution() throws Exception {
        withExceptionDuringServletResolution();

        resolve("some/type");

        assertViewIsNull();
    }

    /**
     * View resolvers are  {@link org.springframework.core.Ordered}. NEBA's resource resolver shall be a fallback with higher
     * order than Spring's default {@link InternalResourceViewResolver} to override it but allow overriding by custom, higher-ranking
     * view resolvers.
     */
    @Test
    public void testNebaViewResolverOrderIsDirectlyAboveSpringsDefaultResolverOrder() throws Exception {
        assertThat(this.testee.getOrder()).isEqualTo(new InternalResourceViewResolver().getOrder() - 1);
    }

    private void withExceptionDuringServletResolution() {
        doThrow(mock(SlingException.class)).when(this.servletResolver).resolveServlet(eq(this.resourceResolver), anyString());
    }

    private void withSuperType(String resourceType, String resourceSuperType) {
        doReturn(resourceSuperType).when(this.resourceResolver).getParentResourceType(resourceType);
    }

    private void verifyViewResolverResolvesScript(String scriptPath) {
        verify(this.servletResolver).resolveServlet(this.resourceResolver, scriptPath);
    }

    private void withDefaultScriptForType(String resourceType) {
        Servlet script = mock(Servlet.class);
        doReturn(script)
                .when(this.servletResolver)
                .resolveServlet(
                        this.resourceResolver,
                        resourceType + resourceType.substring(resourceType.lastIndexOf('/')));
    }

    private void assertViewIsNull() {
        assertThat(this.resolvedView).isNull();
    }

    private void verifyViewForwardsTo(String url) throws ServletException, IOException {
        verify(this.request).getRequestDispatcher(eq(url));
        verify(this.dispatcher).forward(eq(this.request), eq(this.response));
    }

    private void verifyViewRedirectsTo(String value) throws IOException {
        verify(this.response).sendRedirect(eq(value));
    }

    private void renderView() throws Exception {
        this.resolvedView.render(new HashMap<>(), this.request, this.response);
    }

    private void assertViewHasType(Class<?> type) {
        assertThat(this.resolvedView).isInstanceOf(type);
    }

    private void resolve(String viewName) throws Exception {
        this.resolvedView = this.testee.resolveViewName(viewName, null);
    }
}
