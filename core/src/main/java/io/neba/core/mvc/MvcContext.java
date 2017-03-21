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

import org.apache.sling.api.SlingHttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.web.bind.support.WebArgumentResolver;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.HandlerAdapter;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.handler.BeanNameUrlHandlerMapping;
import org.springframework.web.servlet.mvc.HttpRequestHandlerAdapter;
import org.springframework.web.servlet.mvc.annotation.AnnotationMethodHandlerAdapter;
import org.springframework.web.servlet.mvc.annotation.AnnotationMethodHandlerExceptionResolver;
import org.springframework.web.servlet.mvc.annotation.DefaultAnnotationHandlerMapping;
import org.springframework.web.servlet.mvc.annotation.ResponseStatusExceptionResolver;
import org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Map;

import static org.springframework.beans.factory.BeanFactoryUtils.GENERATED_BEAN_NAME_SEPARATOR;
import static org.springframework.web.servlet.DispatcherServlet.MULTIPART_RESOLVER_BEAN_NAME;

/**
 * <p>
 * Configures a bundle-specific {@link ApplicationContext} with the Spring MVC infrastructure beans usually
 * provided to {@link org.springframework.web.context.WebApplicationContext web application contexts} by the
 * {@link DispatcherServlet}. Subsequently
 * {@link DispatcherServlet#initStrategies(org.springframework.context.ApplicationContext) initializes}
 * a context-specific {@link DispatcherServlet} instance with the so configured context. The resulting
 * dispatcher servlet is a fully-featured dispatcher servlet for the specific bundle, with the
 * exception of {@link DispatcherServlet#setPublishEvents(boolean) event publication}, which is disabled
 * as it requires a {@link org.springframework.web.context.WebApplicationContext}.
 * </p>
 * The configured context may provide custom MVC infrastructure beans, e.g.
 * by means of a <code>&lt;mvc:.../&gt;</code> XML configuration in their application context XML.
 * Like the {@link DispatcherServlet}, the {@link MvcContext} will only provide MVC infrastructure
 * beans if no suitable beans exist in the provided context.
 *
 * @author Olaf Otto
 */
public class MvcContext implements ApplicationListener<ApplicationEvent> {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private volatile boolean dispatcherServletInitialized = false;
    private volatile boolean mvcInfrastructureInitialized = false;
    private ApplicationContext context;

    /**
     * Weakens visibility restrictions of the original {@link DispatcherServlet}
     * and {@link DispatcherServlet#setPublishEvents(boolean) disables event publication}
     * as event publication requires the presence of a {@link org.springframework.web.context.WebApplicationContext},
     * whereas an {@link org.eclipse.gemini.blueprint.context.support.OsgiBundleXmlApplicationContext} is
     * used by gemini-blueprint.
     *
     * @author Olaf Otto
     */
	private static class ContextSpecificDispatcherServlet extends DispatcherServlet {
        private ServletConfig servletConfig;

        private ContextSpecificDispatcherServlet() {
            super();
            setPublishEvents(false);
            setDispatchOptionsRequest(true);
            setDispatchTraceRequest(true);
        }

        @Override
        public ServletConfig getServletConfig() {
            return this.servletConfig;
        }

        public void setServletConfig(ServletConfig servletConfig) {
            this.servletConfig = servletConfig;
        }

        @Override
        protected void initStrategies(ApplicationContext context) {
            super.initStrategies(context);
        }

        protected boolean hasHandlerFor(HttpServletRequest request) {
            try {
                return super.getHandler(request, false) != null;
            } catch (Exception e) {
                throw new RuntimeException("Unable to lookup a handler for " + request + ".", e);
            }
        }
    }

    private final ConfigurableListableBeanFactory factory;
    private final ContextSpecificDispatcherServlet dispatcherServlet;

    /**
     * @param factory must not be <code>null</code>.
     */
    public MvcContext(ConfigurableListableBeanFactory factory) {
        if (factory == null) {
            throw new IllegalArgumentException("Constructor parameter factory must not be null.");
        }

        this.dispatcherServlet = new ContextSpecificDispatcherServlet();
        this.factory = factory;
    }

