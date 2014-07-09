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
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.stereotype.Service;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static javax.servlet.http.HttpServletResponse.SC_NOT_FOUND;

/**
 * Dispatches controller requests to the bundle-specific {@link MvcContext mvc contexts}. The first
 * {@link MvcContext#isResponsibleFor(javax.servlet.http.HttpServletRequest) responsible} context wins.
 * If no context is responsible, a 404 response is returned.<br />
 * {@link #enableMvc(org.springframework.beans.factory.config.ConfigurableListableBeanFactory, org.osgi.framework.BundleContext) Enables}
 * and {@link #disableMvc(org.osgi.framework.Bundle) disables} MVC capabilities in bundles
 * via the injection of the {@link MvcContext} into the {@link ConfigurableListableBeanFactory bean factories}
 * of the bundles.
 *
 * @see MvcContext
 *
 * @author Olaf Otto
 */
@Service
public class MvcServlet extends SlingAllMethodsServlet {
    private static final String MVC_ENABLER_BEAN_NAME = "_nebaMvcEnabler";
    private final Map<Bundle, MvcContext> mvcCapableBundles = new ConcurrentHashMap<Bundle, MvcContext>();
    private final Logger logger = LoggerFactory.getLogger("mvc");

    /**
     * Enables MVC capabilities in the given factory by injecting a {@link MvcContext} singleton.
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

        final MvcContext mvcContext = createMvcContext(factory);
        factory.registerSingleton(MVC_ENABLER_BEAN_NAME, mvcContext);
        this.mvcCapableBundles.put(context.getBundle(), mvcContext);
    }


    /**
     * Removes the {@link MvcContext} associated with the given bundle, if any.
     *
     * @param bundle must not be <code>null</code>.
     */
    public void disableMvc(Bundle bundle) {
        if (bundle == null) {
            throw new IllegalArgumentException("Method argument bundle must not be null.");
        }
        this.mvcCapableBundles.remove(bundle);
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

        MvcContext context = lookupResponsible(slingRequest);
        if (context != null) {
            context.service(slingRequest, response);
        } else {
            if (this.logger.isDebugEnabled()) {
                this.logger.debug("No controller found for request " + request + ".");
            }
            response.sendError(SC_NOT_FOUND);
        }
    }

    protected MvcContext createMvcContext(ConfigurableListableBeanFactory factory) {
        return new MvcContext(factory);
    }

    private MvcContext lookupResponsible(SlingHttpServletRequest request) {
        for (MvcContext context : this.mvcCapableBundles.values()) {
            if (context.mustInitializeDispatcherServlet()) {
                context.initializeDispatcherServlet(getServletConfig());
            }
            if (context.isResponsibleFor(request)) {
                return context;
            }
        }
        return null;
    }
}