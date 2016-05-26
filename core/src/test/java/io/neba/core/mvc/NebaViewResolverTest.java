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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.web.servlet.FlashMapManager;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.view.InternalResourceView;
import org.springframework.web.servlet.view.RedirectView;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.web.servlet.DispatcherServlet.FLASH_MAP_MANAGER_ATTRIBUTE;

/**
 * @author Olaf Otto
 */
@RunWith(MockitoJUnitRunner.class)
public class NebaViewResolverTest {
    @Mock
    private HttpServletRequest request;
    @Mock
    private RequestDispatcher dispatcher;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FlashMapManager flashMapManager;

    private View resolvedView;

    @InjectMocks
    private NebaViewResolver testee;

    @Before
    public void setUp() throws Exception {
        doReturn("").when(this.request).getContextPath();
        doReturn(this.dispatcher).when(this.request).getRequestDispatcher(anyString());
        doReturn(this.flashMapManager).when(this.request).getAttribute(FLASH_MAP_MANAGER_ATTRIBUTE);
        Answer<Object> original = invocation -> invocation.getArguments()[0];
        doAnswer(original).when(this.response).encodeRedirectURL(anyString());
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