    /**
     * Configures the context's bean factory with MVC infrastructure beans.
     *
     * @param event must not be <code>null</code>.
     */
    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof ContextRefreshedEvent) {
            synchronized (this) {
                this.context = ((ContextRefreshedEvent) event).getApplicationContext();

                // CHECKSTYLE:OFF

                try {
                    configureMultipartResolver();
                    configureExceptionResolvers();
                    configureHandlerAdapters();
                    registerCustomArgumentResolvers();
                    configureHandlerMappings();
                    configureViewResolvers();
                } catch (Throwable t) {
                    this.logger.error("Unable to initialize MVC infrastructure for context " + context + ".", t);
                    return;
                }
                // CHECKSTYLE:ON

                this.mvcInfrastructureInitialized = true;
            }
        }
    }

    /**
     * @param request  must not be <code>null</code>.
     *
     * @return <code>true</code> if this context has been
     *        {@link #onApplicationEvent(ApplicationEvent) initialized}
     *        and the {@link DispatcherServlet} contains a
     *        {@link DispatcherServlet#getHandler(javax.servlet.http.HttpServletRequest, boolean) handler for the request}.
     */
    public boolean isResponsibleFor(HttpServletRequest request) {
        return this.mvcInfrastructureInitialized && this.dispatcherServletInitialized && this.dispatcherServlet.hasHandlerFor(request);
    }

    /**
     * Delegates to the context specific {@link DispatcherServlet#service(javax.servlet.ServletRequest, javax.servlet.ServletResponse)}.
     * Must only be called if {@link #isResponsibleFor(javax.servlet.http.HttpServletRequest)} returns <code>true</code>.
     *
     * @param request must not be null.
     * @param response request must not be null.
     */
    public void service(SlingMvcServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {
        this.dispatcherServlet.service(request, response);
    }

    /**
     * Registers the custom argument resolvers if a {@link org.springframework.web.servlet.mvc.annotation.AnnotationMethodHandlerAdapter}
     * is present in the factory.
     */
    private void registerCustomArgumentResolvers() {
        AnnotationMethodHandlerAdapter requestMappingHandlerAdapter = this.factory.getBean(AnnotationMethodHandlerAdapter.class);

        if (requestMappingHandlerAdapter != null) {
            requestMappingHandlerAdapter.setCustomArgumentResolvers(new WebArgumentResolver[] {
                    new RequestPathInfoArgumentResolver(),
                    new ResourceResolverArgumentResolver(),
                    new ResourceParamArgumentResolver()
            });
        }
    }

    /**
     * Discovers existing {@link org.springframework.web.servlet.HandlerAdapter handler adapters} in the provided
     * context. Provides the default adapters (see original dispatcher servlet) in case no adapters
     * exist in the context.
     */
    private void configureHandlerAdapters() {
        Map<String, HandlerAdapter> handlerAdapters = this.factory.getBeansOfType(HandlerAdapter.class);
        if (handlerAdapters.isEmpty()) {
            defineBean(HttpRequestHandlerAdapter.class);
            defineBean(AnnotationMethodHandlerAdapter.class);
        }
    }

    /**
     * Discovers existing {@link org.springframework.web.servlet.HandlerExceptionResolver exception resolvers} in the provided
     * context. Provides the default resolvers (see original dispatcher servlet) in case no resolvers
     * exist in the context.
     */
    private void configureExceptionResolvers() {
        Map<String, HandlerExceptionResolver> resolvers = this.factory.getBeansOfType(HandlerExceptionResolver.class);
        if (resolvers.isEmpty()) {
            defineBean(AnnotationMethodHandlerExceptionResolver.class);
            defineBean(ResponseStatusExceptionResolver.class);
            DefaultHandlerExceptionResolver defaultResolver = defineBean(DefaultHandlerExceptionResolver.class);
            defaultResolver.setWarnLogCategory("mvc");
        }
    }

    /**
     * Discovers existing {@link org.springframework.web.servlet.HandlerMapping handler mappings} in the provided
     * context. Provides the default mappings (see original dispatcher servlet) in case no mappings
     * exist in the context.
     */
    private void configureHandlerMappings() {
        Map<String, HandlerMapping> handlerMappings = this.factory.getBeansOfType(HandlerMapping.class);
        if (handlerMappings.isEmpty()) {
            defineBean(BeanNameUrlHandlerMapping.class);
            defineBean(DefaultAnnotationHandlerMapping.class);
        }
    }

    /**
     * Discovers existing {@link org.springframework.web.servlet.ViewResolver view resolver} in the provided
     * context. Provides a special {@link NebaViewResolver} in case no resolver
     * exist in the context.
     */
    private void configureViewResolvers() {
        Map<String, ViewResolver> resolvers = this.factory.getBeansOfType(ViewResolver.class);
        if (resolvers.isEmpty()) {
            defineBean(NebaViewResolver.class);
        }
    }

    /**
     * Discovers existing {@link org.springframework.web.multipart.MultipartResolver multipart resolvers}
     * in the provided context. Provides a special {@link io.neba.core.mvc.SlingMultipartResolver}
     * in case no resolver exists in the context.
     */
    private void configureMultipartResolver() {
        if (!hasBean(MultipartResolver.class)) {
            defineBean(SlingMultipartResolver.class, MULTIPART_RESOLVER_BEAN_NAME);
        }
    }

    private String generateBeanNameFor(Class<?> type) {
        return type.getName() + GENERATED_BEAN_NAME_SEPARATOR + "0";
    }

    private <T> T defineBean(Class<T> type) {
        return defineBean(type, generateBeanNameFor(type));
    }

    private <T> T defineBean(Class<T> type, String beanName) {
        T bean = this.factory.createBean(type);
        this.factory.registerSingleton(beanName, bean);
        return bean;
    }

    public synchronized void initializeDispatcherServlet(ServletConfig config) {
        if (mustInitializeDispatcherServlet()) {
            this.dispatcherServlet.setServletConfig(config);
            this.dispatcherServlet.initStrategies(this.context);
            this.dispatcherServletInitialized = true;
        }
    }

    public boolean mustInitializeDispatcherServlet() {
        return !this.dispatcherServletInitialized && this.mvcInfrastructureInitialized;
    }

    private boolean hasBean(Class<?> type) {
        return !this.factory.getBeansOfType(type).isEmpty();
    }
}
