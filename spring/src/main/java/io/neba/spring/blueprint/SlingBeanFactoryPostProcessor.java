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

package io.neba.spring.blueprint;

import io.neba.spring.mvc.MvcServlet;
import io.neba.spring.resourcemodels.registration.SpringModelRegistrar;
import io.neba.spring.web.RequestScopeConfigurator;
import io.neba.spring.web.ServletInfrastructureAwareConfigurer;
import org.eclipse.gemini.blueprint.extender.OsgiBeanFactoryPostProcessor;
import org.osgi.framework.BundleContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.stereotype.Service;

/**
 * Post-processes {@link ConfigurableListableBeanFactory bean factories}
 * created by the gemini-blueprint-extender before they are initialized.
 * Calls other services that must perform actions during post-processing of a bean factory
 * such as the {@link SpringModelRegistrar}.
 *
 * @author Olaf Otto
 */
@Service
public class SlingBeanFactoryPostProcessor implements OsgiBeanFactoryPostProcessor {
    @Autowired
    private SpringModelRegistrar springModelRegistrar;
    @Autowired
    private RequestScopeConfigurator requestScopeConfigurator;
    @Autowired
    private MvcServlet dispatcherServlet;
    @Autowired
    private ServletInfrastructureAwareConfigurer servletInfrastructureAwareConfigurer;

    @Override
    public void postProcessBeanFactory(BundleContext bundleContext, ConfigurableListableBeanFactory factory) {
        this.requestScopeConfigurator.registerRequestScope(factory);
        this.servletInfrastructureAwareConfigurer.enableServletContextAwareness(factory);
        this.springModelRegistrar.registerModels(bundleContext, factory);
        this.dispatcherServlet.enableMvc(factory, bundleContext);
    }
}
