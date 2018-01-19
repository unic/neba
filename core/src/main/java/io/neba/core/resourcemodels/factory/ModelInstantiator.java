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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import static io.neba.core.util.Annotations.annotations;
import static java.lang.reflect.Modifier.isStatic;

/**
 * Represents the way in which a model can be instantiated, including resolution of the
 * model's dependencies.
 */
class ModelInstantiator<T> {
    private static final String INJECT_ANNOTATION_NAME = "javax.inject.Inject";
    private final ModelConstructor<T> constructor;
    private final ModelServiceSetter[] setters;

    ModelInstantiator(@Nonnull Class<T> modelType) {
        constructor = resolveConstructor(modelType);
        setters = resolveServiceSetters(modelType);
    }

    /**
     * @return an instance of the model with all dependencies resolved and injected.
     */
    @Nonnull
    public T create(@Nonnull BundleContext context) throws ReflectiveOperationException {
        T instance = this.constructor.instantiate(context);
        for (ModelServiceSetter setter : this.setters) {
            setter.set(context, instance);
        }

        return instance;
    }

    /**
     * @return all public methods annotated with @Inject. Fails if a public method
     * annotated with @Inject does not take exactly one argument.
     */
    @Nonnull
    private static ModelServiceSetter[] resolveServiceSetters(@Nonnull Class<?> modelType) {
        List<ModelServiceSetter> serviceSetters = new ArrayList<>();
        for (Method method : modelType.getMethods()) {
            if (isStatic(method.getModifiers())) {
                continue;
            }
            if (!annotations(method).containsName(INJECT_ANNOTATION_NAME)) {
                continue;
            }

            if (method.getParameterCount() != 1) {
                throw new InvalidModelException("The method " + method + " is annotated with @Inject and must thus take exactly one argument.");
            }

            Filter filter = findFilterAnnotation(method.getParameterAnnotations()[0]);
            Type serviceType = method.getGenericParameterTypes()[0];
            ServiceDependency serviceDependency = new ServiceDependency(serviceType, modelType, filter);
            serviceSetters.add(new ModelServiceSetter(serviceDependency, method));
        }
        return serviceSetters.toArray(new ModelServiceSetter[serviceSetters.size()]);
    }

    /**
     * @return either the default or a @Inject constructor, if present. Fails if neither a public default constructor nor a public  @Inject constructor is
     * present, of if multiple @Inject constructors exist.
     */
    @SuppressWarnings("unchecked")
    @Nonnull
    private static <T> ModelConstructor<T> resolveConstructor(@Nonnull Class<T> modelType) {
        ModelConstructor<T> constructor;
        Constructor<T> injectionConstructor = null,
                defaultConstructor = null;
        for (Constructor c : modelType.getConstructors()) {
            if (c.getParameterCount() == 0) {
                defaultConstructor = c;
            }

            if (!annotations(c).containsName(INJECT_ANNOTATION_NAME)) {
                continue;
            }

            if (injectionConstructor != null) {
                throw new InvalidModelException(
                        "Unable to instantiate model " + modelType + ". " +
                                "Found more than one constructor annotated with @Inject: " + injectionConstructor + ", " + c);
            }

            injectionConstructor = c;
        }

        if (injectionConstructor != null) {
            Type[] parameters = injectionConstructor.getGenericParameterTypes();
            ServiceDependency[] serviceDependencies = new ServiceDependency[parameters.length];
            Annotation[][] parameterAnnotations = injectionConstructor.getParameterAnnotations();
            for (int i = 0; i < parameters.length; ++i) {
                Filter filter = findFilterAnnotation(parameterAnnotations[i]);
                serviceDependencies[i] = new ServiceDependency(parameters[i], modelType, filter);
            }
            constructor = new ModelConstructor<>(injectionConstructor, serviceDependencies);
        } else if (defaultConstructor != null) {
            constructor = new ModelConstructor<>(defaultConstructor);
        } else {
            throw new InvalidModelException("The model " + modelType + " has neither a public default constructor nor a public constructor annotated with @Inject.");
        }

        return constructor;
    }

    private static @Nullable Filter findFilterAnnotation(@Nonnull Annotation[] annotations) {
        Filter filter = null;
        for (Annotation annotation : annotations) {
            if (annotation.annotationType() == Filter.class) {
                filter = (Filter) annotation;
            } else {
                filter = annotations(annotation.annotationType()).get(Filter.class);
            }
            if (filter != null) {
                break;
            }
        }
        return filter;
    }

    /**
     * Represents a setter method with a {@link ServiceDependency}.
     */
    private static class ModelServiceSetter {
        private final ServiceDependency serviceDependency;
        private final Method setter;

        private ModelServiceSetter(@Nonnull ServiceDependency serviceDependency, @Nonnull Method setter) {
            this.serviceDependency = serviceDependency;
            this.setter = setter;
        }

        public void set(@Nonnull BundleContext context, @Nonnull Object model) throws InvocationTargetException, IllegalAccessException {
            Object serviceInstance = this.serviceDependency.resolve(context);

            if (serviceInstance == null) {
                throw new ModelInstantiationException(
                        "Unable to inject a required service dependency via '" + this.setter + "', " +
                                " the Service dependency '" + serviceDependency + "' resolved to null.");

            }

            this.setter.invoke(model, serviceInstance);
        }
    }

    /**
     * Creates model instances via either a default constructor or using a constructor annotated with @Inject, resolving
     * the respective {@link ServiceDependency service depencies}.
     */
    private static class ModelConstructor<T> {
        private final ServiceDependency[] serviceDependencies;
        private final Constructor<T> constructor;

        ModelConstructor(@Nonnull Constructor<T> constructor, @Nullable ServiceDependency... serviceDependencies) {
            this.serviceDependencies = serviceDependencies;
            this.constructor = constructor;
        }

        @Nonnull
        T instantiate(@Nonnull BundleContext context) throws ReflectiveOperationException {
            if (this.serviceDependencies == null || this.serviceDependencies.length == 0) {
                return this.constructor.newInstance();
            }
            Object[] resolvedServices = new Object[this.serviceDependencies.length];
            for (int i = 0; i < resolvedServices.length; ++i) {
                ServiceDependency serviceDependency = this.serviceDependencies[i];
                Object serviceInstance = serviceDependency.resolve(context);
                if (serviceInstance == null) {
                    throw new ModelInstantiationException(
                            "Unable to instantiate the model using '" + this.constructor + "'. " +
                                    "The Service dependency '" + serviceDependency + "' resolved to null.");
                }
                resolvedServices[i] = serviceInstance;
            }
            return this.constructor.newInstance(resolvedServices);
        }
    }
}
