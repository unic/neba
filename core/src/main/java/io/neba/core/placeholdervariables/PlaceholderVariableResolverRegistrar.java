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
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringValueResolver;

import static io.neba.core.util.BundleUtil.displayNameOf;

/**
 * Detects all bean of type {@link PlaceholderVariableResolver}
 * in the {@link ConfigurableListableBeanFactory}. Registers a
 * {@link PlaceholderVariableResolverWrapper} for each detected resolver to dispatch placeholder resolution
 * in the application context to the resolver instances. This allows users to simply implement
 * the {@link PlaceholderVariableResolver} and not tie their implementation to the spring context.
 *
 * @author Olaf Otto
 */
@Service
public class PlaceholderVariableResolverRegistrar {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public void registerResolvers(BundleContext bundleContext, ConfigurableListableBeanFactory beanFactory) {
        this.logger.info("Detecting " + PlaceholderVariableResolver.class + " instances in " +  
        		displayNameOf(bundleContext.getBundle()) + "...");
        String[] valueResolvers = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(beanFactory, 
        		PlaceholderVariableResolver.class);

        for (String resolverName : valueResolvers) {
            StringValueResolver embeddableResolver = new PlaceholderVariableResolverWrapper(beanFactory, resolverName);
            beanFactory.addEmbeddedValueResolver(embeddableResolver);  
        }
        this.logger.info("Detected and registered " + valueResolvers.length  + " resolver(s).");
    }
}
