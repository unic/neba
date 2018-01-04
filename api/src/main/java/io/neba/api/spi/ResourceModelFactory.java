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
package io.neba.api.spi;

import io.neba.api.annotations.ResourceModel;

import javax.annotation.Nonnull;
import java.util.Collection;

/**
 * Represents a source for resource models. NEBA will register all services of this type to obtain the available resource models. The models
 * will be tied to the scope of the providing service.
 *
 * @author Olaf Otto
 */
public interface ResourceModelFactory {
    /**
     * @return A list of all {@link ModelDefinition model definitions} suitable for {@link #getModel(ModelDefinition) model} resolution.
     * Never <code>null</code> but rather an empty collections.
     */
    @Nonnull
    Collection<ModelDefinition> getModelDefinitions();

    /**
     * @param modelDefinition must not be <code>null</code>.
     * @return an instance of the model with the given name. Never <code>null</code>.
     */
    @Nonnull
    Object getModel(@Nonnull ModelDefinition modelDefinition);

    /**
     * @author Olaf Otto
     */
    interface ModelDefinition {
        @Nonnull
        ResourceModel getResourceModel();

        @Nonnull
        String getName();

        @Nonnull
        Class<?> getType();
    }
}
