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

package io.neba.spring.mvc;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestPathInfo;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.servlet.http.HttpServletMapping;
import javax.servlet.http.MappingMatch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.web.util.WebUtils.INCLUDE_REQUEST_URI_ATTRIBUTE;
import static org.springframework.web.util.WebUtils.INCLUDE_SERVLET_PATH_ATTRIBUTE;

/**
 * @author Olaf Otto
 */
@RunWith(MockitoJUnitRunner.class)
public class SlingMvcServletRequestTest {
    @Mock
    private SlingHttpServletRequest request;
    @Mock
    private RequestPathInfo requestPathInfo;

    private SlingMvcServletRequest testee;

    @Before
    public void setUp() {
        doReturn(this.requestPathInfo)
                .when(this.request)
                .getRequestPathInfo();

        doReturn("/bin/mvc")
                .when(this.requestPathInfo)
                .getResourcePath();

        doReturn("do")
                .when(this.requestPathInfo)
                .getExtension();

        doReturn("/controllerPath")
                .when(this.requestPathInfo)
                .getSuffix();

        this.testee = new SlingMvcServletRequest(this.request);
    }

    @Test
    public void testMvcRequestProvidesServletResourcePathAndExtensionAsServletPath() {
        assertThat(this.testee.getServletPath()).isEqualTo("/bin/mvc.do");
    }

    /**
     * By default, sling provides the original request's servlet path, even if
     * a completely different path was included. For instance, a request to
     * "/test.html" with a subsequent &lt;sling:include path="/other/path"&gt; will
     * yield the include path "".
     * <p>
     * Test that the {@link SlingMvcServletRequest} overrides this behavior, providing
     * the actually included servlet path. This allows including MVC resources, e.g.
     * &lt;sling:include path="/bin/mvc.do/controller"&gt;
     */
    @Test
    public void testIncludeServletPathIsSameAsServletPath() {
        assertThat(this.testee.getAttribute(INCLUDE_SERVLET_PATH_ATTRIBUTE))
                .isEqualTo("/bin/mvc.do");
    }

    /**
     * By default, sling provides the original request's URI, even if
     * a completely different URI was included. For instance, a request to
     * "/test.html" with a subsequent &lt;sling:include path="/servlet/path"&gt; will
     * yield the include URI path "/test.html".
     * <p>
     * Test that the {@link SlingMvcServletRequest} overrides this behavior, providing
     * the actually included URI. This allows including MVC resources, e.g.
     * &lt;sling:include path="/bin/mvc.do/controller"&gt;
     */
    @Test
    public void testIncludeContextPathIsSameAsServletPath() {
        assertThat(this.testee.getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE))
                .isEqualTo("/bin/mvc.do/controllerPath");
    }

    @Test
    public void testOtherAttributesAreFetchedFromTheOriginalRequest() {
        this.testee.getAttribute("other.attribute");
        verify(this.request).getAttribute("other.attribute");
    }

    @Test
    public void testNullMappingMatchIsPassedOn() {
        withNullMapping();
        assertThat(this.testee.getHttpServletMapping()).isNull();
    }

    @Test
    public void testExistingPathMappingIsPassedOn() {
        withExistingPathMapping();
        assertThat(this.testee.getHttpServletMapping()).isEqualTo(this.request.getHttpServletMapping());
    }

    @Test
    public void testNonPathMappingIsReplacedWithPathMapping() {
        withDefaultServletMapping();

        assertThat(this.testee.getHttpServletMapping().getMappingMatch()).isNotNull();
        assertThat(this.testee.getHttpServletMapping()).isNotEqualTo(this.request.getHttpServletMapping());
        assertThat(this.testee.getHttpServletMapping().getMappingMatch()).isEqualTo(MappingMatch.PATH);
        assertThat(this.testee.getHttpServletMapping().getMatchValue()).isEqualTo("/controllerPath");
        assertThat(this.testee.getHttpServletMapping().getPattern()).isEqualTo("/bin/mvc.do/*");
    }

    private void withDefaultServletMapping() {
        HttpServletMapping mapping = mock(HttpServletMapping.class);
        doReturn(MappingMatch.DEFAULT).when(mapping).getMappingMatch();
        doReturn(mapping).when(this.request).getHttpServletMapping();
    }

    private void withExistingPathMapping() {
        HttpServletMapping mapping = mock(HttpServletMapping.class);
        doReturn(MappingMatch.PATH).when(mapping).getMappingMatch();
        doReturn(mapping).when(this.request).getHttpServletMapping();
    }

    private void withNullMapping() {
        doReturn(null).when(this.request).getHttpServletMapping();
    }
}
