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
import io.neba.core.resourcemodels.metadata.ResourceModelMetaDataRegistrar;
import io.neba.core.util.Key;
import org.apache.sling.api.resource.Resource;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.util.Optional.empty;
import static org.osgi.service.component.annotations.ReferencePolicy.DYNAMIC;

/**
 * Represents all currently registered {@link ResourceModelCache resource model cache services}.
 * <br />
 *
 * Thread-safety must be provided by the underlying {@link ResourceModelCache caches}.
 *
 * @author Olaf Otto
 */

@Component(service = ResourceModelCaches.class)
public class ResourceModelCaches {
    @Reference
    private ResourceModelMetaDataRegistrar metaDataRegistrar;

    @Reference(policy = DYNAMIC, bind = "bind", unbind = "unbind")
    private final List<ResourceModelCache> caches = new ArrayList<>();

    /**
     * Looks up the {@link #store(Resource, Key, Object)} cached model}
     * of the given type for the given resource.
     * Returns the first model found in the caches.
     *
     * @param resource must not be <code>null</code>.
     * @param key      must not be <code>null</code>.
     * @return can be <code>null</code> or {@link java.util.Optional#empty()}. A return value of {@link java.util.Optional#empty()} signals
     * that a an empty model was previously {@link #store(Resource, Key, Optional) stored}, i.e. the adaptation result is known to be null
     * and should not be attempted. A return value of <code>null</code> signals that the model is unknown to this cache, i.e. it was never stored.
     */
    public <T> Optional<T> lookup(@Nonnull Resource resource, @Nonnull Key key) {
        if (resource == null) {
            throw new IllegalArgumentException("Method argument resource must not be null");
        }
        if (key == null) {
            throw new IllegalArgumentException("Method argument key must not be null");
        }

        if (this.caches.isEmpty()) {
            return null;
        }

        final Key lookupKey = key(resource, key);
        for (ResourceModelCache cache : this.caches) {
            final Optional<T> model = cache.get(lookupKey);
            if (model == empty()) {
                return model;
            }

            if (model != null) {
                metaDataRegistrar.get(model.get().getClass()).getStatistics().countCacheHit();
                return model;
            }
        }

        // Null signals that the model was never stored in the cache, i.e. we do not know whether
        // the model can be resolved to anything.
        return null;
    }

    /**
     * Stores the model representing the result of the
     * {@link Resource#adaptTo(Class) adaptation} of the given resource
     * to the given target type.
     *
     * @param resource must not be <code>null</code>.
     * @param key      must not be <code>null</code>.
     * @param model    can be <code>null</code>.
     */
    public <T> void store(@Nonnull Resource resource, @Nonnull Key key, @Nonnull Optional<T> model) {
        if (resource == null) {
            throw new IllegalArgumentException("Method argument resource must not be null");
        }
        if (key == null) {
            throw new IllegalArgumentException("Method argument key must not be null");
        }
        if (model == null) {
            throw new IllegalArgumentException("Method argument model must not be null");
        }

        if (this.caches.isEmpty()) {
            return;
        }

        final Key lookupKey = key(resource, key);
        for (ResourceModelCache cache : this.caches) {
            cache.put(resource, model, lookupKey);
        }
    }

    protected void bind(ResourceModelCache cache) {
        this.caches.add(cache);
    }

    protected void unbind(ResourceModelCache cache) {
        if (cache == null) {
            return;
        }
        this.caches.remove(cache);
    }

    private <T> Key key(Resource resource, Key key) {
        return new Key(resource.getPath(), key, resource.getResourceType(), resource.getResourceResolver().hashCode());
    }

    public static Key key(Object... contents) {
        return new Key(contents);
    }
}
