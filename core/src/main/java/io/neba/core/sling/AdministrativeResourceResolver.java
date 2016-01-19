/**
 * Copyright 2013 the original author or authors.
 * <p/>
 * Licensed under the Apache License, Version 2.0 the "License";
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/

package io.neba.core.sling;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private ResourceResolver resolver;

    public void bind(ResourceResolverFactory factory) throws LoginException {
        if (factory == null) {
            throw new IllegalArgumentException("Method argument resourceResolverFactory must not be null.");
        }
        this.resolver = factory.getAdministrativeResourceResolver(null);
    }

    /**
     * @param ignored can be null.
     * @see #closeResourceResolver()
     */
    public void unbind(@SuppressWarnings("unused") ResourceResolverFactory ignored) {
        closeResourceResolver();
    }

    /**
     * Closes and removes the reference to the current administrative resource resolver, as any resolver instance obtained
     * before a factory changed is malfunctioning, even if {@link ResourceResolver#isLive()} is true. Does
     * also discard the reference to the factory as it is a service proxy and may not be accessed before it is
     * {@link #bind(ResourceResolverFactory) bound} again, as this would result in a deadlock.
     */
    @PreDestroy
    public void closeResourceResolver() {
        if (this.resolver != null && this.resolver.isLive()) {
            this.resolver.close();
        }
        this.resolver = null;
    }

    /**
     * @see ResourceResolver#resolve(String)
     */
    public Resource resolve(String path) {
        return getResolver().resolve(path);
    }

    /**
     * @see org.apache.sling.api.resource.ResourceResolver#resolve(javax.servlet.http.HttpServletRequest, String)
     */
    public Resource resolve(SlingHttpServletRequest request, String path) {
        return getResolver().resolve(request, path);
    }

    /**
     * @see ResourceResolver#getResource(String)
     */
    public Resource get(String path) {
        return getResolver().getResource(path);
    }

    /**
     * Exposes the administrative resource resolver. Warning: The returned resource resolver
     * will seize to function if its factory changes and must thus not be retained.
     *
     * @return never <code>null</code>.
     */
    public ResourceResolver getResolver() {
        if (this.resolver == null) {
            // Logging and throwing is required since subsequent issues hide original exceptions thrown
            // in code synchronously executed in resource resolver factory re-starts.
            IllegalStateException e = new IllegalStateException(
                    "The resource resolver is unavailable - is resource resolution attempted during a resource resolver factory restart? " +
                    "Accessing a resource resolver factory while it is restarting is unsupported, as it would result in a deadlock.");
            logger.error("Unable to provide an administrative resource resolver.", e);
            throw e;
        }
        return resolver;
    }
}
