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
import io.neba.core.placeholdervariables.PlaceholderVariableResolverRegistrar;
import io.neba.core.resourcemodels.registration.ModelRegistrar;
import io.neba.core.web.RequestScopeConfigurator;
import io.neba.core.web.ServletInfrastructureAwareConfigurer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

/**
 * @author Olaf Otto
 */
@RunWith(MockitoJUnitRunner.class)
public class SlingBeanFactoryPostProcessorTest {
	@Mock
    private BundleContext context;
	@Mock
    private ConfigurableListableBeanFactory beanFactory;
    @Mock
    private ModelRegistrar modelRegistrar;
    @Mock
    private PlaceholderVariableResolverRegistrar variableResolver;
    @Mock
    private RequestScopeConfigurator requestScopeConfigurator;
    @Mock
    private ServletInfrastructureAwareConfigurer servletInfrastructureAwareConfigurer;
    @Mock
    private Bundle bundle;
    @Mock
    private MvcServlet dispatcherServlet;
    
    @InjectMocks
    private SlingBeanFactoryPostProcessor testee;

    @Before
    public void prepareBundleContext() {
        when(this.context.getBundle()).thenReturn(this.bundle);
    }

    @Test
    public void testOrderOfRegistrarInvocations() throws Exception {
        postProcessBeanFactory();
        
        InOrder inOrder = inOrder(
    		this.requestScopeConfigurator,
            this.servletInfrastructureAwareConfigurer,
    		this.variableResolver, 
    		this.modelRegistrar, 
        	this.dispatcherServlet);
        
        inOrder.verify(this.requestScopeConfigurator).registerRequestScope(eq(this.beanFactory));
        inOrder.verify(this.servletInfrastructureAwareConfigurer).enableServletContextAwareness(eq(this.beanFactory));
        inOrder.verify(this.variableResolver).registerResolvers(eq(this.context), eq(this.beanFactory));
        inOrder.verify(this.modelRegistrar).registerModels(eq(this.context), eq(this.beanFactory));
        inOrder.verify(this.dispatcherServlet).enableMvc(eq(this.beanFactory), eq(this.context));
    }

    private void postProcessBeanFactory() {
        this.testee.postProcessBeanFactory(this.context, this.beanFactory);
    }

}
