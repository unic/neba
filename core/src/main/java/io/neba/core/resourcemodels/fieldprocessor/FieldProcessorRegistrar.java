package io.neba.core.resourcemodels.fieldprocessor;

import io.neba.api.resourcemodels.fieldprocessor.CustomFieldProcessor;
import io.neba.core.resourcemodels.metadata.ResourceModelMetaDataRegistrar;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import javax.inject.Inject;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Registers {@link CustomFieldProcessor} per resource model.
 * When a processor is added or removed, the affected resource model metadata has to be
 * {@link ResourceModelMetaDataRegistrar#invalidate(List) invalidated}.
 *
 * @author christoph.huber
 * @since 05.12.2014
 */
@Service
public class FieldProcessorRegistrar {
    @Inject
    private ResourceModelMetaDataRegistrar metaDataRegistrar;

    private Map<Class<?>, List<CustomFieldProcessor>> resourceModelProcessors =
            Collections.synchronizedMap(new HashMap<Class<?>, List<CustomFieldProcessor>>());
    private List<CustomFieldProcessor> fieldProcessors =
            Collections.synchronizedList(new LinkedList<CustomFieldProcessor>());

    public void add(CustomFieldProcessor customFieldProcessor) {
        fieldProcessors.add(customFieldProcessor);
        metaDataRegistrar.invalidate(addProcessorToResourceModels(customFieldProcessor));
    }
    public void remove(CustomFieldProcessor customFieldProcessor) {
        fieldProcessors.remove(customFieldProcessor);
        metaDataRegistrar.invalidate(removeProcessorFromResourceModels(customFieldProcessor));
    }

    private List<Class<?>> addProcessorToResourceModels(CustomFieldProcessor processor) {
        List<Class<?>> changedModels = new LinkedList<Class<?>>();
        for (Map.Entry<Class<?>, List<CustomFieldProcessor>> entry : resourceModelProcessors.entrySet()) {
            for (Field field : entry.getKey().getDeclaredFields()) {
                if (processor.accept(field, entry.getKey())) {
                    entry.getValue().add(processor);
                    changedModels.add(entry.getKey());
                    break;
                }
            }
        }
        return changedModels;
    }
    private List<Class<?>> removeProcessorFromResourceModels(CustomFieldProcessor processor) {
        List<Class<?>> changedModels = new LinkedList<Class<?>>();
        for (Map.Entry<Class<?>, List<CustomFieldProcessor>> entry : resourceModelProcessors.entrySet()) {
            if (entry.getValue().contains(processor)) {
                entry.getValue().remove(processor);
                changedModels.add(entry.getKey());
            }
        }
        return changedModels;
    }

    /**
     * This would actually require a complete invalidation of the cached resource model metadata.
     * But since both registrars are in the same bundle, both caches should be cleared anyway.
     */
    @PreDestroy
    public void cleanUp() {
        fieldProcessors.clear();
        resourceModelProcessors.clear();
    }

    /**
     * Is called when a new resource model is registered.
     * @return All processors that affect the provided model.
     */
    public List<CustomFieldProcessor> addModel(Class<?> modelType) {
        List<CustomFieldProcessor> modelProcessors = new LinkedList<CustomFieldProcessor>();
        for (CustomFieldProcessor processor : fieldProcessors) {
            for (Field field : modelType.getDeclaredFields()) {
                if (processor.accept(field, modelType)) {
                    modelProcessors.add(processor);
                    break;
                }
            }
        }
        resourceModelProcessors.put(modelType, modelProcessors);
        return modelProcessors;
    }

    /**
     * Is called when a registered resource model is deactivated.
     */
    public void removeModel(Class<?> modelType) {
        resourceModelProcessors.remove(modelType);
    }

    /**
     * Returns all processors appliable to at least one field of <code>modelType</code>.
     * @return Never <code>null</code>.
     */
    public List<CustomFieldProcessor> getProcessors(Class<?> modelType) {
        List<CustomFieldProcessor> processors = resourceModelProcessors.get(modelType);
        if (processors == null) {
            return Collections.emptyList();
        }
        return processors;
    }
}
