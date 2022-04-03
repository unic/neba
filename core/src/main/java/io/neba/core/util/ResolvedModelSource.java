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

package io.neba.core.util;

import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Represents the result of a lookup of a {@link io.neba.api.annotations.ResourceModel} for
 * a {@link org.apache.sling.api.resource.Resource}. Provides the found model as an {@link OsgiModelSource}
 * and the resource type the model was found for. The resource type may be any type within the resource's
 * resource type or node type hierarchy.
 *
 * @author Olaf Otto
 */
public class ResolvedModelSource<T> {
    private final OsgiModelSource<T> source;
    private final String resourceType;
    private final int hashCode;

    /**
     * @param source       must not be <code>null</code>
     * @param resourceType must not be <code>null</code>
     */
    public ResolvedModelSource(OsgiModelSource<T> source, String resourceType) {
        if (source == null) {
            throw new IllegalArgumentException("Method argument source must not be null.");
        }
        if (resourceType == null) {
            throw new IllegalArgumentException("Method argument resourceType must not be null.");
        }

        this.source = source;
        this.resourceType = resourceType;
        this.hashCode = new HashCodeBuilder().append(source).append(resourceType).toHashCode();
    }

    public OsgiModelSource<T> getSource() {
        return source;
    }

    /**
     * The resource type for which this model was resolved. May be any type within the
     * mapped resource's {@link io.neba.core.resourcemodels.registration.MappableTypeHierarchy}.
     *
     * @return never <code>null</code>.
     */
    public String getResolvedResourceType() {
        return resourceType;
    }

    @Override
    public String toString() {
        return this.resourceType + " -> [" + this.source + "]";
    }

    @Override
    public int hashCode() {
        return this.hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != getClass()) {
            return false;
        }

        ResolvedModelSource<?> other = (ResolvedModelSource<?>) obj;

        return this.source.equals(other.source) &&
                this.resourceType.equals(other.resourceType);
    }
}
