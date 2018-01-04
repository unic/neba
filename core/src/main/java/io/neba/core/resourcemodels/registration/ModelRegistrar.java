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

package io.neba.core.resourcemodels.registration;

import io.neba.api.spi.ResourceModelFactory;
import io.neba.core.resourcemodels.adaptation.ResourceToModelAdapterUpdater;
import io.neba.core.resourcemodels.metadata.ResourceModelMetaDataRegistrar;
import io.neba.core.util.OsgiModelSource;
import java.util.Collection;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import static io.neba.core.util.BundleUtil.displayNameOf;
import static org.apache.commons.lang3.StringUtils.join;

/**
 * @author Olaf Otto
 */
@Component(immediate = true)
public class ModelRegistrar {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Reference
    private ModelRegistry registry;
    @Reference
    private ResourceToModelAdapterUpdater resourceToModelAdapterUpdater;
    @Reference
    private ResourceModelMetaDataRegistrar resourceModelMetaDataRegistrar;
    private ServiceTracker tracker;

    @Activate
    protected void activate(BundleContext context) throws InvalidSyntaxException {
       this.tracker = new ServiceTracker(context, ResourceModelFactory.class.getName(), new ServiceTrackerCustomizer() {
            @Override
            public Object addingService(ServiceReference reference) {
                final ResourceModelFactory factory = (ResourceModelFactory) context.getService(reference);
                registerModels(reference.getBundle(), factory);
                return context.getService(reference);
            }

            @Override
            public void modifiedService(ServiceReference reference, Object service) {
                final ResourceModelFactory factory = (ResourceModelFactory) context.getService(reference);
                unregister(reference.getBundle());
                registerModels(reference.getBundle(), factory);
            }

            @Override
            public void removedService(ServiceReference reference, Object service) {
                unregister(reference.getBundle());
            }
        });
       this.tracker.open(true);
    }

    @Deactivate
    protected void deactivate() {
        this.tracker.close();
    }

    private void registerModels(Bundle bundle, ResourceModelFactory factory) {
        final Collection<ResourceModelFactory.ModelDefinition> modelDefinitions = factory.getModelDefinitions();

        logger.info("Registering {} resource models from bundle: " + displayNameOf(bundle) + " ...", modelDefinitions.size());
        modelDefinitions.forEach(d -> {
            final OsgiModelSource<Object> source = new OsgiModelSource<>(d, factory, bundle);
            this.resourceModelMetaDataRegistrar.register(source);
            this.registry.add(d.getResourceModel().types(), source);
            logger.debug("Registered model {} as a model for the resource types {}.", d.getName(), join(d.getResourceModel().types(), ","));
        });

        this.resourceToModelAdapterUpdater.refresh();
    }

    private void unregister(Bundle bundle) {
        this.registry.removeResourceModels(bundle);
        this.resourceModelMetaDataRegistrar.removeMetadataForModelsIn(bundle);
        this.resourceToModelAdapterUpdater.refresh();
    }
}
