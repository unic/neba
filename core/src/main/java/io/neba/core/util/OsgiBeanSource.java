/**
 * Copyright 2013 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 the "License";
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
**/

package io.neba.core.util;

import org.apache.commons.lang.builder.HashCodeBuilder;
import org.osgi.framework.Bundle;
import org.springframework.beans.factory.BeanFactory;

import static org.osgi.framework.Bundle.ACTIVE;

/**
 * A source for beans in an {@link org.springframework.context.ApplicationContext} obtained
 * from a {@link org.osgi.framework.Bundle}.
 * @param <T> The bean's type.
 * 
 * @author Olaf Otto
 */
public class OsgiBeanSource<T> {
	private final String beanName;
	private final BeanFactory factory;
	private final long bundleId;
    private final int hashCode;
    private final Class<?> beanType;
    private final Bundle bundle;

    /**
     * @param beanName must not be <code>null</code>.
     * @param factory must not be <code>null</code>.
     */
    public OsgiBeanSource(String beanName, BeanFactory factory, Bundle bundle) {
        if (beanName == null) {
            throw new IllegalArgumentException("Method argument beanName must not be null.");
        }
        if (factory == null) {
            throw new IllegalArgumentException("Method argument factory must not be null.");
        }
        if (bundle == null) {
            throw new IllegalArgumentException("Method argument bundle must not be null.");
        }

        this.beanName = beanName;
		this.factory = factory;
		this.bundleId = bundle.getBundleId();
        this.bundle = bundle;
        // Referencing the bean type is safe: It either stems from the source bundle, or a bundle the source bundle depends on
        // via an import-package relationship. Thus, if the type changes, the source bundle is re-loaded as well thus
        // causing this bean source to be re-created.
        this.beanType = factory.getType(beanName);
		this.hashCode = new HashCodeBuilder().append(beanName).append(bundleId).toHashCode();
	}
	
	@SuppressWarnings("unchecked")
    public T getBean() {
		return (T) this.factory.getBean(this.getBeanName());
	}
	
	public Class<?> getBeanType() {
		return this.beanType;
	}

	public long getBundleId() {
		return this.bundleId;
	}

	public BeanFactory getFactory() {
		return this.factory;
	}

    public String getBeanName() {
        return beanName;
    }

    /**
     * @return whether this reference points to an active bundle.
     */
    public boolean isValid() {
        return this.bundle.getState() == ACTIVE;
    }

	@Override
	public String toString() {
        return "Bean " + '"' + this.getBeanName() + '"' + " from bundle with id " + this.bundleId;
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
	    
        OsgiBeanSource<?> other = (OsgiBeanSource<?>) obj;

        // Why are bundleId and beanName compared, but not simply the bean type?
        // Theoretically, it is possible to register the same type in two different OSGi bundles
        return this.bundleId == other.bundleId && 
               this.beanName.equals(other.beanName);
	}

    /**
     * @return never <code>null</code>.
     */
    public Bundle getBundle() {
        return this.bundle;
    }
}