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
import org.springframework.core.Ordered;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.view.InternalResourceView;

import javax.servlet.Servlet;
import java.util.Locale;

import static java.lang.Integer.MAX_VALUE;
import static org.springframework.web.context.request.RequestContextHolder.getRequestAttributes;
import static org.springframework.web.servlet.view.UrlBasedViewResolver.FORWARD_URL_PREFIX;
import static org.springframework.web.servlet.view.UrlBasedViewResolver.REDIRECT_URL_PREFIX;

/**
 * Supports "redirect:" and "forward:" views and falls back to eh {@link SlingServletView} for a provided view name.
 *
 * @author Olaf Otto
 */
public class NebaViewResolver implements ViewResolver, Ordered {
    private final ServletResolver servletResolver;

    public NebaViewResolver(ServletResolver servletResolver) {
        this.servletResolver = servletResolver;
    }

    /**
     * Resolves a {@link View} from the provided view name.
     *
     * @param viewName must not be <code>null</code>.
     */
    @Override
    public View resolveViewName(String viewName, Locale locale) throws Exception {
        if (viewName == null) {
            throw new IllegalArgumentException("Method argument viewName must not be null.");
        }

        if (viewName.startsWith(REDIRECT_URL_PREFIX)) {
            String redirectUrl = viewName.substring(REDIRECT_URL_PREFIX.length());
            return new SlingRedirectView(redirectUrl, true, true);
        }

        if (viewName.startsWith(FORWARD_URL_PREFIX)) {
            String forwardUrl = viewName.substring(FORWARD_URL_PREFIX.length());
            return new InternalResourceView(forwardUrl);
        }

        return resolveScriptingView(viewName);
    }

    private SlingServletView resolveScriptingView(String resourceType) {
        final ResourceResolver resourceResolver = ((SlingHttpServletRequest) ((ServletRequestAttributes) getRequestAttributes()).getRequest()).getResourceResolver();

        // Support script inheritance by traversing the type hierarchy of the resource.
        // The type hierarchy is also traversed by the Servlet Resolver, but the resolver
        // only does this compliant to the Sling Script resolution when invoked with a HTTP request.
        // However, invocation with a request relies on an undocumented and unstable API contract,
        // depending, amongst others, on the HTTP method, request path, suffix, selector and extension
        // of the request path, which cannot be leveraged for controller responses.
        // Thus, the script is resolved explicitly.
        String currentResourceType = resourceType;

        do {
            int separatorPos = currentResourceType.lastIndexOf('/');
            if (separatorPos != -1) {
                // Since the view resolution is not sensitive to request state, resolve the default view name
                // for a resource type, e.g. "myType" in app/components/myType, for instance app/components/myType/myType.html
                String defaultScriptName = currentResourceType.substring(separatorPos);
                Servlet servlet = getServlet(resourceResolver, currentResourceType, defaultScriptName);
                if (servlet != null) {
                    return new SlingServletView(resourceType, servlet);
                }
            }
            currentResourceType = resourceResolver.getParentResourceType(currentResourceType);
        } while (currentResourceType != null);

        return null;
    }

    private Servlet getServlet(ResourceResolver resourceResolver, String type, String defaultScriptName) {
        try {
            return servletResolver.resolveServlet(resourceResolver, type + defaultScriptName);
        } catch (SlingException e) {
            // Ignore as this is expected for nonexistent scripts as per API contract
            return null;
        }
    }

    /**
     * Multiple view resolvers may coexist and are prioritized via implementation of {@link Ordered}.
     * Spring's {@link org.springframework.web.servlet.view.InternalResourceViewResolver} has the lowest priority
     * {@link Integer#MAX_VALUE}. This view resolver shall have a higher priority, but allow for any
     * custom view resolvers to override it.
     */
    @Override
    public int getOrder() {
        return MAX_VALUE - 1;
    }
}
