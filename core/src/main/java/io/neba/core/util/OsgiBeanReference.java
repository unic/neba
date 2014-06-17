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

import static org.springframework.util.Assert.notNull;

/**
 * References a bean from a {@link org.osgi.framework.Bundle}.
 *
 * @param <T> the type of the bean.
 * @author Olaf Otto
 */
public class OsgiBeanReference<T> {
    private final T bean;
    private final long bundleId;
    private final Class<?> beanClass;
    private final int hashCode;

    public OsgiBeanReference(T bean, long bundleId) {
        notNull(bean, "Constructor argument bean must not be null.");
        this.bean = bean;
        this.bundleId = bundleId;
        this.beanClass = bean.getClass();
        this.hashCode = new HashCodeBuilder().append(beanClass.getName()).append(bundleId).toHashCode();
    }

    public T getBean() {
        return bean;
    }

    public long getBundleId() {
        return bundleId;
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

        OsgiBeanReference<?> other = (OsgiBeanReference<?>) obj;

        return this.bundleId == other.bundleId &&
                this.beanClass.getName().equals(other.beanClass.getName());
    }

    @Override
    public String toString() {
        return "Bean with type " + '"' + this.beanClass.getName() + '"' + " from bundle with id " + this.bundleId;
    }
}
