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

import io.neba.api.annotations.ResourceModel;
import io.neba.api.spi.ResourceModelFactory;

import javax.annotation.Nonnull;

import static java.lang.Character.toLowerCase;

/**
 * A model definition based on a class annotated with {@link ResourceModel}.
 */
class ClassBasedModelDefinition implements ResourceModelFactory.ModelDefinition {
    private final Class c;

    public ClassBasedModelDefinition(Class c) {
        this.c = c;
    }

    @Nonnull
    @Override
    public ResourceModel getResourceModel() {
        return (ResourceModel) c.getAnnotation(ResourceModel.class);
    }

    @Nonnull
    @Override
    public String getName() {
        return toLowerCase(c.getSimpleName().charAt(0)) + c.getSimpleName().substring(1);
    }

    @Nonnull
    @Override
    public Class<?> getType() {
        return c;
    }

    @Override
    public int hashCode() {
        return c.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj.getClass() == getClass() && ((ResourceModelFactory.ModelDefinition) obj).getClass().equals(getClass());
    }

    @Override
    public String toString() {
        return "ClassBasedModelDefinition{" +
                "c=" + c +
                '}';
    }
}
