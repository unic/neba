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

import io.neba.api.resourcemodels.ResourceModelCache;
import io.neba.core.resourcemodels.metadata.ResourceModelMetaDataRegistrar;
import io.neba.core.util.Key;
import io.neba.core.util.OsgiBeanSource;
import org.apache.sling.api.resource.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents all currently registered {@link ResourceModelCache resource model cache services}.
 * <br />
 *
 * Thread-safety must be provided by the underlying {@link ResourceModelCache caches}.
 *
 * @author Olaf Otto
 */
@Service
public class ResourceModelCaches {
    @Autowired
    private ResourceModelMetaDataRegistrar metaDataRegistrar;

    private final List<ResourceModelCache> caches = new ArrayList<>();

    /**
     * Looks up the {@link #store(Resource, OsgiBeanSource, Object)} cached model}
     * of the given type for the given resource.
     * Returns the first model found in the caches.
     *
     * @param resource must not be <code>null</code>.
     * @param modelSource must not be <code>null</code>.
     * @return can be <code>null</code>.
     */
    public <T> T lookup(Resource resource, OsgiBeanSource<?> modelSource) {
        if (resource == null) {
            throw new IllegalArgumentException("Method argument resource must not be null");
        }
        if (modelSource == null) {
            throw new IllegalArgumentException("Method argument modelSource must not be null");
        }

        final Key key = key(resource, modelSource.getBeanType());
        for (ResourceModelCache cache : this.caches) {
            T model = cache.get(key);
            if (model != null) {
                metaDataRegistrar.get(model.getClass()).getStatistics().countCacheHit();
                return model;
            }
        }
        return null;
    }

    /**
     * Stores the model representing the result of the
     * {@link Resource#adaptTo(Class) adaptation} of the given resource
     * to the given target type.
     *
     * @param resource must not be <code>null</code>.
     * @param modelSource must not be <code>null</code>.
     * @param model can be <code>null</code>.
     */
    public <T> void store(Resource resource, OsgiBeanSource<?> modelSource, T model) {
        if (resource == null) {
            throw new IllegalArgumentException("Method argument resource must not be null");
        }
        if (modelSource == null) {
            throw new IllegalArgumentException("Method argument modelSource must not be null");
        }
        if (model == null) {
            throw new IllegalArgumentException("Method argument model must not be null");
        }

        final Key key = key(resource, modelSource.getBeanType());
        for (ResourceModelCache cache : this.caches) {
            cache.put(resource, model, key);
        }
    }

    public void bind(ResourceModelCache cache) {
        this.caches.add(cache);
    }

    public void unbind(ResourceModelCache cache) {
        if (cache == null) {
            return;
        }
        this.caches.remove(cache);
    }

    private <T> Key key(Resource resource, Class<T> modelType) {
        return new Key(resource.getPath(), modelType, resource.getResourceType(), resource.getResourceResolver().hashCode());
    }
}
