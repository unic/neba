package io.neba.core.resourcemodels.fieldprocessor;

import io.neba.api.resourcemodels.fieldprocessor.CustomFieldProcessor;
import io.neba.core.resourcemodels.mapping.testmodels.TestResourceModel;
import io.neba.core.resourcemodels.metadata.ResourceModelMetaDataRegistrar;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.verification.VerificationMode;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class FieldProcessorRegistrarTest {

    @Mock
    private ResourceModelMetaDataRegistrar resourceModelMetaDataRegistrar;

    @Mock
    private CustomFieldProcessor testFieldProcessor;

    @InjectMocks
    private FieldProcessorRegistrar testee;

    @Test
    public void testProcessorIsAddedWithoutModels() {
        withProcessorsAcceptingAnyField(testFieldProcessor);
        addProcessor(testFieldProcessor);
        assertEmptyCacheEntry(TestResourceModel.class);
    }

    @Test
    public void testProcessorIsAddedWithExistingModels() {
        withProcessorsAcceptingAnyField(testFieldProcessor);
        addModel(TestResourceModel.class);
        addProcessor(testFieldProcessor);
        assertCacheEntry(TestResourceModel.class, testFieldProcessor);
        assertMetaDataInvalidated(TestResourceModel.class, times(1));
    }

    @Test
    public void testModelIsAdded() {
        withProcessorsAcceptingAnyField(testFieldProcessor);
        addProcessor(testFieldProcessor);
        addModel(TestResourceModel.class, testFieldProcessor);
        assertCacheEntry(TestResourceModel.class, testFieldProcessor);
    }

    @Test
    public void testProcessorIsRemoved() {
        withProcessorsAcceptingAnyField(testFieldProcessor);
        addModel(TestResourceModel.class);
        addProcessor(testFieldProcessor);
        removeProcessor(testFieldProcessor);
        assertEmptyCacheEntry(TestResourceModel.class);
        assertMetaDataInvalidated(TestResourceModel.class, times(2));
    }

    @Test
    public void testModelIsRemoved() {
        withProcessorsAcceptingAnyField(testFieldProcessor);
        addModel(TestResourceModel.class);
        addProcessor(testFieldProcessor);
        removeModel(TestResourceModel.class);
        assertEmptyCacheEntry(TestResourceModel.class);
        assertMetaDataInvalidated(TestResourceModel.class, times(1));
    }

    @Test
    public void testProcessorThatNotAcceptsAnyField() {
        addProcessor(testFieldProcessor);
        addModel(TestResourceModel.class);
        assertEmptyCacheEntry(TestResourceModel.class);
    }

    private void withProcessorsAcceptingAnyField(CustomFieldProcessor processor) {
        when(processor.accept(any(Field.class), any())).thenReturn(true);
    }

    private void addModel(Class<?> modelType, CustomFieldProcessor... expectedProcessors) {
        assertThat(testee.addModel(modelType)).isEqualTo(Arrays.asList(expectedProcessors));
    }
    private void removeModel(Class<?> modelType) {
        testee.removeModel(modelType);
    }
    private void addProcessor(CustomFieldProcessor processor) {
        testee.add(processor);
    }
    private void removeProcessor(CustomFieldProcessor processor) {
        testee.remove(processor);
    }

    private void assertEmptyCacheEntry(Class<?> modelType) {
        assertThat(testee.getProcessors(modelType)).isEmpty();
    }
    private void assertCacheEntry(Class<?> modelType, CustomFieldProcessor processor) {
        assertThat(testee.getProcessors(modelType)).contains(processor);
    }
    private void assertMetaDataInvalidated(Class<?> modelType, VerificationMode times) {
        List<Class<?>> modelTypes = new ArrayList<Class<?>>();
        modelTypes.add(modelType);
        verify(resourceModelMetaDataRegistrar, times).invalidate(modelTypes);
    }
}