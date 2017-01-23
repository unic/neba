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
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.ServletResolver;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.stereotype.Service;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static io.neba.core.util.BundleUtil.displayNameOf;
import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;
import static org.springframework.beans.factory.BeanFactoryUtils.GENERATED_BEAN_NAME_SEPARATOR;

/**
 * Dispatches controller requests to the bundle-specific {@link BundleSpecificDispatcherServlet dispatcher servlet}. The first
 * {@link BundleSpecificDispatcherServlet#hasHandlerFor(javax.servlet.http.HttpServletRequest)} responsible servlet} wins.
 * If no servlet is responsible, a 404 response is returned.<br />
 * {@link #enableMvc(org.springframework.beans.factory.config.ConfigurableListableBeanFactory, org.osgi.framework.BundleContext) Enables}
 * and {@link #disableMvc(org.osgi.framework.Bundle) disables} MVC capabilities in bundles
 * via the injection of the {@link BundleSpecificDispatcherServlet} into the {@link ConfigurableListableBeanFactory bean factory}
 * of the bundles.
 *
 * @author Olaf Otto
 * @see BundleSpecificDispatcherServlet
 */
@Service
public class MvcServlet extends SlingAllMethodsServlet {
    private final Map<Bundle, BundleSpecificDispatcherServlet> mvcCapableBundles = new ConcurrentHashMap<>();
    private final Logger logger = LoggerFactory.getLogger("mvc");

    @Autowired
    @Qualifier("servletConfig")
    private ServletConfig servletConfig;
    @Autowired
    private ServletContext servletContext;
    @Autowired
    private ServletResolver servletResolver;

    /**
     * Enables MVC capabilities in the given factory by injecting a {@link BundleSpecificDispatcherServlet}.
     *
     * @param factory must not be <code>null</code>.
     * @param context must not be <code>null</code>.
     */
    public void enableMvc(ConfigurableListableBeanFactory factory, BundleContext context) {
        if (factory == null) {
            throw new IllegalArgumentException("Method argument factory must not be null.");
        }
        if (context == null) {
            throw new IllegalArgumentException("Method argument context must not be null.");
        }

        final BundleSpecificDispatcherServlet dispatcherServlet = createBundleSpecificDispatcherServlet(factory, context);
        factory.registerSingleton(generateNameFor(BundleSpecificDispatcherServlet.class), dispatcherServlet);
        this.mvcCapableBundles.put(context.getBundle(), dispatcherServlet);
    }

    /**
     * Removes the {@link BundleSpecificDispatcherServlet} associated with the given bundle, if any.
     *
     * @param bundle must not be <code>null</code>.
     */
    public void disableMvc(Bundle bundle) {
        if (bundle == null) {
            throw new IllegalArgumentException("Method argument bundle must not be null.");
        }

        this.mvcCapableBundles.remove(bundle);
    }

    protected BundleSpecificDispatcherServlet createBundleSpecificDispatcherServlet(ConfigurableListableBeanFactory factory, BundleContext context) {
        BundleAwareServletConfig bundleAwareServletConfig = new BundleAwareServletConfig(context);
        return new BundleSpecificDispatcherServlet(bundleAwareServletConfig, this.servletResolver, factory);
    }

    @Override
    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {
        handle(request, response);
    }

    @Override
    protected void doPut(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {
        handle(request, response);
    }

    @Override
    protected void doDelete(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {
        handle(request, response);
    }

    @Override
    protected void doHead(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {
        handle(request, response);
    }

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {
        handle(request, response);
    }

    @Override
    protected void doOptions(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {
        handle(request, response);
    }

    @Override
    protected void doTrace(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {
        handle(request, response);
    }

    protected void handle(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {
        final SlingMvcServletRequest slingRequest = new SlingMvcServletRequest(request);

        for (BundleSpecificDispatcherServlet context : this.mvcCapableBundles.values()) {
            if (context.hasHandlerFor(slingRequest)) {
                context.service(slingRequest, response);
                return;
            }
        }

        if (this.logger.isDebugEnabled()) {
            this.logger.debug("No controller found for request " + request + ".");
        }

        response.sendError(SC_NOT_FOUND);
    }

    private String generateNameFor(Class<?> type) {
        return type.getSimpleName() + GENERATED_BEAN_NAME_SEPARATOR + "0";
    }

    /**
     * Improves the {@link #getServletName() naming} of the servlet configuration
     * to yield better error messages for the individually registered dispatcher servlets.
     *
     * @author Olaf Otto
     */
    private class BundleAwareServletConfig implements ServletConfig {
        private final BundleContext context;

        BundleAwareServletConfig(BundleContext context) {
            this.context = context;
        }

        @Override
        public String getServletName() {
            return BundleSpecificDispatcherServlet.class.getSimpleName() + " for bundle " + displayNameOf(context.getBundle());
        }

        @Override
        public ServletContext getServletContext() {
            return servletContext;
        }

        @Override
        public String getInitParameter(String s) {
            return servletConfig.getInitParameter(s);
        }

        @Override
        public Enumeration getInitParameterNames() {
            return servletConfig.getInitParameterNames();
        }
    }
}