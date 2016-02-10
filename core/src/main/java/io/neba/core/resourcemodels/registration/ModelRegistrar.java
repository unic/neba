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

package io.neba.core.resourcemodels.registration;

import io.neba.api.annotations.ResourceModel;
import io.neba.core.resourcemodels.adaptation.ResourceToModelAdapterUpdater;
import io.neba.core.resourcemodels.metadata.ResourceModelMetaDataRegistrar;
import io.neba.core.util.OsgiBeanSource;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.stereotype.Service;

import static io.neba.core.util.BundleUtil.displayNameOf;
import static org.apache.commons.lang.StringUtils.join;
import static org.springframework.beans.factory.BeanFactoryUtils.beanNamesForTypeIncludingAncestors;

/**
 * Whenever a {@link org.springframework.beans.factory.BeanFactory} is initialized, this registrar
 * {@link #registerModels(BundleContext, ConfigurableListableBeanFactory)
 * searches} the factory's bean definitions for beans annotated with
 * {@link ResourceModel}. The corresponding mappings {type -&gt; model} are then
 * added to the {@link ModelRegistry}, providing models for resources. This
 * registrar also signals changes of the {@link ModelRegistry} to the
 * {@link io.neba.core.resourcemodels.adaptation.ResourceToModelAdapter}.
 *
 * @author Olaf Otto
 * @see io.neba.core.resourcemodels.adaptation.ResourceToModelAdapterUpdater
 * @see io.neba.core.blueprint.SlingBeanFactoryPostProcessor
 */
@Service
public class ModelRegistrar {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    @Autowired
    private ModelRegistry registry;
    @Autowired
    private ResourceToModelAdapterUpdater resourceToModelAdapterUpdater;
    @Autowired
    private ResourceModelMetaDataRegistrar resourceModelMetaDataRegistrar;

    private void discoverResourceModels(ConfigurableListableBeanFactory factory, Bundle bundle) {
        logger.info("Discovering resource models in bundle: " + displayNameOf(bundle) + " ...");
        String[] beanNames = beanNamesForTypeIncludingAncestors(factory, Object.class);
        int numberOfDiscoveredModels = 0;
        for (String beanName : beanNames) {
            ResourceModel resourceModel = factory.findAnnotationOnBean(beanName, ResourceModel.class);
            if (resourceModel != null) {
                ++numberOfDiscoveredModels;
                OsgiBeanSource<Object> source = new OsgiBeanSource<>(beanName, factory, bundle);
                this.resourceModelMetaDataRegistrar.register(source);
                this.registry.add(resourceModel.types(), source);
                logger.debug("Registered bean " + beanName + " as a model for the resource types "
                            + join(resourceModel.types(), ", ") + ".");
            }
        }
        logger.info("Discovered " + numberOfDiscoveredModels + " resource model(s) in bundle: "
                + displayNameOf(bundle) + ".");
    }

    public void registerModels(BundleContext bundleContext, ConfigurableListableBeanFactory beanFactory) {
        discoverResourceModels(beanFactory, bundleContext.getBundle());
        this.resourceToModelAdapterUpdater.refresh();
    }

    public void unregister(Bundle bundle) {
        this.registry.removeResourceModels(bundle);
        this.resourceModelMetaDataRegistrar.remove(bundle);
        this.resourceToModelAdapterUpdater.refresh();
    }
}