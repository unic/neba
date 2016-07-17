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

import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestScope;

import static org.springframework.web.context.WebApplicationContext.SCOPE_REQUEST;

/**
 * {@link ConfigurableBeanFactory#registerScope(String, org.springframework.beans.factory.config.Scope) Registers}
 * the {@link RequestScope} in the bean factory using the
 * {@link org.springframework.web.context.WebApplicationContext#SCOPE_REQUEST standard scope name for request}.
 * 
 * @author Olaf Otto
 */
@Service
public class RequestScopeConfigurator {
    public void registerRequestScope(ConfigurableBeanFactory beanFactory) {
        beanFactory.registerScope(SCOPE_REQUEST, new RequestScope());
    }
}
