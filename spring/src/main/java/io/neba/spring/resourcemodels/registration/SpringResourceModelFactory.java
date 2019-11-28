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
import java.util.Collection;
import java.util.List;

class SpringResourceModelFactory implements ResourceModelFactory {
    private final List<ModelDefinition<?>> modelDefinitions;
    private final ContentToModelMappingBeanPostProcessor beanPostProcessor;
    private final ConfigurableListableBeanFactory factory;

    SpringResourceModelFactory(List<ModelDefinition<?>> modelDefinitions, ContentToModelMappingBeanPostProcessor beanPostProcessor, ConfigurableListableBeanFactory factory) {
        this.modelDefinitions = modelDefinitions;
        this.beanPostProcessor = beanPostProcessor;
        this.factory = factory;
    }

    @Override
    @Nonnull
    public Collection<ModelDefinition<?>> getModelDefinitions() {
        return modelDefinitions;
    }

    @Override
    public <T> T provideModel(@Nonnull ModelDefinition<T> modelDefinition, @Nonnull ContentToModelMappingCallback<T> callback) {
        try {
            beanPostProcessor.push(callback);
            return callback.map(factory.getBean(modelDefinition.getName(), modelDefinition.getType()));
        } finally {
            beanPostProcessor.pop();
        }
    }
}
