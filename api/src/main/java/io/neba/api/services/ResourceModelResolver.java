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
package io.neba.api.services;

import io.neba.api.spi.ResourceModelFactory;
import org.apache.sling.api.resource.Resource;

import javax.annotation.CheckForNull;

/**
 * This service is automatically published by the NEBA core and allows programmatic lookup of resource models.
 *
 * @author Olaf Otto
 */
public interface ResourceModelResolver {
    /**
     * @param resource must not be <code>null</code>
     * @param name must not be <code>null</code>
     * @return the most specific model instance compatible with the
     * given resource's resource type, or <code>null</code>. The
     * model's {@link ResourceModelFactory.ModelDefinition#getName() name} matches the given name.
     */
    @CheckForNull
    Object resolveMostSpecificModelWithName(Resource resource, String name);

    /**
     * @param resource must not be <code>null</code>.
     * @return the most specific model for the given resource, or <code>null</code> if
     * there is no unique most specific model. Models for base types such as nt:usntructured
     * or nt:base are not considered.
     */
    @CheckForNull
    Object resolveMostSpecificModel(Resource resource);

    /**
     * @param resource must not be <code>null</code>.
     * @return the most specific model for the given resource, or <code>null</code> if
     * there is no unique most specific model. Models for base types such as nt:unstructured
     * or nt:base are considered.
     */
    @CheckForNull
    Object resolveMostSpecificModelIncludingModelsForBaseTypes(Resource resource);
}
