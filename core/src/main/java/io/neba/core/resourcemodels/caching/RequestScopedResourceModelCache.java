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

package io.neba.core.resourcemodels.caching;

import io.neba.core.resourcemodels.metadata.ResourceModelMetaDataRegistrar;
import io.neba.core.util.Key;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.substringBefore;
import static org.osgi.framework.Constants.SERVICE_RANKING;
import static org.osgi.framework.Constants.SERVICE_VENDOR;

/**
 * A request-scoped resource model cache. Models added to this cache may either be cached for the entire
 * request regardless of state changes (selectors, suffixes, extension, query string...)
 * during the request processing, or in a request-state sensitive manner.
 *
 * @author Olaf Otto
 */
@Component(
        service = {Filter.class, RequestScopedResourceModelCache.class},
        property = {
                SERVICE_VENDOR + "=neba.io",
                "sling.filter.scope=REQUEST",
                "sling.filter.scope=ERROR",
                SERVICE_RANKING + ":Integer=9000"
        }
)
@Designate(ocd = RequestScopedResourceModelCache.Configuration.class)
public class RequestScopedResourceModelCache implements Filter {
    private final ThreadLocal<Map<Key, Optional<?>>> cacheHolder = new ThreadLocal<>();
    private final ThreadLocal<SlingHttpServletRequest> requestHolder = new ThreadLocal<>();
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Reference
    private ResourceModelMetaDataRegistrar metaDataRegistrar;

    private Configuration configuration;

    @Activate
    protected void activate(Configuration configuration) {
        this.configuration = configuration;
    }

    /**
     * Retrieve a cached model.
     *
     * @param resource The resource {@link Resource#adaptTo(Class) adapted} to the target type. Never <code>null</code>.
     * @param key      The key used to identify the stored model. Never <code>null</code>.
     * @return Either an instance of Optional - which means the object was stored as non null or known null value, depending on whether the option {@link Optional#isPresent()},
     * or <code>null</code>, signaling that the key is not known to the cache.
     */
    @CheckForNull
    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(@Nonnull Resource resource, @Nonnull Key key) {
        if (key == null) {
            throw new IllegalArgumentException("Method argument key must not be null.");
        }
        if (resource == null) {
            throw new IllegalArgumentException("Method argument resource must not be null.");
        }
        if (!this.configuration.enabled()) {
            return null;
        }

        Map<Key, Optional<?>> cache = this.cacheHolder.get();
        if (cache == null) {
            this.logger.debug("No cache found, the cache will not be used.");
            return null;
        }

        final Optional<T> lookupResult;
        if (this.configuration.safeMode()) {
            lookupResult = (Optional<T>) cache.get(createSafeModeKey(resource, key));
        } else {
            lookupResult = (Optional<T>) cache.get(createKey(resource, key));
        }

        if (lookupResult != null && lookupResult.isPresent()) {
            metaDataRegistrar.get(lookupResult.get().getClass()).getStatistics().countCacheHit();
        }

        return lookupResult;
    }

