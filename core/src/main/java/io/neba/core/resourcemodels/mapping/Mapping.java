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

package io.neba.core.resourcemodels.mapping;

import io.neba.core.resourcemodels.metadata.ResourceModelMetaData;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

/**
 * Represents a mapping of the form <code>resource path -&gt; model</code>.
 *
 * @param <T> the type of the contained {@link #getMappedModel()} mapped model.
 * @author Olaf Otto
 * @author Christoph Huber
 */
public class Mapping<T> {
    private final String srcPath;
    private final ResourceModelMetaData metadata;
    private final String resourceType;
    private final int hashCode;

    private T mappedModel = null;

    /**
     * @param resourcePath must not be <code>null</code>.
     * @param metadata     must not be <code>null</code>.
     * @param resourceType the resource type, e.g. "components/wide/teaser" or "cq:Page". Must not be <code>null</code>.
     */
    Mapping(@Nonnull String resourcePath, @Nonnull ResourceModelMetaData metadata, @Nonnull String resourceType) {
        if (resourcePath == null) {
            throw new IllegalArgumentException("Constructor parameter resourcePath must not be null.");
        }
        if (metadata == null) {
            throw new IllegalArgumentException("Constructor parameter metadata must not be null.");
        }
        if (resourceType == null) {
            throw new IllegalArgumentException("Constructor parameter resourceType must not be null.");
        }

        this.resourceType = resourceType;
        this.srcPath = resourcePath;
        this.metadata = metadata;
        this.hashCode = 31 * (31 + srcPath.hashCode()) + metadata.hashCode();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [" + srcPath + " -> " + this.metadata.getTypeName() + ']';
    }

    void setMappedModel(T mappedModel) {
        this.mappedModel = mappedModel;
    }

    @CheckForNull
    T getMappedModel() {
        return mappedModel;
    }

    @Nonnull
    public ResourceModelMetaData getMetadata() {
        return metadata;
    }

    @Nonnull
    public String getResourceType() {
        return resourceType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Mapping<?> mapping = (Mapping<?>) o;
        return srcPath.equals(mapping.srcPath) &&
                metadata.equals(mapping.metadata);
    }

    @Override
    public int hashCode() {
        return this.hashCode;
    }
}
