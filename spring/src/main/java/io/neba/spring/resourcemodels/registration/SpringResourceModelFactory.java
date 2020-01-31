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

import io.neba.api.spi.ResourceModelFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Retrieves resource model beans from a {@link org.springframework.beans.factory.ListableBeanFactory#getBean(String, Class) bean factory}.
 */
class SpringResourceModelFactory implements ResourceModelFactory {
    private final List<SpringBasedModelDefinition> modelDefinitions;
    private final ConfigurableListableBeanFactory factory;

    SpringResourceModelFactory(List<SpringBasedModelDefinition> modelDefinitions, ConfigurableListableBeanFactory factory) {
        this.modelDefinitions = modelDefinitions;
        this.factory = factory;
    }

    @Override
    @Nonnull
    public Collection<ModelDefinition<?>> getModelDefinitions() {
        return new ArrayList<>(this.modelDefinitions);
    }

    @Override
    public <T> T provideModel(@Nonnull ModelDefinition<T> modelDefinition, @Nonnull ContentToModelMappingCallback<T> callback) {
        if (!(modelDefinition instanceof SpringBasedModelDefinition)) {
            throw new IllegalArgumentException("Unable to provide the model " + modelDefinition + ": The model definition does not stem from spring. This factory should not have been asked to provide a model for it.");
        }

        SpringBasedModelDefinition springBasedModelDefinition = (SpringBasedModelDefinition) modelDefinition;
        T bean = factory.getBean(springBasedModelDefinition.getBeanName(), modelDefinition.getType());
        return callback.map(bean);
    }
}
