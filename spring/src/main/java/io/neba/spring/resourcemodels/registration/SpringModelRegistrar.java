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

package io.neba.spring.resourcemodels.registration;

import io.neba.api.annotations.ResourceModel;
import io.neba.api.spi.ResourceModelFactory;
import io.neba.api.spi.ResourceModelFactory.ModelDefinition;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.lang.annotation.IncompleteAnnotationException;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Arrays.stream;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.osgi.framework.Constants.SERVICE_DESCRIPTION;
import static org.osgi.framework.Constants.SERVICE_VENDOR;
import static org.springframework.beans.factory.BeanFactoryUtils.beanNamesForTypeIncludingAncestors;

/**
 * Whenever a {@link org.springframework.beans.factory.BeanFactory} is initialized, this registrar
 * {@link #registerModels(BundleContext, ConfigurableListableBeanFactory)
 * searches} the factory's bean definitions for beans annotated with
 * {@link ResourceModel}. The discovered models are published via a dedicated {@link ResourceModelFactory} for
 * the provided bundle.
 *
 * @author Olaf Otto
 * @see ResourceModelFactory
 * @see io.neba.spring.blueprint.SlingBeanFactoryPostProcessor
 */
@Service
public class SpringModelRegistrar {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Map<Bundle, ServiceRegistration> bundlesWithModels = new ConcurrentHashMap<>();

    public void registerModels(BundleContext bundleContext, ConfigurableListableBeanFactory factory) {
        final Bundle bundle = bundleContext.getBundle();
        logger.info("Discovering resource models in bundle {}  ...", bundle.getSymbolicName());

        final List<SpringBasedModelDefinition> modelDefinitions =
                stream(beanNamesForTypeIncludingAncestors(factory, Object.class))
                        .map(beanName -> {
                            final Class<?> modelType = factory.getType(beanName);
                            if (modelType == null) {
                                logger.error("The spring application context cannot determine the type of the resource model bean {} in bundle {}. Skipping this model.", beanName, bundle);
                                return null;
                            }

                            final ResourceModel model = getResourceModelAnnotation(factory, beanName, modelType);

                            if (model == null) {
                                return null;
                            }
                            return new SpringBasedModelDefinition(model, beanName, modelType);
                        })
                        .filter(Objects::nonNull)
                        .collect(toList());

        final ContentToModelMappingBeanPostProcessor beanPostProcessor = new ContentToModelMappingBeanPostProcessor(modelDefinitions);
        factory.addBeanPostProcessor(beanPostProcessor);

        Hashtable<String, Object> properties = new Hashtable<>();
        properties.put(SERVICE_DESCRIPTION, "Provides NEBA resource models from Spring Beans annotated with @" + ResourceModel.class.getSimpleName() + ".");
        properties.put(SERVICE_VENDOR, "neba.io");

        this.bundlesWithModels.put(bundle, bundle.getBundleContext().registerService(
                ResourceModelFactory.class.getName(),
                new SpringResourceModelFactory(modelDefinitions, beanPostProcessor, factory),
                properties
        ));
    }

    private ResourceModel getResourceModelAnnotation(ConfigurableListableBeanFactory factory, String n, Class<?> beanType) {
        try {
            return factory.findAnnotationOnBean(n, ResourceModel.class);
        } catch (IncompleteAnnotationException e) {
            // Legacy support: This is very likely an old version of the resource model annotation.
            // Spring currently assumes it can always load all annotation values, which is not true for
            // binary-compatible changes like newly added annotation attributes. This will be fixed in upcoming Spring version (issue 24029).
            return Annotations.annotations(beanType).get(ResourceModel.class);
        }
    }

    public void unregister(Bundle bundle) {
        ofNullable(this.bundlesWithModels.remove(bundle)).ifPresent(ServiceRegistration::unregister);
    }

    @PreDestroy
    protected void shutdown() {
        this.bundlesWithModels.forEach((b, s) -> {
            try {
                s.unregister();
            } catch (IllegalStateException e) {
                logger.trace("Cannot unregister the resource model factory service. The service may already have been unregistered.", e);
            }
        });
    }
}
