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

package io.neba.core.blueprint;

import io.neba.core.mvc.MvcServlet;
import io.neba.core.resourcemodels.registration.ModelRegistrar;
import io.neba.core.selftests.SelfTestRegistrar;
import io.neba.core.spring.applicationcontext.configuration.PlaceholderVariableResolverRegistrar;
import io.neba.core.spring.applicationcontext.configuration.RequestScopeConfigurator;
import org.eclipse.gemini.blueprint.extender.OsgiBeanFactoryPostProcessor;
import org.osgi.framework.BundleContext;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;

/**
 * Post-processes {@link ConfigurableListableBeanFactory bean factories}
 * created by the gemini-blueprint-extender before they are initialized. 
 * Calls other services that must perform actions during post-processing of a bean factory
 * such as the {@link ModelRegistrar} and {@link PlaceholderVariableResolverRegistrar}.
 * 
 * @author Olaf Otto
 */
@Service
public class SlingBeanFactoryPostProcessor implements OsgiBeanFactoryPostProcessor {
    @Inject
    private PlaceholderVariableResolverRegistrar variableResolverRegistrar;
    @Inject
    private ModelRegistrar modelRegistrar;
    @Inject
    private SelfTestRegistrar selfTestRegistrar;
    @Inject
    private RequestScopeConfigurator requestScopeConfigurator;
    @Inject
    private MvcServlet dispatcherServlet;
    
    @Override
    public void postProcessBeanFactory(BundleContext bundleContext, ConfigurableListableBeanFactory beanFactory) {
        EventhandlingBarrier.begin();
        try {
            this.requestScopeConfigurator.registerRequestScope(beanFactory);
            this.variableResolverRegistrar.registerResolvers(bundleContext, beanFactory);
            this.modelRegistrar.registerModels(bundleContext, beanFactory);
            this.selfTestRegistrar.registerSelftests(beanFactory, bundleContext.getBundle());
            this.dispatcherServlet.enableMvc(beanFactory, bundleContext);
        } finally {
            EventhandlingBarrier.end();
        }
    }
}
