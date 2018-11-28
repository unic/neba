/*
  Copyright 2013 the original author or authors.
  <p/>
  Licensed under the Apache License, Version 2.0 the "License";
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at
  <p/>
  http://www.apache.org/licenses/LICENSE-2.0
  <p/>
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
 */

package io.neba.core.resourcemodels.adaptation;

import io.neba.core.resourcemodels.caching.ResourceModelCaches;
import io.neba.core.resourcemodels.mapping.ResourceToModelMapper;
import io.neba.core.resourcemodels.registration.LookupResult;
import io.neba.core.resourcemodels.registration.ModelRegistry;
import io.neba.core.util.Key;
import io.neba.core.util.OsgiModelSource;
import org.apache.sling.api.adapter.AdapterFactory;
import org.apache.sling.api.resource.Resource;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Optional;

import static io.neba.core.resourcemodels.caching.ResourceModelCaches.key;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.apache.commons.lang3.StringUtils.join;

/**
 * Adapts a {@link Resource} to it's {@link io.neba.api.annotations.ResourceModel} using the
 * {@link ModelRegistry} to lookup and the {@link ResourceToModelMapper} to map the model.
 *
 * @author Olaf Otto
 * @see ResourceToModelAdapterUpdater
 */
@Component(service = ResourceToModelAdapter.class)
public class ResourceToModelAdapter implements AdapterFactory {
    @Reference
    private ModelRegistry registry;
    @Reference
    private ResourceToModelMapper mapper;
    @Reference
    private ResourceModelCaches caches;

    /**
     * @return the resource model provided by the
     * {@link io.neba.core.resourcemodels.registration.ModelRegistrar}
     * or <code>null</code> if no model for the given resource exists or
     * the resource model's type does not match the target type.
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T getAdapter(@Nonnull Object adaptable, @Nonnull Class<T> target) {
        if (!(adaptable instanceof Resource)) {
            return null;
        }

        Resource resource = (Resource) adaptable;
        final Key key = key(target);

        Optional<T> cachedModel = this.caches.lookup(resource, key);
        if (cachedModel == empty()) {
            // The model cannot be resolved, i.e. adapdation results in null and has been cached before.
            return null;
        }

        if (cachedModel != null) {
            return cachedModel.get();
        }

        Collection<LookupResult> models = this.registry.lookupMostSpecificModels(resource, target);

        if (models == null || models.isEmpty()) {
            // Cache the lookup failure
            this.caches.store(resource, key, empty());
            return null;
        }

        if (models.size() != 1) {
            throw new AmbiguousModelAssociationException("There is more than one model that maps " +
                    resource.getPath() + " to " + target.getName() + ": " + join(models, ", ") + ".");
        }

        OsgiModelSource<?> source = models.iterator().next().getSource();

        T model = (T) this.mapper.map(resource, source);

        this.caches.store(resource, key, of(model));

        return model;
    }
}