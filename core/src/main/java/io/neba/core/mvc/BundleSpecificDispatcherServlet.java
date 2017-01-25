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

import io.neba.core.web.WebApplicationContextAdapter;
import org.apache.sling.api.servlets.ServletResolver;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.HandlerAdapter;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.handler.BeanNameUrlHandlerMapping;
import org.springframework.web.servlet.mvc.HttpRequestHandlerAdapter;
import org.springframework.web.servlet.mvc.annotation.ResponseStatusExceptionResolver;
import org.springframework.web.servlet.mvc.method.annotation.ExceptionHandlerExceptionResolver;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.servlet.mvc.support.DefaultHandlerExceptionResolver;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.springframework.beans.factory.BeanFactoryUtils.GENERATED_BEAN_NAME_SEPARATOR;

/**
 * Initializes the Spring MVC infrastructure, if required, and adds NEBA-specific customizations.
 * {@link DispatcherServlet#setPublishEvents(boolean) Disables event publication}
 * as event publication requires the presence of a {@link org.springframework.web.context.WebApplicationContext},
 * whereas an {@link org.eclipse.gemini.blueprint.context.support.OsgiBundleXmlApplicationContext} is
 * used by gemini-blueprint.
 *
 * @author Olaf Otto
 */
public class BundleSpecificDispatcherServlet extends DispatcherServlet implements ApplicationListener<ApplicationEvent> {
    private final ServletConfig servletConfig;
    private final ConfigurableListableBeanFactory factory;
    private final ServletResolver servletResolver;

    private boolean initialized = false;

    public BundleSpecificDispatcherServlet(ServletConfig servletConfig,
                                           ServletResolver servletResolver,
                                           ConfigurableListableBeanFactory factory) {

        super();
        if (servletConfig == null) {
            throw new IllegalArgumentException("Constructor parameter servletConfig must not be null");
        }
        if (factory == null) {
            throw new IllegalArgumentException("method parameter factory must not be null");
        }
        if (servletResolver == null) {
            throw new IllegalArgumentException("method parameter servletResolver must not be null");
        }

        this.servletConfig = servletConfig;
        this.factory = factory;
        this.servletResolver = servletResolver;

        setPublishEvents(true);
        setDispatchOptionsRequest(true);
        setDispatchTraceRequest(true);
    }

    /**
     * Configures the context's bean factory with MVC infrastructure beans.
     *
     * @param event can be <code>null</code>, in which case the event is ignored.
     */
    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof ContextRefreshedEvent) {
            synchronized (this) {
                ApplicationContext applicationContext = ((ContextRefreshedEvent) event).getApplicationContext();
                setApplicationContext(new WebApplicationContextAdapter(applicationContext, this.servletConfig.getServletContext()));

                // Configure the MVC infrastructure
                configureMultipartResolver();
                configureExceptionResolvers();
                configureHandlerAdapters();
                registerCustomArgumentResolvers();
                configureHandlerMappings();
                addNebaViewResolver();

                // Picks up the previously registered MVC infrastructure
                onRefresh(applicationContext);

                this.initialized = true;
            }
        }
    }

    /**
     * Registers the custom argument resolvers if a {@link RequestMappingHandlerAdapter}
     * is present in the factory.
     */
    private void registerCustomArgumentResolvers() {
        RequestMappingHandlerAdapter requestMappingHandlerAdapter = this.factory.getBean(RequestMappingHandlerAdapter.class);

        if (requestMappingHandlerAdapter != null) {
            List<HandlerMethodArgumentResolver> argumentResolvers = requestMappingHandlerAdapter.getArgumentResolvers();

            if (argumentResolvers == null) {
                throw new IllegalStateException("No argument resolvers found in " + requestMappingHandlerAdapter +
                        ". It appears the handler was not initialized by the application context.");
            }

            // Add Sling-specific argument resolvers first
            List<HandlerMethodArgumentResolver> resolvers = new LinkedList<>();
            resolvers.add(new RequestPathInfoArgumentResolver());
            resolvers.add(new ResourceResolverArgumentResolver());
            resolvers.add(new ResourceParamArgumentResolver());

            // Subsequently add all existing argument resolvers (they are order-sensitive, ending with a catch-all resolver,
            // thus the custom resolvers have to go first)
            resolvers.addAll(argumentResolvers);
            requestMappingHandlerAdapter.setArgumentResolvers(resolvers);
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
            defineBean(RequestMappingHandlerAdapter.class);
        }
    }

    /**
     * Discovers existing {@link org.springframework.web.servlet.HandlerExceptionResolver exception resolvers} in the provided
     * context. Provides the default resolvers (see original dispatcher servlet) in case no resolvers
     * exist in the context.
     */
    private void configureExceptionResolvers() {
        Map<String, HandlerExceptionResolver> resolvers = this.factory.getBeansOfType(HandlerExceptionResolver.class);
        DefaultHandlerExceptionResolver defaultResolver;

        if (resolvers.isEmpty()) {
            defineBean(ExceptionHandlerExceptionResolver.class);
            defineBean(ResponseStatusExceptionResolver.class);
            defaultResolver = defineBean(DefaultHandlerExceptionResolver.class);
        } else {
            defaultResolver = this.factory.getBean(DefaultHandlerExceptionResolver.class);
        }

        if (defaultResolver != null) {
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
            defineBean(RequestMappingHandlerMapping.class);
        }
    }

    private void addNebaViewResolver() {
        this.factory.registerSingleton(
                generateBeanNameFor(NebaViewResolver.class),
                new NebaViewResolver(this.servletResolver));
    }

    /**
     * Discovers existing {@link org.springframework.web.multipart.MultipartResolver multipart resolvers}
     * in the provided context. Provides the {@link io.neba.core.mvc.SlingMultipartResolver}
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

    private boolean hasBean(Class<?> type) {
        return !this.factory.getBeansOfType(type).isEmpty();
    }

    @Override
    public ServletConfig getServletConfig() {
        return this.servletConfig;
    }

    /**
     * @param request must not be <code>null</code>.
     */
    public boolean hasHandlerFor(HttpServletRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Method argument request must not be null.");
        }

        if (!this.initialized) {
            return false;
        }

        try {
            return super.getHandler(request) != null;
        } catch (Exception e) {
            throw new RuntimeException("Unable to lookup a handler for " + request + ".", e);
        }
    }
}
