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
import org.apache.sling.api.wrappers.SlingHttpServletRequestWrapper;

import javax.servlet.http.HttpServletMapping;
import javax.servlet.http.MappingMatch;

import static org.apache.commons.lang3.StringUtils.removeStart;
import static org.springframework.web.util.WebUtils.INCLUDE_REQUEST_URI_ATTRIBUTE;
import static org.springframework.web.util.WebUtils.INCLUDE_SERVLET_PATH_ATTRIBUTE;

/**
 * Provides the actual path of the MVC servlet through {@link #getServletPath()} to enable
 * spring to determine the controller path of the request without having to use a custom
 * {@link org.springframework.web.util.UrlPathHelper}.
 *
 * @author Olaf Otto
 */
public class SlingMvcServletRequest extends SlingHttpServletRequestWrapper {
    public SlingMvcServletRequest(SlingHttpServletRequest request) {
        super(request);
    }

    @Override
    public Object getAttribute(String name) {
        if (INCLUDE_REQUEST_URI_ATTRIBUTE.equals(name)) {
            return getServletPath() + getRequestPathInfo().getSuffix();
        }
        if (INCLUDE_SERVLET_PATH_ATTRIBUTE.equals(name)) {
            return getServletPath();
        }
        return super.getAttribute(name);
    }


    @Override
    public HttpServletMapping getHttpServletMapping() {
        HttpServletMapping mapping = super.getHttpServletMapping();
        if (mapping == null) {
            return null;
        }
        if (MappingMatch.PATH.equals(mapping.getMappingMatch())) {
            return mapping;
        }

        // It's not a PATH mapping. This cannot be correct, since we know the MVC servlet is mapped to a path.
        // We therefore create a new mapping with the correct path.
        return new HttpServletMapping() {
            @Override
            public String getMatchValue() {
                return getRequestPathInfo().getSuffix();
            }

            @Override
            public String getPattern() {
                return getServletPath() + "/*";
            }

            @Override
            public String getServletName() {
                return "io.neba.spring.mvc.MvcServlet";
            }

            @Override
            public MappingMatch getMappingMatch() {
                return MappingMatch.PATH;
            }
        };
    }

    @Override
    public String getServletPath() {
        final RequestPathInfo pathInfo = getRequestPathInfo();
        return pathInfo.getResourcePath() + "." + pathInfo.getExtension();
    }
}
