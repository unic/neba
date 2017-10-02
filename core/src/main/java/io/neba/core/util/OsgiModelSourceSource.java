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

import io.neba.api.resourcemodels.ResourceModelFactory;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.osgi.framework.Bundle;


import static org.osgi.framework.Bundle.ACTIVE;

/**
 * A source for beans in an {@link org.springframework.context.ApplicationContext} obtained
 * from a {@link org.osgi.framework.Bundle}.
 * @param <T> The bean's type.
 * 
 * @author Olaf Otto
 */
public class OsgiModelSourceSource<T> {
	private final String modelName;
	private final ResourceModelFactory factory;
	private final long bundleId;
    private final int hashCode;
    private final Class<?> modelType;
    private final Bundle bundle;

    /**
     * @param modelName must not be <code>null</code>.
     * @param factory must not be <code>null</code>.
	 * @param bundle must not be <code>null</code>.
     */
    public OsgiModelSourceSource(String modelName, ResourceModelFactory factory, Bundle bundle) {
        if (modelName == null) {
            throw new IllegalArgumentException("Method argument modelName must not be null.");
        }
        if (factory == null) {
            throw new IllegalArgumentException("Method argument factory must not be null.");
        }
        if (bundle == null) {
            throw new IllegalArgumentException("Method argument bundle must not be null.");
        }

        this.modelName = modelName;
		this.factory = factory;
		this.bundleId = bundle.getBundleId();
        this.bundle = bundle;
        // Referencing the bean type is safe: It either stems from the source bundle, or a bundle the source bundle depends on
        // via an import-package relationship. Thus, if the type changes, the source bundle is re-loaded as well thus
        // causing this bean source to be re-created.
        this.modelType = factory.getType(modelName);
		this.hashCode = new HashCodeBuilder().append(modelName).append(bundleId).toHashCode();
	}
	
	@SuppressWarnings("unchecked")
    public T getModel() {
		return (T) this.factory.getModel(this.getModelName());
	}
	
	public Class<?> getModelType() {
		return this.modelType;
	}

	public long getBundleId() {
		return this.bundleId;
	}

	public ResourceModelFactory getFactory() {
		return this.factory;
	}

    public String getModelName() {
        return modelName;
    }

    /**
     * @return whether this reference points to an active bundle.
     */
    public boolean isValid() {
        return this.bundle.getState() == ACTIVE;
    }

	@Override
	public String toString() {
        return "Bean " + '"' + this.getModelName() + '"' + " from bundle with id " + this.bundleId;
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
	    
        OsgiModelSourceSource<?> other = (OsgiModelSourceSource<?>) obj;

        // Why are bundleId and modelName compared, but not simply the bean type?
        // Theoretically, it is possible to register the same type in two different OSGi bundles
        return this.bundleId == other.bundleId && 
               this.modelName.equals(other.modelName);
	}

    /**
     * @return never <code>null</code>.
     */
    public Bundle getBundle() {
        return this.bundle;
    }
}