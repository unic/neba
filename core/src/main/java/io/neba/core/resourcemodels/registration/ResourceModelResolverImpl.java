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

package io.neba.core.resourcemodels.registration;

import io.neba.api.services.ResourceModelResolver;
import io.neba.core.resourcemodels.caching.RequestScopedResourceModelCache;
import io.neba.core.resourcemodels.mapping.ResourceToModelMapper;
import io.neba.core.util.Key;
import io.neba.core.util.ResolvedModel;
import org.apache.sling.api.resource.Resource;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Optional;

import static io.neba.api.Constants.SYNTHETIC_RESOURCETYPE_ROOT;
import static io.neba.core.util.Key.key;
import static java.util.Optional.empty;
import static java.util.Optional.of;

/**
 * Resolves a {@link Resource} to a {@link io.neba.api.annotations.ResourceModel}
 * if a model is registered for the {@link Resource#getResourceType() resource type}.
 * <br />
 * Serves as a source for generic models if the resource cannot be
 * {@link Resource#adaptTo(Class) adapted} to a specific target type.<br />
 * If multiple generic models specifically target the type of the given resource through their
 * {@link io.neba.api.annotations.ResourceModel#types()}, this provider
 * may return <code>null</code> since there are no means to automatically resolve such ambiguities.
 *
 * @author Olaf Otto
 */
@Component(service = ResourceModelResolver.class)
public class ResourceModelResolverImpl implements ResourceModelResolver {
    @Reference
    private ModelRegistry registry;
    @Reference
    private ResourceToModelMapper mapper;
    @Reference
    private RequestScopedResourceModelCache cache;

    /**
     * {@inheritDoc}
     */
    @Override
    public Object resolveMostSpecificModelWithName(Resource resource, String name) {
        if (resource == null) {
            throw new IllegalArgumentException("Method argument resource must not be null.");
        }
        if (name == null) {
            throw new IllegalArgumentException("Method argument modelName must not be null.");
        }
        return resolveMostSpecificModelForResource(resource, true, name);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object resolveMostSpecificModel(Resource resource) {
        if (resource == null) {
            throw new IllegalArgumentException("Method argument resource must not be null.");
        }
        return resolveMostSpecificModelForResource(resource, false, null);
    }

    @Override
    public Object resolveMostSpecificModelIncludingModelsForBaseTypes(Resource resource) {
        if (resource == null) {
            throw new IllegalArgumentException("Method argument resource must not be null.");
        }
        return resolveMostSpecificModelForResource(resource, true, null);
    }

    private <T> T resolveMostSpecificModelForResource(@Nonnull Resource resource, boolean includeBaseTypes, @Nullable String modelName) {
        final Key key = key(includeBaseTypes, modelName);

        Optional<T> cachedModel = this.cache.get(resource, key);
        if (cachedModel == empty()) {
            return null;
        }

        if (cachedModel != null) {
            return cachedModel.get();
        }

        Collection<ResolvedModel<?>> models = (modelName == null) ?
                this.registry.lookupMostSpecificModels(resource) :
                this.registry.lookupMostSpecificModels(resource, modelName);

        if (models == null || models.size() != 1) {
            this.cache.put(resource, key, empty());
            return null;
        }

        @SuppressWarnings("unchecked")
        ResolvedModel<T> resolvedModel = (ResolvedModel<T>) models.iterator().next();
        if (!includeBaseTypes && isMappedFromGenericBaseType(resolvedModel)) {
            this.cache.put(resource, key, empty());
            return null;
        }

        T model = this.mapper.map(resource, resolvedModel);
        this.cache.put(resource, key, of(model));

        return model;
    }

    private boolean isMappedFromGenericBaseType(ResolvedModel resolvedModel) {
        final String resourceType = resolvedModel.getResolvedResourceType();

        return "nt:unstructured".equals(resourceType) ||
                "nt:base".equals(resourceType) ||
                SYNTHETIC_RESOURCETYPE_ROOT.equals(resourceType);
    }
}
