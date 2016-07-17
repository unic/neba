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

package io.neba.core.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.context.ServletConfigAware;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.context.support.ServletContextAwareProcessor;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

/**
 * Enables {@link ServletContextAware} and {@link ServletConfigAware} for application contexts
 * created by the given {@link ConfigurableListableBeanFactory}.
 *
 * @author Olaf Otto
 */
@Service
public class ServletInfrastructureAwareConfigurer {
    @Autowired
    @Qualifier("servletConfig")
    private ServletConfig servletConfig;
    @Autowired
    private ServletContext servletContext;

    /**
     * @param factory must not be <code>null</code>.
     */
    public void enableServletContextAwareness(ConfigurableListableBeanFactory factory) {
        factory.addBeanPostProcessor(new ServletContextAwareProcessor(this.servletContext, this.servletConfig));
        factory.ignoreDependencyInterface(ServletContextAware.class);
        factory.ignoreDependencyInterface(ServletConfigAware.class);
    }
}
