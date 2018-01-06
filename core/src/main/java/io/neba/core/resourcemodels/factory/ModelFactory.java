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

import io.neba.api.annotations.ResourceModel;
import io.neba.api.spi.ResourceModelFactory;
import org.apache.commons.lang3.StringUtils;
import org.osgi.framework.Bundle;

import javax.annotation.Nonnull;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import static io.neba.core.util.Annotations.annotations;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;

/**
 * Finds {@link ResourceModel} in bundles with a <code>Neba-Packages</code> header and provides the respective
 * {@link io.neba.api.spi.ResourceModelFactory.ModelDefinition model definitions} and {@link #getModel(ModelDefinition) means to instantiate}
 * the models, including injection of <em>OSGi service dependencies</em> via {@link javax.inject.Inject} and {@link io.neba.api.annotations.Filter}.
 */
class ModelFactory implements ResourceModelFactory {
    private final Bundle bundle;
    private List<ModelDefinition> modelDefinitions = null;

    ModelFactory(Bundle bundle) {
        this.bundle = bundle;
    }

    @Nonnull
    @Override
    public Collection<ModelDefinition> getModelDefinitions() {
        if (modelDefinitions == null) {
            String packages = this.bundle.getHeaders().get("Neba-Packages");

            modelDefinitions = packages == null ? emptyList() :
                    unmodifiableList(stream(packages.split(","))
                    .map(String::trim)
                    .filter(StringUtils::isNotBlank)
                    .map(packageNameToDirectory())
                    .map(findClassesInDirectory())
                    .flatMap(streamUrls())
                    .map(urlToClassName())
                    .map(loadClass())
                    .filter(o -> o.map(c -> c.isAnnotationPresent(ResourceModel.class)).orElse(false))
                    .map(Optional::get)
                    .map(ClassBasedModelDefinition::new)
                    .distinct()
                    .collect(toList()));
        }

        return modelDefinitions;
    }

    @SuppressWarnings("unchecked")
    private Function<String, Optional<Enumeration<URL>>> findClassesInDirectory() {
        return s -> ofNullable(bundle.findEntries(s, "*.class", true));
    }

    private Function<String, String> packageNameToDirectory() {
        return s ->  '/' + s.replace('.', '/');
    }

    private Function<String, Optional<Class<?>>> loadClass() {
        return name -> {
            try {
                return of(this.bundle.loadClass(name));
            } catch (ClassNotFoundException e) {
                return empty();
            }
        };
    }

    private Function<URL, String> urlToClassName() {
        return u -> {
            String file = u.getFile();
            final String classFileName = file.substring(1, file.length() - ".class".length());
            return classFileName.replace('/', '.');
        };
    }

    private Function<Optional<Enumeration<URL>>, Stream<? extends URL>> streamUrls() {
        return o -> o.map(e -> {
            List<URL> urls = new ArrayList<>(32);
            while (e.hasMoreElements()) {
                urls.add(e.nextElement());
            }
            return urls;
        }).orElse(emptyList()).stream();
    }

    @Nonnull
    @Override
    public Object getModel(@Nonnull ModelDefinition modelDefinition) {
        // Step 1; Find a suitable constructor. This is either a constructor annotated with @Inject or
        // a no.args default constructor.

        // TODO: cache the resolved meta data
        Constructor injectionConstructor = null,
                    defaultConstructor = null;
        for (Constructor c : modelDefinition.getType().getConstructors()) {
            if (c.getParameterCount() == 0) {
                defaultConstructor = c;
            }

            if (!annotations(c).containsName("javax.inject.Inject")) {
                continue;
            }

            if (injectionConstructor != null) {
                throw new InvalidModelException(
                        "Unable to instantiate model " + modelDefinition.getType() + ". " +
                        "Found more than one constructor annotated with @Inject: " + injectionConstructor + ", " + c);
            }

            injectionConstructor = c;
        }

        Object instance = null;

        if (injectionConstructor != null) {
            // FIXME: Service lookup for each argument, consider @Filter
            instance = null;
        }

        if (defaultConstructor != null) {
            try {
                instance = defaultConstructor.newInstance();
            } catch (Exception e) {
                throw new ModelInstantiationException("Unable to invoke " + defaultConstructor + ".", e);
            }
        }

        if (instance == null) {
            throw new InvalidModelException(
                    "Unable to instantiate model " + modelDefinition.getType() + ". " +
                            "The model has neither a public default constructor nor a public constructor annotated with @Inject."
            );
        }

        // FIXME: for each @Inject field, perform service injection.

        return instance;
    }

}
