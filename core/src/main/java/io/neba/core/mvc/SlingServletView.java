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

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.SyntheticResource;
import org.apache.sling.api.wrappers.SlingHttpServletRequestWrapper;
import org.springframework.web.servlet.View;

import javax.servlet.Servlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * Resolves a resource path to a {@link Servlet} representing a {@link org.apache.sling.api.scripting.SlingScript}
 * and invokes the script to {@link #render(Map, HttpServletRequest, HttpServletResponse) render} the view.
 *
 * @author Olaf Otto
 */
public class SlingServletView implements View {
    private final String resourceType;
    private final Servlet servlet;

    /**
     * @param resourceType must not be <code>null</code>.
     * @param servlet must not be <code>null</code>.
     */
    public SlingServletView(String resourceType, Servlet servlet) {
        if (resourceType == null) {
            throw new IllegalArgumentException("Method argument resourceType must not be null.");
        }
        if (servlet == null) {
            throw new IllegalArgumentException("Method argument servlet must not be null.");
        }
        this.resourceType = resourceType;
        this.servlet = servlet;
    }

    @Override
    public String getContentType() {
        return null;
    }

    /**
     * @param model    can be <code>null</code>.
     * @param request  must not be <code>null</code>.
     * @param response must not be <code>null</code>.
     */
    @Override
    public void render(Map<String, ?> model, HttpServletRequest request, HttpServletResponse response) throws Exception {
        final SlingHttpServletRequest slingRequest = (SlingHttpServletRequest) request;
        final ResourceResolver resourceResolver = slingRequest.getResourceResolver();
        final String resourcePath = request.getPathInfo();

        final Resource resource = new SyntheticResource(resourceResolver, resourcePath, this.resourceType);
        final SlingHttpServletRequest wrapped = new MvcResourceRequest(slingRequest, resource);

        if (model != null) {
            for (Map.Entry<String, ?> entry : model.entrySet()) {
                wrapped.setAttribute(entry.getKey(), entry.getValue());
            }
        }

        servlet.service(wrapped, response);
    }

    /**
     * Wraps the original controller request to override the
     * {@link #getResource() request's resource}.
     *
     * @author Olaf Otto
     */
    static class MvcResourceRequest extends SlingHttpServletRequestWrapper {
        private final Resource resource;

        MvcResourceRequest(SlingHttpServletRequest slingRequest, Resource resource) {
            super(slingRequest);
            this.resource = resource;
        }

        @Override
        public Resource getResource() {
            return resource;
        }
    }
}