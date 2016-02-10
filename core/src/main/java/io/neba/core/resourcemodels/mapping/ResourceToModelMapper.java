/**
 * Copyright 2013 the original author or authors.
 * <p/>
 * Licensed under the Apache License, Version 2.0 the "License";
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/

package io.neba.core.resourcemodels.mapping;

import io.neba.api.resourcemodels.ResourceModelPostProcessor;
import io.neba.core.resourcemodels.metadata.MappedFieldMetaData;
import io.neba.core.resourcemodels.metadata.ResourceModelMetaData;
import io.neba.core.resourcemodels.metadata.ResourceModelMetaDataRegistrar;
import io.neba.core.util.OsgiBeanSource;
import org.apache.sling.api.resource.Resource;
import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.Advised;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static java.lang.System.currentTimeMillis;
import static org.apache.commons.lang.StringUtils.join;
import static org.springframework.util.Assert.notNull;

/**
 * Maps the properties of a {@link Resource} onto a {@link io.neba.api.annotations.ResourceModel} using
 * the {@link FieldValueMappingCallback}. Applies the registered
 * {@link ResourceModelPostProcessor post processors} to the model before and
 * after the fields are mapped.
 *
 * @author Olaf Otto
 */
@Service
public class ResourceToModelMapper {
    private final List<ResourceModelPostProcessor> postProcessors = new ArrayList<>();
    @Autowired
    private ModelProcessor modelProcessor;
    @Autowired
    private NestedMappingSupport nestedMappingSupport;
    @Autowired
    private AnnotatedFieldMappers annotatedFieldMappers;
    @Autowired
    private ResourceModelMetaDataRegistrar resourceModelMetaDataRegistrar;

    /**
     * @param resource must not be <code>null</code>.
     * @param modelSource   must not be <code>null</code>.
     * @param <T>      the bean type.
     * @return never <code>null</code>.
     */
    public <T> T map(final Resource resource, final OsgiBeanSource<T> modelSource) {
        notNull(resource, "Method argument resource must not be null.");
        notNull(modelSource, "Method argument modelSource must not be null.");

        T model = null;

        final Class<?> beanType = modelSource.getBeanType();
        final ResourceModelMetaData metaData = this.resourceModelMetaDataRegistrar.get(beanType);
        final Mapping<T> mapping = new Mapping<>(resource.getPath(), metaData);
        // Do not track mapping time for nested resource models of the same type: this would yield
        // a useless average and total mapping time as the mapping durations would sum up multiple times.
        final boolean trackMappingDuration = !this.nestedMappingSupport.hasOngoingMapping(metaData);

        final Mapping<T> alreadyOngoingMapping = this.nestedMappingSupport.begin(mapping);

        if (alreadyOngoingMapping == null) {
            try {
                // Phase 1: Obtain bean instance. All standard bean lifecycle phases (such as @PostConstruct)
                // and processors are executed during this invocation.
                final T bean = modelSource.getBean();

                metaData.getStatistics().countInstantiation();

                // Phase 2: Retain the bean prior to mapping in order to return it if the mapping results in a cycle.
                mapping.setMappedModel(bean);

                // Phase 3: Map the bean (may create a cycle).

                // Retain current time for statistics
                final long startTimeInMs = trackMappingDuration ? currentTimeMillis() : 0;

                model = map(resource, bean, metaData, modelSource.getFactory());

                if (trackMappingDuration) {
                    // Update statistics with mapping duration
                    metaData.getStatistics().countMappingDuration((int) (currentTimeMillis() - startTimeInMs));
                }

            } finally {
                this.nestedMappingSupport.end(mapping);
            }
        } else {
            // Yield the currently mapped bean.
            model = alreadyOngoingMapping.getMappedModel();

            if (model == null) {
                // This can only be the case if a cycle was introduced during phase 1.
                // Cycles introduced during bean initialization in the bean factory always
                // represent unresolvable programming errors (the bean depends on itself to initialize itself),
                // thus we must raise an exception.
                throw new CycleInBeanInitializationException("Unable to provide bean " + beanType +
                        " for resource " + resource + ". The bean initialization resulted in a cycle: "
                        + join(this.nestedMappingSupport.getOngoingMappings(), " >> ") + " >> " + mapping + ". " +
                        "Does the bean depend on itself to initialize, e.g. in a @PostConstruct method?");
            }
        }

        return model;
    }

    @SuppressWarnings("unchecked")
    private <T> T getTargetObjectOfAdvisedBean(Advised bean) {
        TargetSource targetSource = bean.getTargetSource();
        if (targetSource == null) {
            throw new IllegalStateException("Model " + bean + " is " + Advised.class.getName() + ", but its target source is null.");
        }
        Object target;
        try {
            target = targetSource.getTarget();
        } catch (Exception e) {
            throw new IllegalStateException("Unable to obtain the target of the advised model " + bean + ".", e);
        }
        if (target == null) {
            throw new IllegalStateException("The advised target of bean " + bean + " must not be null.");
        }
        return (T) target;
    }

    private <T> T map(final Resource resource, final T bean, final ResourceModelMetaData metaData, final BeanFactory factory) {
        T preprocessedModel = preProcess(resource, bean, factory);

        T model = preprocessedModel;
        // Unwrap proxied beans prior to mapping. The mapping must access the target
        // bean's fields in order to perform value injection there.
        if (preprocessedModel instanceof Advised) {
            model = getTargetObjectOfAdvisedBean((Advised) bean);
        }

        final FieldValueMappingCallback callback = new FieldValueMappingCallback(model, resource, factory, this.annotatedFieldMappers);

        for (MappedFieldMetaData mappedFieldMetaData : metaData.getMappableFields()) {
            callback.doWith(mappedFieldMetaData);
        }

        // Do not expose the unwrapped model to the post processors, use the proxy (if any) instead.
        return postProcess(resource, preprocessedModel, factory);
    }

    private <T> T preProcess(final Resource resource, final T model, final BeanFactory factory) {
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

    private <T> T postProcess(final Resource resource, final T model, final BeanFactory factory) {
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

    public void bind(ResourceModelPostProcessor postProcessor) {
        this.postProcessors.add(postProcessor);
    }

    public void unbind(ResourceModelPostProcessor postProcessor) {
        if (postProcessor == null) {
            return;
        }
        this.postProcessors.remove(postProcessor);
    }
}
