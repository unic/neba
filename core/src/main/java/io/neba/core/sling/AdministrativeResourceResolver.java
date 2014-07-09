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

package io.neba.core.sling;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;

/**
 * Serves as a facade to
 * {@link ResourceResolverFactory#getAdministrativeResourceResolver(java.util.Map) administrative resource resolvers}.
 * Handles the lifecycle of the underlying {@link ResourceResolverFactory}, i.e.
 * re-obtains the administrative resolver when the factory changes.
 *
 * @author Olaf Otto
 */
@Service
public class AdministrativeResourceResolver {
    private ResourceResolver resolver;

    public void bind(ResourceResolverFactory resourceResolverFactory) throws LoginException {
        if (resourceResolverFactory == null) {
            throw new IllegalArgumentException("Method argument resourceResolverFactory must not be null.");
        }
        this.resolver = resourceResolverFactory.getAdministrativeResourceResolver(null);
    }

    public void unbind(ResourceResolverFactory resourceResolverFactory) {
        closeResourceResolver();
    }

    @PreDestroy
    public void closeResourceResolver() {
        if (this.resolver != null && this.resolver.isLive()) {
            this.resolver.close();
        }
    }

    /**
     * @see ResourceResolver#resolve(String)
     */
    public Resource resolve(String path) {
        ensureInitialization();
        return this.resolver.resolve(path);
    }

    /**
     * @see ResourceResolver#getResource(String)
     */
    public Resource get(String path) {
        ensureInitialization();
        return this.resolver.getResource(path);
    }

    /**
     * Exposes the administrative resource resolver. Warning: The returned resource resolver
     * will seize to function if its factory changes and must thus not be retained.
     * @return never <code>null</code>.
     */
    public ResourceResolver getResolver() {
        ensureInitialization();
        return resolver;
    }

    private void ensureInitialization() {
        if (this.resolver == null) {
            throw new IllegalStateException("The resolver is not initialized - is the " +
                    ResourceResolverFactory.class.getName() + " service unavailable?");
        }
    }
}
