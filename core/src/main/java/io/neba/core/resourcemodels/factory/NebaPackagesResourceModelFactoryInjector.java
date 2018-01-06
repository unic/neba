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
package io.neba.core.resourcemodels.factory;

import io.neba.api.spi.ResourceModelFactory;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.ComponentContext;
import org.osgi.util.tracker.BundleTracker;
import org.osgi.util.tracker.BundleTrackerCustomizer;

import java.util.Hashtable;

import static org.osgi.framework.Bundle.ACTIVE;

/**
 * Provides a {@link ModelFactory} service via every bundle that has a <code>Neba-Packages</code> header
 * and contains {@link io.neba.api.annotations.ResourceModel resource models} within the respective packages.
 */
@Component
public class NebaPackagesResourceModelFactoryInjector {
    private BundleTracker<ServiceRegistration<?>> tracker;

    @Activate
    protected void activate(ComponentContext context) {
        this.tracker = new BundleTracker<>(context.getBundleContext(), ACTIVE, new BundleTrackerCustomizer<ServiceRegistration<?>>() {
            @Override
            public ServiceRegistration<?> addingBundle(Bundle bundle, BundleEvent event) {
                ModelFactory factory = new ModelFactory(bundle);
                if (factory.getModelDefinitions().isEmpty()) {
                    return null;
                }
                return bundle.getBundleContext().registerService(ResourceModelFactory.class, factory, new Hashtable<>());
            }

            @Override
            public void modifiedBundle(Bundle bundle, BundleEvent event, ServiceRegistration<?> registration) {
                // ignore
            }

            @Override
            public void removedBundle(Bundle bundle, BundleEvent event, ServiceRegistration<?> registration) {
                registration.unregister();
            }
        });
        this.tracker.open();
    }

    @Deactivate
    protected void deactivate() {
        this.tracker.close();
    }
}
