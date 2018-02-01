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

import io.neba.api.spi.ResourceModelCache;
import io.neba.core.util.Key;
import org.apache.commons.lang3.StringUtils;
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

/**
 * A request-scoped {@link ResourceModelCache}. Models added to this cache may either be cached for the entire
 * request regardless of state changes (selectors, suffixes, extension, query string...)
 * during the request processing, or in a request-state sensitive manner.
 *
 * @author Olaf Otto
 */
@Component(
        service = {ResourceModelCache.class, Filter.class},
        property = {
            "service.vendor=neba.io",
            "sling.filter.scope=REQUEST",
            "sling.filter.scope=ERROR"
        }
)
@Designate(ocd = RequestScopedResourceModelCache.Configuration.class)
public class RequestScopedResourceModelCache implements ResourceModelCache, Filter {
    private final ThreadLocal<Map<Object, Object>> cacheHolder = new ThreadLocal<>();
    private final ThreadLocal<SlingHttpServletRequest> requestHolder = new ThreadLocal<>();
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private Configuration configuration;

    /**
     * Returns the key for a cacheHolder. If the key changes, the cacheHolder will be cleared.
     * The key consists of:
     * <ul><li>The current resources page</li>
     * <li>the selector string</li>
     * <li>the extension</li>
     * <li>the suffix</li>
     * <li>the query string</li>
     * </ul>
     */
    private static Key toKey(SlingHttpServletRequest request) {
        final RequestPathInfo requestPathInfo = request.getRequestPathInfo();
        return new Key(StringUtils.substringBefore(requestPathInfo.getResourcePath(), "/jcr:content"),
                requestPathInfo.getSelectorString(),
                requestPathInfo.getExtension(),
                requestPathInfo.getSuffix(),
                request.getQueryString());
    }

    @Activate
    protected void activate(Configuration configuration) {
        this.configuration = configuration;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(Object key) {
        if (key == null) {
            throw new IllegalStateException("Method argument key must not be null.");
        }
        if (!this.configuration.enabled()) {
            return null;
        }

        T model = null;
        Map<Object, T> cache = (Map<Object, T>) this.cacheHolder.get();
        if (cache == null) {
            this.logger.debug("No cache found, the cache will not be used.");
        } else {
            Object internalKey = createInternalKey(key);
            model = cache.get(internalKey);
        }
        return model;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> void put(@Nonnull Resource resource, T model, @Nonnull Object key) {
        if (resource == null) {
            throw new IllegalStateException("Method argument resource must not be null.");
        }
        if (model == null) {
            throw new IllegalStateException("Method argument model must not be null.");
        }
        if (key == null) {
            throw new IllegalStateException("Method argument key must not be null.");
        }
        if (this.configuration.enabled()) {
            Map<Object, Object> cache = this.cacheHolder.get();
            if (cache == null) {
                this.logger.debug("No cache found, the cache will not be used.");
            } else {
                Object internalKey = createInternalKey(key);
                cache.put(internalKey, model);
            }
        }
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
     * The externally provided key may be wrapped to add more key elements in order
     * to restrict the cached object's scope to a specific component when safe mode is enabled.
     *
     * @return A request-state sensitive key in {@link Configuration#safeMode()}, the original key otherwise.
     */
    private Object createInternalKey(Object key) {
        if (this.configuration.safeMode()) {
            // Create a request-state sensitive key to scope the cached model to a request with specific parameters.
            final SlingHttpServletRequest request = this.requestHolder.get();
            if (request != null) {
                return new Key(key, toKey(request));
            }
        }
        return key;
    }

    @ObjectClassDefinition(name = "NEBA request-scoped resource model cache", description = "Provides a request-scoped resource model cache.s")
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
