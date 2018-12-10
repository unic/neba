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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.osgi.framework.BundleContext;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;

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
    private SpringModelRegistrar modelRegistrar;
    @Mock
    private RequestScopeConfigurator requestScopeConfigurator;
    @Mock
    private MvcServlet dispatcherServlet;

    @InjectMocks
    private SlingBeanFactoryPostProcessor testee;

    @Test
    public void testOrderOfRegistrarInvocations() {
        postProcessBeanFactory();

        InOrder inOrder = inOrder(
                this.requestScopeConfigurator,
                this.modelRegistrar,
                this.dispatcherServlet);

        inOrder.verify(this.requestScopeConfigurator).registerRequestScope(eq(this.beanFactory));
        inOrder.verify(this.modelRegistrar).registerModels(eq(this.context), eq(this.beanFactory));
        inOrder.verify(this.dispatcherServlet).enableMvc(eq(this.beanFactory), eq(this.context));
    }

    private void postProcessBeanFactory() {
        this.testee.postProcessBeanFactory(this.context, this.beanFactory);
    }

}
