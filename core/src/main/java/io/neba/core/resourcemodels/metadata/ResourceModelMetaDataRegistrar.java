/*
  Copyright 2013 the original author or authors.
  <p>
  Licensed under the Apache License, Version 2.0 the "License";
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at
  <p>
  http://www.apache.org/licenses/LICENSE-2.0
  <p>
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
 */

package io.neba.core.resourcemodels.metadata;

import io.neba.core.util.OsgiModelSource;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.osgi.framework.Bundle;


import static java.util.stream.Collectors.toList;

/**
 * Builds and caches {@link ResourceModelMetaData} for each {@link io.neba.api.annotations.ResourceModel}.
 * This meta-data may then be used by processors of a resource model,
 * e.g. the {@link io.neba.core.resourcemodels.mapping.FieldValueMappingCallback},
 * to avoid having to reflect on the resource model type.
 *
 * @author Olaf Otto
 */
@Service(ResourceModelMetaDataRegistrar.class)
@Component
public class ResourceModelMetaDataRegistrar {
    /**
     * @author Olaf Otto
     */
    private static class ResourceModelMetadataHolder {
        private final OsgiModelSource<?> source;
        private final ResourceModelMetaData metaData;

        private ResourceModelMetadataHolder(OsgiModelSource<?> source, ResourceModelMetaData metaData) {
            this.source = source;
            this.metaData = metaData;
        }
    }

    private Map<Class<?>, ResourceModelMetadataHolder> cache = new HashMap<>(512);

    /**
     * @return the {@link ResourceModelMetaData} of all currently known resource models.
     */
    public Collection<ResourceModelMetaData> get() {
        return this.cache.values()
                .stream()
                .map(holder -> holder.metaData)
                .collect(toList());
    }

    /**
     * @param modelType must not be <code>null</code>.
     * @return the {@link ResourceModelMetaData} of the specified model. Never <code>null</code> - throws an {@link IllegalStateException}
     *         if the model type is not known as a resource model must always be registered.
     */
    public ResourceModelMetaData get(Class<?> modelType) {
        if (modelType == null) {
            throw new IllegalArgumentException("Method argument modelType must not be null.");
        }

        // Optimistic lookup: Most of the models types are most likely not enhanced by CGLib.
        ResourceModelMetadataHolder metaDataHolder = this.cache.get(modelType);

        if (metaDataHolder == null) {
            // The model type might have been enhanced, explicitly lookup with the user (non-enhanced) class.
            metaDataHolder = this.cache.get(getUserClass(modelType));
        }

        if (metaDataHolder == null) {
            throw new IllegalStateException("Unable to obtain resource model metadata for " + modelType +
                    " - this type was either never registered or has been removed, i.e. " +
                    " it's source bundle was uninstalled.");
        }

        return metaDataHolder.metaData;
    }

    /**
     * Creates a new {@link ResourceModelMetaData} for the model represented
     * by the provided model source.
     *
     * @param modelSource must not be <code>null</code>.
     * @return the newly created meta data. Never <code>null</code>.
     */
    public ResourceModelMetaData register(OsgiModelSource<?> modelSource) {
        if (modelSource == null) {
            throw new IllegalArgumentException("method parameter modelSource must not be null");
        }

        Class<?> modelType = modelSource.getModelType();
        ResourceModelMetaData modelMetaData = new ResourceModelMetaData(modelType);
        ResourceModelMetadataHolder holder = new ResourceModelMetadataHolder(modelSource, modelMetaData);

        Map<Class<?>, ResourceModelMetadataHolder> newCache = copyCache();
        newCache.put(getUserClass(modelType), holder);

        this.cache = newCache;
        return modelMetaData;
    }

    /**
     * Removes the metadata of all models contained in the provided bundle from the registrar.
     *
     * @param bundle must not be <code>null</code>
     */
    public void removeMetadataForModelsIn(Bundle bundle) {
        if (bundle == null) {
            throw new IllegalArgumentException("method parameter bundle must not be null");
        }

        Map<Class<?>, ResourceModelMetadataHolder> newCache = copyCache();
        Iterator<Map.Entry<Class<?>, ResourceModelMetadataHolder>> it = newCache.entrySet().iterator();
        while (it.hasNext()) {
            OsgiModelSource<?> source = it.next().getValue().source;
            if (source.getBundleId() == bundle.getBundleId()) {
                it.remove();
            }
        }
        this.cache = newCache;
    }

    private Map<Class<?>, ResourceModelMetadataHolder> copyCache() {
        return new HashMap<>(this.cache);
    }

    private Class<?> getUserClass(Class<?> type){
        if (type.getName().contains("$$")) {
            Class<?> superclass = type.getSuperclass();
            if (superclass != null && Object.class != superclass) {
                return superclass;
            }
        }
        return type;
    }

    @Deactivate
    protected void deactivate() {
        this.cache.clear();
    }
}
