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
 * References a model from a {@link org.osgi.framework.Bundle}.
 *
 * @param <T> the type of the model.
 * @author Olaf Otto
 */
public class OsgiModelReference<T> {
    private final T model;
    private final long bundleId;
    private final Class<?> modelClass;
    private final int hashCode;

    OsgiModelReference(T model, long bundleId) {
        if (model == null) {
            throw new IllegalArgumentException("Constructor argument model must not be null");
        }
        this.model = model;
        this.bundleId = bundleId;
        this.modelClass = model.getClass();
        this.hashCode = new HashCodeBuilder().append(modelClass.getName()).append(bundleId).toHashCode();
    }

    public T getModel() {
        return model;
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

        OsgiModelReference<?> other = (OsgiModelReference<?>) obj;

        return this.bundleId == other.bundleId &&
                this.modelClass.getName().equals(other.modelClass.getName());
    }

    @Override
    public String toString() {
        return "Model with type " + '"' + this.modelClass.getName() + '"' + " from bundle with id " + this.bundleId;
    }
}
