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

package io.neba.core.resourcemodels.tagsupport;

import io.neba.api.resourcemodels.ResourceModelProvider;
import io.neba.core.resourcemodels.caching.ResourceModelCaches;
import io.neba.core.resourcemodels.mapping.ResourceToModelMapper;
import io.neba.core.resourcemodels.registration.LookupResult;
import io.neba.core.resourcemodels.registration.ModelRegistry;
import io.neba.core.util.OsgiBeanSource;
import org.apache.sling.api.resource.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;

import static io.neba.api.Constants.SYNTHETIC_RESOURCETYPE_ROOT;

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
@Service
public class ResourceModelProviderImpl implements ResourceModelProvider {
    @Autowired
    private ModelRegistry registry;
    @Autowired
    private ResourceToModelMapper mapper;
    @Autowired
    private ResourceModelCaches caches;

    /**
     * {@inheritDoc}
     */
    @Override
    public Object resolveMostSpecificModelWithBeanName(Resource resource, String beanName) {
        if (resource == null) {
            throw new IllegalArgumentException("Method argument resource must not be null.");
        }
        if (beanName == null) {
            throw new IllegalArgumentException("Method argument beanName must not be null.");
        }
        return resolveMostSpecificModelForResource(resource, true, beanName);
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

    private <T> T resolveMostSpecificModelForResource(Resource resource, boolean includeBaseTypes, String beanName) {
        T model = null;
        Collection<LookupResult> models = (beanName == null) ?
                this.registry.lookupMostSpecificModels(resource) :
                this.registry.lookupMostSpecificModels(resource, beanName);

        if (models != null && models.size() == 1) {
            LookupResult lookupResult = models.iterator().next();
            if (includeBaseTypes || !isMappedFromGenericBaseType(lookupResult)) {

                @SuppressWarnings("unchecked")
                OsgiBeanSource<T> source = (OsgiBeanSource<T>) lookupResult.getSource();

                model = this.caches.lookup(resource, source);
                if (model != null) {
                    return model;
                }

                model = this.mapper.map(resource, source);
                this.caches.store(resource, source, model);
            }
        }

        return model;
    }

    private boolean isMappedFromGenericBaseType(LookupResult lookupResult) {
        final String resourceType = lookupResult.getResourceType();

        return "nt:unstructured".equals(resourceType) ||
                "nt:base".equals(resourceType) ||
                SYNTHETIC_RESOURCETYPE_ROOT.equals(resourceType);
    }
}
