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
import io.neba.core.resourcemodels.metadata.MethodMetaData;
import io.neba.core.resourcemodels.metadata.ResourceModelMetaData;
import io.neba.core.resourcemodels.metadata.ResourceModelMetaDataRegistrar;
import io.neba.core.resourcemodels.metadata.ResourceModelStatistics;
import io.neba.core.util.OsgiModelSource;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Olaf Otto
 */
@RunWith(MockitoJUnitRunner.class)
public class ResourceToModelMapperTest {
    /**
     * @author Olaf Otto
     */
    public static class TestModel {
    }

    /**
     * @author Olaf Otto
     */
    public static class AopTestModel extends TestModel {
    }

    @Mock
    private Resource resource;
    @Mock
    private ValueMap valueMap;
    @Mock
    private ResourceModelFactory factory;
    @Mock
    private ModelProcessor modelProcessor;
    @Mock
    private ResourceModelMetaDataRegistrar resourceModelMetaDataRegistrar;
    @Mock
    private NestedMappingSupport nestedMappingSupport;
    @Mock
    private ResourceModelMetaData resourceMetaData;
    @Mock
    private ResourceModelStatistics resourceModelStatistics;
    @Mock
    private AnnotatedFieldMappers annotatedFieldMappers;
    @Mock
    private PlaceholderVariableResolvers placeholderVariableResolvers;
    @Mock
    private Mapping<Object> ongoingMapping;

    private TestModel model;
    private Class<?> modelType;
    private ResourceModelPostProcessor postProcessor;
    private TestModel modelReturnedFromPostProcessor;
    private TestModel mappedModel;
    private AopSupport aopSupport;

    @InjectMocks
    private ResourceToModelMapper testee;

    @Before
    @SuppressWarnings("unchecked")
    public void prepareMapper() {
        when(this.resource.adaptTo(eq(ValueMap.class)))
                .thenReturn(this.valueMap);

        when(this.resource.getPath())
                .thenReturn("/resource/path");

        when(this.resourceModelMetaDataRegistrar.get(isA(Class.class)))
                .thenReturn(this.resourceMetaData);

        when(this.nestedMappingSupport.begin(isA(Mapping.class)))
                .thenReturn(null);

        when(this.resourceMetaData.getMappableFields())
                .thenReturn(new MappedFieldMetaData[]{});

        when(this.resourceMetaData.getPostMappingMethods())
                .thenReturn(new MethodMetaData[]{});

        when(this.resourceMetaData.getPreMappingMethods())
                .thenReturn(new MethodMetaData[]{});

        when(this.resourceMetaData.getStatistics())
                .thenReturn(this.resourceModelStatistics);

        this.model = new TestModel();
        this.modelType = TestModel.class;
    }

    @Test
    public void testFieldMappingWithModelSource() {
        mapResourceToModel();
        assertModelReturnedFromMapperIsOriginalModel();
    }

    @Test
    public void testPostProcessingWithoutChangedModel() {
        withPostProcessor(mock(ResourceModelPostProcessor.class));
        mapResourceToModel();
        verifyPostProcessorIsInvokedBeforeAndAfterMapping();
    }

    @Test
    public void testModelChangeInPreProcessing() {
        withPostProcessor(mock(ResourceModelPostProcessor.class));
        withModelReturnedFromPreProcessing(new TestModel());
        mapResourceToModel();
        assertModelReturnedFromMapperWasProvidedByPostProcessor();
    }

    @Test
    public void testModelChangeInPostProcessing() {
        withPostProcessor(mock(ResourceModelPostProcessor.class));
        withModelReturnedFromPostProcessing(new TestModel());
        mapResourceToModel();
        assertModelReturnedFromMapperWasProvidedByPostProcessor();
    }

    @Test
    public void testNoModelChangeInPostProcessing() {
        withPostProcessor(mock(ResourceModelPostProcessor.class));
        withModelReturnedFromPostProcessing(null);
        mapResourceToModel();
        assertModelReturnedFromMapperIsOriginalModel();
    }

    @Test
    public void testNoModelChangeInPreProcessing() {
        withPostProcessor(mock(ResourceModelPostProcessor.class));
        withModelReturnedFromPreProcessing(null);
        mapResourceToModel();
        assertModelReturnedFromMapperIsOriginalModel();
    }

    @Test
    public void testMappingPostProcessorIsInvokedOnDirectAdaptation() {
        mapResourceToModel();
        verify(this.modelProcessor).processBeforeMapping(isA(ResourceModelMetaData.class), eq(this.model));
        verify(this.modelProcessor).processAfterMapping(isA(ResourceModelMetaData.class), eq(this.model));
    }

    @Test
    public void testMappingPostProcessorIsInvokedOnIndirectAdaptation() {
        mapResourceToModel();
        verify(this.modelProcessor).processBeforeMapping(isA(ResourceModelMetaData.class), eq(this.model));
        verify(this.modelProcessor).processAfterMapping(isA(ResourceModelMetaData.class), eq(this.model));
    }

    @Test(expected = CycleInModelInitializationException.class)
    public void testHandlingOfCyclesDuringModelInitialization() {
        withCycleCheckerReportingCycle();
        mapResourceToModel();
    }

    @Test
    public void testHandlingOfCyclesDuringMappingPhase() {
        withCycleCheckerReportingCycle();
        withMappedModelReturnedFromMapping();
        mapResourceToModel();
        verifyCyclecheckIsNotEnded();
        assertModelReturnedFromMapperIsOriginalModel();
    }

