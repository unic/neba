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

package io.neba.core.placeholdervariables;

import io.neba.api.configuration.PlaceholderVariableResolver;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.util.PropertyPlaceholderHelper;
import org.springframework.util.PropertyPlaceholderHelper.PlaceholderResolver;
import org.springframework.util.StringValueResolver;

/**
 * Uses a {@link PlaceholderVariableResolver} as a {@link PlaceholderResolver} to enable
 * {@link PropertyPlaceholderHelper property-placeholder-style} substitution of ${variable}s.  
 * 
 * @author Olaf Otto
 * @see PropertyPlaceholderHelper
 */
public class PlaceholderVariableResolverWrapper implements StringValueResolver {
    /**
     * @author Olaf Otto
     */
    private static final class DispatchingPlaceholderResolver implements PlaceholderResolver {
        private final String resolverName;
        private final ConfigurableListableBeanFactory beanFactory;

        private DispatchingPlaceholderResolver(String resolverName, ConfigurableListableBeanFactory beanFactory) {
            this.resolverName = resolverName;
            this.beanFactory = beanFactory;
        }

        @Override
        public String resolvePlaceholder(String variableName) {
            PlaceholderVariableResolver resolver = beanFactory.getBean(resolverName, PlaceholderVariableResolver.class);
            return resolver.resolve(variableName);
        }
    }

    private final PropertyPlaceholderHelper propertyPlaceholderHelper = new PropertyPlaceholderHelper("${", "}");
    private final PlaceholderResolver resolver;
    

    public PlaceholderVariableResolverWrapper(final ConfigurableListableBeanFactory beanFactory,  final String resolverName) {
        this.resolver = new DispatchingPlaceholderResolver(resolverName, beanFactory);
    }

    @Override
    public String resolveStringValue(String strVal) {
        return this.propertyPlaceholderHelper.replacePlaceholders(strVal, this.resolver);
    }
}