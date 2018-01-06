/*
  Copyright 2013 the original author or authors.
  <p/>
  Licensed under the Apache License, Version 2.0 the "License";
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at
  <p/>
  http://www.apache.org/licenses/LICENSE-2.0
  <p/>
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
 */

package io.neba.core.resourcemodels.mapping;

import io.neba.api.spi.AopSupport;
import io.neba.api.spi.ResourceModelFactory;
import io.neba.api.spi.ResourceModelPostProcessor;
import io.neba.core.resourcemodels.metadata.MappedFieldMetaData;
import io.neba.core.resourcemodels.metadata.ResourceModelMetaData;
import io.neba.core.resourcemodels.metadata.ResourceModelMetaDataRegistrar;
import io.neba.core.util.OsgiModelSource;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.References;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.Resource;

import java.util.ArrayList;
import java.util.List;

import static java.lang.System.currentTimeMillis;
import static org.apache.commons.lang3.StringUtils.join;
import static org.apache.felix.scr.annotations.ReferenceCardinality.OPTIONAL_MULTIPLE;
import static org.apache.felix.scr.annotations.ReferencePolicy.DYNAMIC;

/**
 * Maps the properties of a {@link Resource} onto a {@link io.neba.api.annotations.ResourceModel} using
 * the {@link FieldValueMappingCallback}. Applies the registered
 * {@link ResourceModelPostProcessor post processors} to the model before and
 * after the fields are mapped.
 *
 * @author Olaf Otto
 */
@Service(ResourceToModelMapper.class)
@Component
@References({
        @Reference(referenceInterface = ResourceModelPostProcessor.class,
                cardinality = OPTIONAL_MULTIPLE,
                policy = DYNAMIC,
                name = "processors",
                bind = "bindProcessor",
                unbind = "unbindProcessor"),
        @Reference(referenceInterface = AopSupport.class,
                cardinality = OPTIONAL_MULTIPLE,
                policy = DYNAMIC,
                name = "aopSupport",
                bind = "bindAopSupport",
                unbind = "unbindAopSupport")

})
public class ResourceToModelMapper {
    private final List<ResourceModelPostProcessor> postProcessors = new ArrayList<>();
    private final List<AopSupport> aopSupports = new ArrayList<>();

    @Reference
    private ModelProcessor modelProcessor;
    @Reference
    private NestedMappingSupport nestedMappingSupport;
    @Reference
    private AnnotatedFieldMappers fieldMappers;
    @Reference
    private PlaceholderVariableResolvers variableResolvers;
    @Reference
    private ResourceModelMetaDataRegistrar resourceModelMetaDataRegistrar;

    /**
     * @param resource    must not be <code>null</code>.
     * @param modelSource must not be <code>null</code>.
     * @param <T>         the model type.
     * @return never <code>null</code>.
     */
    public <T> T map(final Resource resource, final OsgiModelSource<T> modelSource) {
        if (resource == null) {
            throw new IllegalArgumentException("Method argument resource must not be null");
        }
        if (modelSource == null) {
            throw new IllegalArgumentException("Method argument modelSource must not be null");
        }

        T model;

        final Class<?> modelType = modelSource.getModelType();
        final ResourceModelMetaData metaData = this.resourceModelMetaDataRegistrar.get(modelType);
        final Mapping<T> mapping = new Mapping<>(resource.getPath(), metaData);
        // Do not track mapping time for nested resource models of the same type: this would yield
        // a useless average and total mapping time as the mapping durations would sum up multiple times.
        final boolean trackMappingDuration = !this.nestedMappingSupport.hasOngoingMapping(metaData);

        final Mapping<T> alreadyOngoingMapping = this.nestedMappingSupport.begin(mapping);

        if (alreadyOngoingMapping == null) {
            try {
                // Phase 1: Obtain model instance. All standard model lifecycle phases (such as @PostConstruct)
                // and processors are executed during this invocation.
                final T mappedModel = modelSource.getModel();

                metaData.getStatistics().countInstantiation();

                // Phase 2: Retain the model prior to mapping in order to return it if the mapping results in a cycle.
                mapping.setMappedModel(mappedModel);

                // Phase 3: Map the model (may create a cycle).

                // Retain current time for statistics
                final long startTimeInMs = trackMappingDuration ? currentTimeMillis() : 0;

                model = map(resource, mappedModel, metaData, modelSource.getFactory());

                if (trackMappingDuration) {
                    // Update statistics with mapping duration
                    metaData.getStatistics().countMappingDuration((int) (currentTimeMillis() - startTimeInMs));
                }

            } finally {
                this.nestedMappingSupport.end(mapping);
            }
        } else {
            // Yield the currently mapped model.
            model = alreadyOngoingMapping.getMappedModel();

            if (model == null) {
                // This can only be the case if a cycle was introduced during phase 1.
                // Cycles introduced during model initialization in the model factory always
                // represent unresolvable programming errors (the model depends on itself to initialize itself),
                // thus we must raise an exception.
                throw new CycleInModelInitializationException("Unable to provide model " + modelType +
                        " for resource " + resource + ". The model initialization resulted in a cycle: "
                        + join(this.nestedMappingSupport.getOngoingMappings(), " >> ") + " >> " + mapping + ". " +
                        "Does the model depend on itself to initialize, e.g. in a @PostConstruct method?");
            }
        }

        return model;
    }

