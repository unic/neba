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
package io.neba.core.resourcemodels.factory;

import io.neba.api.annotations.Filter;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static io.neba.core.util.ReflectionUtil.getLowerBoundOfSingleTypeParameter;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.reflect.TypeUtils.getRawType;
import static org.osgi.framework.FrameworkUtil.createFilter;

/**
 * Represents a dependency to OSGi services with an optional {@link Filter}.
 */
class ServiceDependency {
    private final Class<?> serviceType;
    private final Filter filter;
    private final boolean hasFilter;
    private final boolean isOptional;
    private final boolean isList;

    /**
     * @param serviceType Either the actual service type (e.g. the service interface), a Optional&lt;ServiceTyp&gt;.
     *                    If a {@link Filter} is present, List&lt;ServiceType&gt; is supported as well.
     * @param modelType the type using the service dependency, used to resolve generic type parameters.
     * @param filter may be <code>null</code>.
     */
    ServiceDependency(@Nonnull Type serviceType, @Nonnull Class<?> modelType, @Nullable Filter filter) {
        final Class<?> actualServiceType;
        final Class<?> rawType = getRawType(serviceType, modelType);

        if (rawType == null) {
            throw new InvalidModelException(
                    "Unable to resolve the service dependency from  '" + modelType + "' to '" + serviceType + "'," +
                            " resolving the actual class of the model returned null.");
        }

        if (filter != null && rawType.isAssignableFrom(List.class)) {
            // If a filter annotation is present, the service type may be a List<X> of services.
            // In this case, the service type is X.
            actualServiceType = getRawType(getLowerBoundOfSingleTypeParameter(serviceType), modelType);
            this.isList = true;
            this.isOptional = false;
        } else if (serviceType instanceof ParameterizedType && ((ParameterizedType) serviceType).getRawType() == Optional.class) {
            // If an the type is Optional<X>, the service type type is X.
            actualServiceType = getRawType(getLowerBoundOfSingleTypeParameter(serviceType), modelType);
            this.isOptional = true;
            this.isList = false;
        } else {
            actualServiceType = rawType;
            this.isOptional = false;
            this.isList = false;
        }

        if (actualServiceType == null) {
            throw new InvalidModelException(
                    "Unable to resolve the service dependency from  '" + modelType + "' to '" + serviceType + "'," +
                            " resolving the actual class of the service type returned null.");
        }

        if (filter != null && isNotBlank(filter.value())) {
            try {
                createFilter(filter.value());
            } catch (InvalidSyntaxException e) {
                throw new InvalidModelException(
                        "The syntax of the filter '" + filter.value() + "' " +
                                "for the service dependency '" + serviceType + "' " +
                                "of the model '" + modelType + "' is invalid", e);
            }
            this.hasFilter = true;
        } else {
            this.hasFilter = false;
        }
        this.filter = filter;
        this.serviceType = actualServiceType;

    }

    @Nullable
    public Object resolve(@Nonnull BundleContext context) {
        Object resolved = null;

        if (this.hasFilter) {
            Collection<? extends ServiceReference<?>> serviceReferences;
            try {
                serviceReferences = context.getServiceReferences(this.serviceType, this.filter.value());
            } catch (InvalidSyntaxException e) {
                // This should not happen as the filter syntax is checked during meta data construction.
                throw new IllegalStateException("Unable to retrieve service references of type '" + this.serviceType + "'.", e);
            }

            if (this.isList) {
                List<Object> serviceInstances = new ArrayList<>(serviceReferences.size());
                for (ServiceReference<?> reference : serviceReferences) {
                    Object serviceInstance = context.getService(reference);
                    if (serviceInstance != null) {
                        serviceInstances.add(serviceInstance);
                    }
                }
                // Lists are implicitly optional as they have a natural representation of emptiness,
                // thus we are done.
                return serviceInstances;
            }

            if (serviceReferences.size() > 1) {
                throw new ModelInstantiationException(
                        "Unable to resolve the service dependency " + this + ", " +
                                "got more than one matching service instance: " + serviceReferences + ".");
            }
            if (!serviceReferences.isEmpty()) {
                resolved = context.getService(serviceReferences.iterator().next());
            }

        } else {
            ServiceReference<?> reference = context.getServiceReference(this.serviceType);
            if (reference != null) {
                resolved = context.getService(reference);
            }
        }

        return this.isOptional ? ofNullable(resolved) : resolved;
    }

    @Override
    public String toString() {
        return "ServiceDependency{" +
                "serviceType=" + serviceType +
                ", filter=" + filter +
                '}';
    }
}
