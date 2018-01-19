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
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

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
    private List<ModelDefinition> modelDefinitions;
    private Map<ModelDefinition, ModelInstantiator<?>> modelMetadata;

    ModelFactory(Bundle bundle) {
        this.bundle = bundle;

        String packages = this.bundle.getHeaders().get("Neba-Packages");

        this.modelDefinitions = packages == null ? emptyList() :
                unmodifiableList(stream(packages.split(","))
                        .map(String::trim)
                        .filter(StringUtils::isNotBlank)
                        .map(this::packageNameToDirectory)
                        .map(this::findClassesInDirectory)
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .flatMap(this::streamUrls)
                        .map(this::urlToClassName)
                        .map(this::loadClass)
                        .filter(o -> o.map(c -> c.isAnnotationPresent(ResourceModel.class)).orElse(false))
                        .map(Optional::get)
                        .map(ClassBasedModelDefinition::new)
                        .distinct()
                        .collect(toList()));

        Map<ModelDefinition, ModelInstantiator<?>> metaData = new HashMap<>();
        for (ModelDefinition definition : this.modelDefinitions) {
            metaData.put(definition, new ModelInstantiator<>(definition.getType()));
        }

        this.modelMetadata = metaData;
    }

    @Nonnull
    @Override
    public Collection<ModelDefinition> getModelDefinitions() {
        return this.modelDefinitions;
    }

    @SuppressWarnings("unchecked")
    private Optional<Enumeration<URL>> findClassesInDirectory(String directory) {
        return ofNullable(bundle.findEntries(directory, "*.class", true));
    }

    private String packageNameToDirectory(String packageName) {
        return '/' + packageName.replace('.', '/');
    }

    private String urlToClassName(URL url) {
        String file = url.getFile();
        final String classFileName = file.substring(1, file.length() - ".class".length());
        return classFileName.replace('/', '.');
    }

    private Optional<Class<?>> loadClass(String name) {
        try {
            return of(this.bundle.loadClass(name));
        } catch (ClassNotFoundException e) {
            return empty();
        }
    }

    private Stream<? extends URL> streamUrls(Enumeration<URL> urls) {
        List<URL> l = new ArrayList<>(32);
        while (urls.hasMoreElements()) {
            l.add(urls.nextElement());
        }
        return l.stream();
    }

    @Nonnull
    @Override
    public Object getModel(@Nonnull ModelDefinition modelDefinition) {
        ModelInstantiator modelInstantiator = this.modelMetadata.get(modelDefinition);
        if (modelInstantiator == null) {
            throw new IllegalStateException("Unable to instantiate " + modelDefinition + ", there is no model metadata for this model type in this factory.");
        }
        try {
            return modelInstantiator.create(this.bundle.getBundleContext());
        } catch (ReflectiveOperationException e) {
            throw new ModelInstantiationException("Unable to instantiate model " + modelDefinition, e);
        }
    }
}