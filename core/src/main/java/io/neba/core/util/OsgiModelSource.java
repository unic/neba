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

import io.neba.api.spi.ResourceModelFactory;
import io.neba.api.spi.ResourceModelFactory.ModelDefinition;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.osgi.framework.Bundle;

/**
 * A source for models provided by a {@link ResourceModelFactory} provided
 * by a {@link org.osgi.framework.Bundle}.
 *
 * @param <T> The model's type.
 * @author Olaf Otto
 */
public class OsgiModelSource<T> {
    private final ModelDefinition modelDefinition;
    private final ResourceModelFactory factory;
    private final long bundleId;
    private final int hashCode;
    private final Bundle bundle;

    /**
     * @param modelDefinition must not be <code>null</code>.
     * @param factory         must not be <code>null</code>.
     * @param bundle          must not be <code>null</code>.
     */
    public OsgiModelSource(ModelDefinition modelDefinition, ResourceModelFactory factory, Bundle bundle) {
        if (modelDefinition == null) {
            throw new IllegalArgumentException("Method argument modelDefinition must not be null.");
        }
        if (factory == null) {
            throw new IllegalArgumentException("Method argument factory must not be null.");
        }
        if (bundle == null) {
            throw new IllegalArgumentException("Method argument bundle must not be null.");
        }

        // Referencing the definition is safe: It either stems from the source bundle, or a bundle the source bundle depends on
        // via an import-package relationship. Thus, if the type changes, the source bundle is re-loaded as well thus
        // causing this model source to be re-created.
        this.modelDefinition = modelDefinition;
        this.factory = factory;
        this.bundleId = bundle.getBundleId();
        this.bundle = bundle;
        this.hashCode = new HashCodeBuilder().append(this.modelDefinition.getName()).append(bundleId).toHashCode();
    }

    @SuppressWarnings("unchecked")
    public T getModel() {
        return (T) this.factory.getModel(this.modelDefinition);
    }

    public Class<?> getModelType() {
        return this.modelDefinition.getType();
    }

    public long getBundleId() {
        return this.bundleId;
    }

    public ResourceModelFactory getFactory() {
        return this.factory;
    }

    public String getModelName() {
        return modelDefinition.getName();
    }

    @Override
    public String toString() {
        return "Model " + '"' + this.getModelName() + '"' + " from bundle with id " + this.bundleId;
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

        OsgiModelSource<?> other = (OsgiModelSource<?>) obj;

        // Why are bundleId and modelDefinition name compared, but not simply the model type?
        // Theoretically, it is possible to register the same type in two different OSGi bundles
        return this.bundleId == other.bundleId &&
                this.modelDefinition.getName().equals(other.modelDefinition.getName());
    }

    /**
     * @return never <code>null</code>.
     */
    public Bundle getBundle() {
        return this.bundle;
    }
}