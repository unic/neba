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

import io.neba.core.web.RequestScopeConfigurator;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.request.RequestScope;

import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.verify;

/**
 * @author Olaf Otto
 */
@RunWith(MockitoJUnitRunner.class)
public class RequestScopeConfiguratorTest {
	@Mock
    private ConfigurableBeanFactory beanFactory;
	
	@InjectMocks
    private RequestScopeConfigurator testee;

    @Test
    public void testConfigurationOfBeanFactoryWithRequestScope() throws Exception {
        configure();
        verifyRequestScopeIsConfigured();
    }

    private void configure() {
        this.testee.registerRequestScope(this.beanFactory);
    }
    
    private void verifyRequestScopeIsConfigured() {
        verify(this.beanFactory)
          .registerScope(eq(WebApplicationContext.SCOPE_REQUEST), isA(RequestScope.class));
    }
}
