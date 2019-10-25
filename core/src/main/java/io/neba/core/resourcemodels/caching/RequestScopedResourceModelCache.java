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

import io.neba.core.util.Key;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
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
    private final ThreadLocal<Map<Object, Object>> cacheHolder = new ThreadLocal<>();
    private final ThreadLocal<SlingHttpServletRequest> requestHolder = new ThreadLocal<>();
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private Configuration configuration;

    @Activate
    protected void activate(Configuration configuration) {
        this.configuration = configuration;
    }

    /**
     * Retrieve a cached model.
     * @param resource The resource {@link Resource#adaptTo(Class) adapted} to the target type. Never <code>null</code>.
     * @param key The key used to identify the stored model. Never <code>null</code>.
     * @return The cached model, or <code>null</code>.
     */
    @CheckForNull
    @SuppressWarnings("unchecked")
    public <T> T get(@Nonnull Resource resource, @Nonnull Key key) {
        if (key == null) {
            throw new IllegalArgumentException("Method argument key must not be null.");
        }
        if (resource == null) {
            throw new IllegalArgumentException("Method argument resource must not be null.");
        }
        if (!this.configuration.enabled()) {
            return null;
        }

        Map<Object, T> cache = (Map<Object, T>) this.cacheHolder.get();
        if (cache == null) {
            this.logger.debug("No cache found, the cache will not be used.");
            return null;
        }

        if (this.configuration.safeMode()) {
            return cache.get(createSafeModeKey(key));
        }

        return cache.get(key);
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

        if (model == null) {
            return;
        }

        if (!this.configuration.enabled()) {
            return;
        }

        Map<Object, Object> cache = this.cacheHolder.get();
        if (cache == null) {
            this.logger.debug("No cache found, the cache will not be used.");
            return;
        }

        if (this.configuration.safeMode()) {
            cache.put(createSafeModeKey(key), model);
            return;
        }

        cache.put(key, model);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        if (!this.configuration.enabled()) {
            chain.doFilter(request, response);
            return;
        }

        if (!(request instanceof SlingHttpServletRequest)) {
            throw new IllegalStateException("Expected a " + SlingHttpServletRequest.class.getName() + ", but got: " + request + ".");
        }

        final SlingHttpServletRequest slingHttpServletRequest = (SlingHttpServletRequest) request;
        this.requestHolder.set(slingHttpServletRequest);
        this.cacheHolder.set(new HashMap<>(1024));

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
     */
    private Object createSafeModeKey(Object key) {
        // Create a request-state sensitive key to scope the cached model to a request with specific parameters.
        final SlingHttpServletRequest request = this.requestHolder.get();

        if (request == null) {
            return key;
        }

        final RequestPathInfo requestPathInfo = request.getRequestPathInfo();
        return new Key(key, new Key(substringBefore(requestPathInfo.getResourcePath(), "/jcr:content"),
                requestPathInfo.getSelectorString(),
                requestPathInfo.getExtension(),
                requestPathInfo.getSuffix(),
                request.getQueryString()));
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

    private <T> Key key(Resource resource, Key key) {
        return new Key(resource.getPath(), key, resource.getResourceType(), resource.getResourceResolver().hashCode());
    }
}
