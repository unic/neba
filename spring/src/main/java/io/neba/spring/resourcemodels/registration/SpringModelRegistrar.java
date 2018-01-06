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
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import javax.annotation.PreDestroy;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.stereotype.Service;


import static java.util.Arrays.stream;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
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

        final List<ResourceModelFactory.ModelDefinition> modelDefinitions =
                stream(beanNamesForTypeIncludingAncestors(factory, Object.class))
                        .map(n -> {
                            final ResourceModel model = factory.findAnnotationOnBean(n, ResourceModel.class);
                            if (model == null) {
                                return null;
                            }
                            return new ResourceModelFactory.ModelDefinition() {
                                @Override
                                @Nonnull
                                public ResourceModel getResourceModel() {
                                    return model;
                                }

                                @Override
                                @Nonnull
                                public String getName() {
                                    return n;
                                }

                                @Override
                                @Nonnull
                                public Class<?> getType() {
                                    return factory.getType(n);
                                }
                            };
                        })
                        .filter(Objects::nonNull)
                        .collect(toList());

        this.bundlesWithModels.put(bundle, bundle.getBundleContext().registerService(
                ResourceModelFactory.class.getName(),
                new ResourceModelFactory() {
                    @Override
                    @Nonnull
                    public Collection<ModelDefinition> getModelDefinitions() {
                        return modelDefinitions;
                    }

                    @Override
                    @Nonnull
                    public Object getModel(@Nonnull ModelDefinition modelDefinition) {
                        return factory.getBean(modelDefinition.getName());
                    }
                },
                new Hashtable<>()
        ));
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