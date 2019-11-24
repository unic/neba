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
import io.neba.core.util.Annotations;
import io.neba.core.util.ReflectionUtil;
import org.osgi.framework.BundleContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import static io.neba.core.util.Annotations.annotations;
import static io.neba.core.util.ReflectionUtil.makeAccessible;
import static io.neba.core.util.ReflectionUtil.methodsOf;
import static java.lang.reflect.Modifier.isFinal;
import static java.lang.reflect.Modifier.isStatic;
import static org.apache.commons.lang3.ArrayUtils.reverse;
import static org.apache.commons.lang3.reflect.FieldUtils.getAllFields;

/**
 * Represents the way in which a model can be instantiated, including resolution of the
 * model's dependencies.
 */
class ModelInstantiator<T> {
    private static final String INJECT_ANNOTATION_NAME = "javax.inject.Inject";
    private static final String POSTCONSTRUCT_ANNOTATION_NAME = "javax.annotation.PostConstruct";

    private final ModelConstructor<T> constructor;
    private final ModelServiceSetter[] setters;
    private final Method[] postConstructMethods;
    private final ModelFieldInjection[] fieldInjections;

    ModelInstantiator(@Nonnull Class<T> modelType) {
        this.constructor = resolveConstructor(modelType);
        this.setters = resolveServiceSetters(modelType);
        this.postConstructMethods = resolvePostConstructMethods(modelType);
        this.fieldInjections = resolveServiceFieldInjections(modelType);
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

        for (ModelFieldInjection injection : this.fieldInjections) {
            injection.set(context, instance);
        }

        for (Method m : this.postConstructMethods) {
            m.invoke(instance);
        }

        return instance;
    }

    private ModelFieldInjection[] resolveServiceFieldInjections(@Nonnull Class<T> modelType) {
        List<ModelFieldInjection> fieldInjectionList = new ArrayList<>();

        for (Field field : getAllFields(modelType)) {
            if (isStatic(field.getModifiers()) || isFinal(field.getModifiers())) {
                continue;
            }

            final Annotations annotations = annotations(field);
            if (!annotations.containsName(INJECT_ANNOTATION_NAME)) {
                continue;
            }

            final Filter filter = annotations.get(Filter.class);
            final ServiceDependency serviceDependency = new ServiceDependency(field.getGenericType(), modelType, filter);

            makeAccessible(field);
            fieldInjectionList.add(new ModelFieldInjection(serviceDependency, field));
        }

        return fieldInjectionList.toArray(new ModelFieldInjection[0]);
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
        return serviceSetters.toArray(new ModelServiceSetter[0]);
    }

    @Nonnull
    private Method[] resolvePostConstructMethods(@Nonnull Class<T> modelType) {
        Method[] postConstructMethods = methodsOf(modelType).stream()
                .filter(m -> annotations(m).containsName(POSTCONSTRUCT_ANNOTATION_NAME))
                .peek(m -> {
                    if (isStatic(m.getModifiers())) {
                        throw new InvalidModelException("The @PostConstruct callback '" + m + "' must not be static.");
                    }
                    if (m.getParameterCount() != 0) {
                        throw new InvalidModelException("The @PostConstruct callback '" + m + "' must not take any arguments.");
                    }
                })
                .peek(ReflectionUtil::makeAccessible)
                .toArray(Method[]::new);

        // The post construct methods shall be applied in inverse order, i.e. the once stemming from base classes shall be called first.
        // The assumption is that child classes depend on the initialization of their base classes. Note that this is
        // a deviation from the contract described in @PostConstruct, which states that there shall only be one post construct method.
        // However, invoking all is the de facto standard used e.g. by the spring framework.
        reverse(postConstructMethods);

        return postConstructMethods;
    }

    /**
     * @return either the default or an @Inject constructor, if present. Fails if neither a public default constructor nor a public @Inject constructor is
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

    @Nullable
    private static Filter findFilterAnnotation(@Nonnull Annotation[] annotations) {
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
     * Represents a field with a  {@link ServiceDependency}.
     */
    private static class ModelFieldInjection {
        private final ServiceDependency serviceDependency;
        private final Field field;

        private ModelFieldInjection(ServiceDependency serviceDependency, Field field) {
            this.serviceDependency = serviceDependency;
            this.field = makeAccessible(field);
        }

        public void set(@Nonnull BundleContext context, @Nonnull Object model) throws IllegalAccessException {
            Object serviceInstance = this.serviceDependency.resolve(context);

            if (serviceInstance == null) {
                throw new ModelInstantiationException(
                        "Unable to inject a required service dependency via '" + this.field + "', " +
                                " the Service dependency '" + serviceDependency + "' resolved to null.");

            }

            this.field.set(model, serviceInstance);
        }
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