    @Test
    public void testHandlingOfAdvisedModelModel() {
        withAopSupportServiceReturningOriginalModel();
        mapResourceToModel();
        verifyMapperObtainsModelFromAopSupport();
        assertModelReturnedFromMapperIsOriginalModel();
    }

    @Test
    public void testRemovalOfAopSupportAtRuntime() {
        withAopSupportServiceReturningOriginalModel();
        unbindAopSupport();
        mapResourceToModel();
        verifyAopSupportIsNeverCalled();
        assertModelReturnedFromMapperIsOriginalModel();
    }

    @Test
    public void testResourceModelInstantiationIsCountedIfMappingIsNotOngoing() {
        mapResourceToModel();
        verifyModelInstantiationIsCounted();
    }

    @Test
    public void testResourceModelInstantiationIsNotCountedIfMappingIsOngoing() {
        withAlreadyOngoingMapping();
        mapResourceToModel();
        verifyModelInstantiationIsNotCounted();
    }

    @Test
    public void testResourceModelMappingDurationIsCountedIfResourceModelIsNotAlreadyMapped() {
        mapResourceToModel();
        verifyMappingDurationIsTracked();
    }

    @Test
    public void testResourceModelMappingDurationIsNotCountedIfResourceModelIsAlreadyMapped() {
        withOngoingMappingForSameResourceModel();
        mapResourceToModel();
        verifyMappingDurationIsNotTracked();
    }

    @Test
    public void testRemovalOfNullPostProcessorDoesNotCauseException() {
        this.testee.unbindProcessor(null);
    }

    private void withOngoingMappingForSameResourceModel() {
        doReturn(true).when(this.nestedMappingSupport).hasOngoingMapping(this.resourceMetaData);
    }

    private void verifyMappingDurationIsTracked() {
        verify(this.resourceModelStatistics).countMappingDuration(anyInt());
    }

    private void verifyMappingDurationIsNotTracked() {
        verify(this.resourceModelStatistics, never()).countMappingDuration(anyInt());
    }

    @SuppressWarnings("unchecked")
    private void withAlreadyOngoingMapping() {
        doReturn(this.ongoingMapping).when(this.nestedMappingSupport).begin(isA(Mapping.class));
        doReturn(this.model).when(this.ongoingMapping).getMappedModel();
    }

    private void verifyModelInstantiationIsCounted() {
        verify(this.resourceModelStatistics).countInstantiation();
    }

    private void verifyModelInstantiationIsNotCounted() {
        verify(this.resourceModelStatistics, never()).countInstantiation();
    }

    private void verifyMapperObtainsModelFromAopSupport() {
        verify(this.aopSupport).prepareForFieldInjection(this.model);
    }

    private void verifyAopSupportIsNeverCalled() {
        verify(this.aopSupport, never()).prepareForFieldInjection(any());
    }

    private void unbindAopSupport() {
        this.testee.unbindAopSupport(this.aopSupport);
    }
    
    private void withAopSupportServiceReturningOriginalModel() {
        this.model = new AopTestModel();
        this.aopSupport = mock(AopSupport.class);
        this.testee.bindAopSupport(this.aopSupport);
        doReturn(new TestModel()).when(this.aopSupport).prepareForFieldInjection(this.model);
    }

    private void verifyCyclecheckIsNotEnded() {
        verify(this.nestedMappingSupport, never()).end(isA(Mapping.class));
    }

    @SuppressWarnings("unchecked")
    private void withCycleCheckerReportingCycle() {
        when(this.nestedMappingSupport.begin(isA(Mapping.class))).thenReturn(this.ongoingMapping);
    }

    private void withMappedModelReturnedFromMapping() {
        doReturn(this.model).when(this.ongoingMapping).getMappedModel();
    }

    private void withModelReturnedFromPostProcessing(TestModel model) {
        this.modelReturnedFromPostProcessor = model;
        when(this.postProcessor.processAfterMapping(eq(this.model), eq(this.resource), eq(this.factory)))
                .thenReturn(this.modelReturnedFromPostProcessor);
    }

    private void assertModelReturnedFromMapperWasProvidedByPostProcessor() {
        assertThat(this.mappedModel).isEqualTo(this.modelReturnedFromPostProcessor);
    }

    private void assertModelReturnedFromMapperIsOriginalModel() {
        assertThat(this.mappedModel).isEqualTo(this.model);
    }

    private void withModelReturnedFromPreProcessing(TestModel model) {
        this.modelReturnedFromPostProcessor = model;
        when(this.postProcessor.processBeforeMapping(eq(this.model), eq(this.resource), eq(this.factory)))
                .thenReturn(this.modelReturnedFromPostProcessor);
    }

    private void verifyPostProcessorIsInvokedBeforeAndAfterMapping() {
        InOrder inOrder = inOrder(this.postProcessor, this.postProcessor);
        inOrder.verify(this.postProcessor, times(1))
                .processBeforeMapping(anyObject(), eq(this.resource), eq(this.factory));
        inOrder.verify(this.postProcessor, times(1))
                .processAfterMapping(anyObject(), eq(this.resource), eq(this.factory));
    }

    private void withPostProcessor(ResourceModelPostProcessor mock) {
        this.postProcessor = mock;
        this.testee.bindProcessor(this.postProcessor);
    }

    @SuppressWarnings("unchecked")
    private void mapResourceToModel() {
        OsgiModelSource<TestModel> source = mock(OsgiModelSource.class);
        when(source.getModel()).thenReturn(this.model);
        when(source.getFactory()).thenReturn(this.factory);
        doReturn(this.modelType).when(source).getModelType();
        this.mappedModel = this.testee.map(this.resource, source);
    }
}
