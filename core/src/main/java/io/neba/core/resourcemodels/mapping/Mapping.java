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
import io.neba.core.util.Key;

/**
 * Represents a mapping of the form <code>resource path -&gt; model</code>.
 * 
 * @author Olaf Otto
 * @author Christoph Huber
 * @param <T> the type of the contained {@link #getMappedModel()} mapped model.
 */
public class Mapping<T> extends Key {
    private final String srcPath;
    private final ResourceModelMetaData metadata;
    private T mappedModel = null;

    /**
     * @param resourcePath must not be <code>null</code>.
     * @param metadata must not be <code>null</code>.
     */
    public Mapping(String resourcePath, ResourceModelMetaData metadata) {
        super(resourcePath, metadata);

        if (resourcePath == null) {
            throw new IllegalArgumentException("Method argument resourcePath must not be null.");
        }
        if (metadata == null) {
            throw new IllegalArgumentException("Method argument metadata must not be null.");
        }

        this.srcPath = resourcePath;
        this.metadata = metadata;
    }
    
    @Override
    public String toString() {
        return getClass().getSimpleName() + " [" + srcPath + " -> " + this.metadata.getTypeName() + ']';
    }

    public void setMappedModel(T mappedModel) {
        this.mappedModel = mappedModel;
    }

    public T getMappedModel() {
        return mappedModel;
    }

    public ResourceModelMetaData getMetadata() {
        return metadata;
    }
}
