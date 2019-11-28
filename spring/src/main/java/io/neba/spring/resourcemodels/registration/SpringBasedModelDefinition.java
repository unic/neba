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

import javax.annotation.Nonnull;

class SpringBasedModelDefinition implements ResourceModelFactory.ModelDefinition<Object> {
    private final ResourceModel model;
    private final String beanName;
    private final Class<?> modelType;

    SpringBasedModelDefinition(ResourceModel model, String beanName, Class<?> modelType) {
        this.model = model;
        this.beanName = beanName;
        this.modelType = modelType;
    }

    @Override
    @Nonnull
    public ResourceModel getResourceModel() {
        return model;
    }

    @Override
    @Nonnull
    public String getName() {
        if (model.name().isEmpty()) {
            return beanName;
        }
        return model.name();
    }

    @Override
    @Nonnull
    public Class<?> getType() {
        return modelType;
    }
}
