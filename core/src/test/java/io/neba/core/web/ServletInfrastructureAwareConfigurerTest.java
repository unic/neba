/**
 * Copyright 2013 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 the "License";
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/
package io.neba.core.web;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.web.context.ServletConfigAware;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.context.support.ServletContextAwareProcessor;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.verify;

/**
 * @author Olaf Otto
 */
@RunWith(MockitoJUnitRunner.class)
public class ServletInfrastructureAwareConfigurerTest {
    @Mock
    private ServletConfig servletConfig;
    @Mock
    private ServletContext servletContext;
    @Mock
    private ConfigurableListableBeanFactory factory;

    @InjectMocks
    private ServletInfrastructureAwareConfigurer testee;

    @Test
    public void testAdditionOfServletContextAwarePostProcessor() throws Exception {
        configure();

        verifyServletContextAwarePostProcessorIsAddedToFactory();
        verifyServletConfigAwareIsIgnoredDependency();
        verifyServletContextAwareIsIgnoredDependency();
    }

    private void verifyServletContextAwareIsIgnoredDependency() {
        verify(this.factory).ignoreDependencyInterface(ServletContextAware.class);
    }

    private void verifyServletConfigAwareIsIgnoredDependency() {
        verify(this.factory).ignoreDependencyInterface(ServletConfigAware.class);
    }

    private void verifyServletContextAwarePostProcessorIsAddedToFactory() {
        verify(this.factory).addBeanPostProcessor(isA(ServletContextAwareProcessor.class));
    }

    private void configure() {
        this.testee.enableServletContextAwareness(this.factory);
    }
}