    /**
     * @param resource The resource {@link Resource#adaptTo(Class) adapted} to the target type. Never <code>null</code>.
     * @param model    the model representing the mapped result of the adaptation. Can be <code>null</code>.
     * @param key      the key by which the model is identified and {@link #get(Resource, Key)} retrieved}. Never <code>null</code>.
     */
    public <T> void put(@Nonnull Resource resource, @Nonnull Key key, @CheckForNull T model) {
        if (resource == null) {
            throw new IllegalArgumentException("Method argument resource must not be null.");
        }

        if (key == null) {
            throw new IllegalArgumentException("Method argument key must not be null.");
        }

        if (!this.configuration.enabled()) {
            return;
        }

        Map<Key, Optional<?>> cache = this.cacheHolder.get();
        if (cache == null) {
            this.logger.debug("No cache found, the cache will not be used.");
            return;
        }

        final Optional<?> storedValue = ofNullable(model);

        if (this.configuration.safeMode()) {
            cache.put(createSafeModeKey(resource, key), storedValue);
            return;
        }

        cache.put(createKey(resource, key), storedValue);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void doFilter(@Nonnull ServletRequest request, @Nonnull ServletResponse response, @Nonnull FilterChain chain) throws IOException, ServletException {
        if (!this.configuration.enabled()) {
            chain.doFilter(request, response);
            return;
        }

        if (!(request instanceof SlingHttpServletRequest)) {
            throw new IllegalStateException("Expected a " + SlingHttpServletRequest.class.getName() + ", but got: " + request + ".");
        }

        final SlingHttpServletRequest slingHttpServletRequest = (SlingHttpServletRequest) request;
        this.requestHolder.set(slingHttpServletRequest);
        this.cacheHolder.set(new HashMap<>(256));

        try {
            chain.doFilter(slingHttpServletRequest, response);
        } finally {
            this.cacheHolder.remove();
            this.requestHolder.remove();
        }
    }

    @Override
    public void init(FilterConfig filterConfig) {
        // ignore
    }

    @Override
    public void destroy() {
        // ignore
    }

    /**
     * The provided key may be wrapped to add more key elements in order
     * to restrict the cached object's scope to a specific component when safe mode is enabled.
     *
     * @return A request-state sensitive key if the cached if the current thread is a HTTP request, the original key if not.
     * @see #createKey(Resource, Key)
     */
    @Nonnull
    private Key createSafeModeKey(@Nonnull Resource resource, @Nonnull Key key) {
        // Create a request-state sensitive key to scope the cached model to a request with specific parameters.
        final SlingHttpServletRequest request = this.requestHolder.get();

        if (request == null) {
            return key;
        }

        final RequestPathInfo requestPathInfo = request.getRequestPathInfo();
        return new Key(
                resource.getPath(),
                key,
                resource.getResourceType(),
                identityOf(resource.getResourceResolver()),

                substringBefore(requestPathInfo.getResourcePath(), "/jcr:content"),
                requestPathInfo.getSelectorString(),
                requestPathInfo.getExtension(),
                requestPathInfo.getSuffix(),
                request.getQueryString());
    }

    /**
     * Create a key by combining the given key with standardized resource information.
     * Here, it is essential to  not only include the resource path but also the resource type, since the same resource path might be
     * included (synthetic resources...) with different resource types, and the resource resolver identity, since different resource resolvers
     * may be used within the same request and they might feature different views on resource trees, e.g. through deviating privileges.
     */
    @Nonnull
    private static Key createKey(@Nonnull Resource resource, @Nonnull Key key) {
        return new Key(
                resource.getPath(),
                key,
                resource.getResourceType(),
                identityOf(resource.getResourceResolver())
        );
    }

    /**
     * A resource resolver is associated with specific repository permissions. To avoid leaking privileges by sharing resource-to-model mapping results
     * between different resource resolvers, the resource resolver identity is used as part of the cache key. Here, we either use the
     * user ID associated with the resolver or, if no user is associated, the resolver itself, which translates
     * to the {@link ResourceResolver#hashCode() resource resolver's hash code} being used in the key.
     */
    @Nonnull
    private static Object identityOf(@Nonnull ResourceResolver resourceResolver) {
        Object id = resourceResolver.getUserID();
        if (id != null) {
            return id;
        }
        return resourceResolver;
    }

    @ObjectClassDefinition(name = "NEBA request-scoped resource model cache", description = "Provides a request-scoped resource model cache")
    public @interface Configuration {
        @AttributeDefinition(
                name = "Enabled",
                description = "Activates the request-scoped cache for resource models.")
        boolean enabled() default true;

        @AttributeDefinition(
                name = "Safemode",
                description = "In safemode, caching is sensitive to the current page resource and request parameters " +
                        "such as selectors, suffix, extension and the query string. Should @ResourceModels erroneously cache such state, " +
                        "e.g. by initializing the corresponding value once in a @AfterMapping method, safemode prevents errors caused " +
                        "when performing subsequent internal changes to the request state (e.g. during forwards and includes). Note that " +
                        "enabling this feature is likely to a significant negative performance impact. It is highly recommended to disable " +
                        "safemode in favor of safe-to-cache @ResourceModels.")
        boolean safeMode() default false;
    }
}
