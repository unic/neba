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

package io.neba.core.resourcemodels.metadata;

import io.neba.core.util.OsgiBeanSource;
import org.osgi.framework.Bundle;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static org.springframework.util.ClassUtils.getUserClass;

/**
 * Builds and caches {@link ResourceModelMetaData} for each {@link io.neba.api.annotations.ResourceModel}.
 * This meta-data may then be used by processors of a resource model,
 * e.g. the {@link io.neba.core.resourcemodels.mapping.FieldValueMappingCallback},
 * to avoid having to reflect on the resource model type.
 *
 * @author Olaf Otto
 */
@Service
public class ResourceModelMetaDataRegistrar {
    /**
     * @author Olaf Otto
     */
    private static class ResourceModelMetadataHolder {
        private final OsgiBeanSource<?> source;
        private final ResourceModelMetaData metaData;

        private ResourceModelMetadataHolder(OsgiBeanSource<?> source, ResourceModelMetaData metaData) {
            this.source = source;
            this.metaData = metaData;
        }
    }

    private Map<Class<?>, ResourceModelMetadataHolder> cache = new HashMap<Class<?>, ResourceModelMetadataHolder>(512);

    /**
     * @return the {@link ResourceModelMetaData} of all currently known resource models.
     */
    public Collection<ResourceModelMetaData> get() {
        Collection<ResourceModelMetaData> metaData = new ArrayList<ResourceModelMetaData>(256);
        for (ResourceModelMetadataHolder holder : this.cache.values()) {
            metaData.add(holder.metaData);
        }
        return metaData;
    }

    /**
     * @return the {@link ResourceModelMetaData} of the specified model. Never <code>null</code> - throws an {@link IllegalStateException}
     *         if the model type is not known as a resource model must always be registered.
     */
    public ResourceModelMetaData get(Class<?> modelType) {
        if (modelType == null) {
            throw new IllegalArgumentException("Method argument modelType must not be null.");
        }
        ResourceModelMetadataHolder metaDataHolder = this.cache.get(getUserClass(modelType));
        if (metaDataHolder == null) {
            throw new IllegalStateException("Unable to obtain resource model metadata for " + modelType +
                    " - this type was either never registered or has been removed, i.e. " +
                    " it's source bundle was uninstalled.");
        }
        return metaDataHolder.metaData;
    }

    public ResourceModelMetaData register(OsgiBeanSource<?> beanSource) {
        Class<?> beanType = beanSource.getBeanType();
        ResourceModelMetaData modelMetaData = new ResourceModelMetaData(beanType);
        ResourceModelMetadataHolder holder = new ResourceModelMetadataHolder(beanSource, modelMetaData);

        Map<Class<?>, ResourceModelMetadataHolder> newCache = copyCache();
        newCache.put(getUserClass(beanType), holder);

        this.cache = newCache;
        return modelMetaData;
    }

    public void remove(Bundle bundle) {
        Map<Class<?>, ResourceModelMetadataHolder> newCache = copyCache();
        Iterator<Map.Entry<Class<?>, ResourceModelMetadataHolder>> it = newCache.entrySet().iterator();
        while (it.hasNext()) {
            OsgiBeanSource<?> source = it.next().getValue().source;
            if (source.getBundleId() == bundle.getBundleId()) {
                it.remove();
            }
        }
        this.cache = newCache;
    }

    private Map<Class<?>, ResourceModelMetadataHolder> copyCache() {
        return new HashMap<Class<?>, ResourceModelMetadataHolder>(this.cache);
    }

    @PreDestroy
    public void tearDown() {
        this.cache.clear();
    }
}