    private <T> T map(final Resource resource, final T model, final ResourceModelMetaData metaData, final ResourceModelFactory factory) {
        T preprocessedModel = preProcess(resource, model, factory);

        T fieldInjectionViewOnPreprocessedModel = prepareAopEnhancedModelTypes(preprocessedModel);

        final FieldValueMappingCallback callback = new FieldValueMappingCallback(fieldInjectionViewOnPreprocessedModel, resource, factory, this.fieldMappers, this.variableResolvers);

        for (MappedFieldMetaData mappedFieldMetaData : metaData.getMappableFields()) {
            callback.doWith(mappedFieldMetaData);
        }

        // Do not expose the unwrapped model to the post processors, use the proxy (if any) instead.
        return postProcess(resource, preprocessedModel, factory);
    }

    @SuppressWarnings("unchecked")
    private <T> T prepareAopEnhancedModelTypes(T preprocessedModel) {
        T model = preprocessedModel;
        for (AopSupport aopSupport : this.aopSupports) {
            model = (T) aopSupport.prepareForFieldInjection(model);
        }
        return model;
    }

    private <T> T preProcess(final Resource resource, final T model, final ResourceModelFactory factory) {
        final ResourceModelMetaData metaData = this.resourceModelMetaDataRegistrar.get(model.getClass());
        this.modelProcessor.processBeforeMapping(metaData, model);

        T currentModel = model;
        for (ResourceModelPostProcessor processor : this.postProcessors) {
            T processedModel = processor.processBeforeMapping(currentModel, resource, factory);
            if (processedModel != null) {
                currentModel = processedModel;
            }
        }
        return currentModel;
    }

    private <T> T postProcess(final Resource resource, final T model, final ResourceModelFactory factory) {
        final ResourceModelMetaData metaData = this.resourceModelMetaDataRegistrar.get(model.getClass());
        this.modelProcessor.processAfterMapping(metaData, model);

        T currentModel = model;
        for (ResourceModelPostProcessor processor : this.postProcessors) {
            T processedModel = processor.processAfterMapping(currentModel, resource, factory);
            if (processedModel != null) {
                currentModel = processedModel;
            }
        }
        return currentModel;
    }

    protected void bindProcessor(ResourceModelPostProcessor postProcessor) {
        this.postProcessors.add(postProcessor);
    }

    protected void unbindProcessor(ResourceModelPostProcessor postProcessor) {
        if (postProcessor == null) {
            return;
        }
        this.postProcessors.remove(postProcessor);
    }

    protected void bindAopSupport(AopSupport aopSupport) {
        this.aopSupports.add(aopSupport);
    }

    protected void unbindAopSupport(AopSupport support) {
        if (support == null) {
            return;
        }
        this.aopSupports.remove(support);
    }
}
