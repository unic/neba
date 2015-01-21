/**
 * Copyright 2013 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 the "License";
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
**/

package io.neba.core.resourcemodels.mapping;

import io.neba.api.resourcemodels.ResourceModelPostProcessor;
import io.neba.core.resourcemodels.metadata.MappedFieldMetaData;
import io.neba.core.resourcemodels.metadata.MethodMetaData;
import io.neba.core.resourcemodels.metadata.ResourceModelMetaData;
import io.neba.core.resourcemodels.metadata.ResourceModelMetaDataRegistrar;
import io.neba.core.resourcemodels.metadata.ResourceModelStatistics;
import io.neba.core.util.OsgiBeanSource;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.Advised;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.NoOp;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Olaf Otto
 */
@RunWith(MockitoJUnitRunner.class)
public class ResourceToModelMapperTest {
    /**
     * @author Christoph Huber
     * @author Olaf Otto
     */
    public static class TestModel {
    }

    @Mock
    private Resource resource;
    @Mock
    private ValueMap valueMap;
    @Mock
    private BeanFactory factory;
    @Mock
    private ModelProcessor modelProcessor;
    @Mock
    private ResourceModelMetaDataRegistrar resourceModelMetaDataRegistrar;
    @Mock
    private CyclicMappingSupport cyclicMappingSupport;
    @Mock
    private ResourceModelMetaData resourceMetaData;
    @Mock
    private ResourceModelStatistics resourceModelStatistics;
    @Mock
    private CustomFieldMappers customFieldMappers;
    @Mock
    private Mapping<Object> ongoingMapping;

    private TestModel model;
    private Class<?> beanType;
    private ResourceModelPostProcessor postProcessor;
    private TestModel modelReturnedFromPostProcessor;
    private TestModel mappedModel;
    private TargetSource targetSource;

    @InjectMocks
    private ResourceToModelMapper testee;

    @Before
    @SuppressWarnings("unchecked")
    public void prepareMapper() {
        when(this.resource.adaptTo(eq(ValueMap.class))).thenReturn(this.valueMap);
        when(this.resourceModelMetaDataRegistrar.get(isA(Class.class))).thenReturn(this.resourceMetaData);
        when(this.cyclicMappingSupport.begin(isA(Mapping.class))).thenReturn(null);
        when(this.resourceMetaData.getMappableFields()).thenReturn(new MappedFieldMetaData[]{});
        when(this.resourceMetaData.getPostMappingMethods()).thenReturn(new MethodMetaData[]{});
        when(this.resourceMetaData.getPreMappingMethods()).thenReturn(new MethodMetaData[]{});
        when(this.resourceMetaData.getStatistics()).thenReturn(this.resourceModelStatistics);
        this.model = new TestModel();
        this.beanType = TestModel.class;
    }

    @Test
    public void testFieldMappingWithBeanSource() throws Exception {
        mapResourceToModel();
        assertModelReturnedFromMapperIsOriginalModel();
    }

    @Test
    public void testPostProcessingWithoutChangedModel() throws Exception {
        withPostProcessor(mock(ResourceModelPostProcessor.class));
        mapResourceToModel();
        verifyPostProcessorIsInvokedBeforeAndAfterMapping();
    }

    @Test
    public void testModelChangeInPreProcessing() throws Exception {
        withPostProcessor(mock(ResourceModelPostProcessor.class));
        withModelReturnedFromPreProcessing(new TestModel());
        mapResourceToModel();
        assertModelReturnedFromMapperWasProvidedByPostProcessor();
    }

    @Test
    public void testModelChangeInPostProcessing() throws Exception {
        withPostProcessor(mock(ResourceModelPostProcessor.class));
        withModelReturnedFromPostProcessing(new TestModel());
        mapResourceToModel();
        assertModelReturnedFromMapperWasProvidedByPostProcessor();
    }

    @Test
    public void testNoModelChangeInPostProcessing() throws Exception {
        withPostProcessor(mock(ResourceModelPostProcessor.class));
        withModelReturnedFromPostProcessing(null);
        mapResourceToModel();
        assertModelReturnedFromMapperIsOriginalModel();
    }

    @Test
    public void testNoModelChangeInPreProcessing() throws Exception {
        withPostProcessor(mock(ResourceModelPostProcessor.class));
        withModelReturnedFromPreProcessing(null);
        mapResourceToModel();
        assertModelReturnedFromMapperIsOriginalModel();
    }

    @Test
    public void testMappingPostProcessorIsInvokedOnDirectAdaptation() throws Exception {
        mapResourceToModel();
        verify(this.modelProcessor).processBeforeMapping(isA(ResourceModelMetaData.class), eq(this.model));
        verify(this.modelProcessor).processAfterMapping(isA(ResourceModelMetaData.class), eq(this.model));
    }

    @Test
    public void testMappingPostProcessorIsInvokedOnIndirectAdaptation() throws Exception {
        mapResourceToModel();
        verify(this.modelProcessor).processBeforeMapping(isA(ResourceModelMetaData.class), eq(this.model));
        verify(this.modelProcessor).processAfterMapping(isA(ResourceModelMetaData.class), eq(this.model));
    }

    @Test(expected = CycleInBeanInitializationException.class)
    public void testHandlingOfCyclesDuringBeanInitialization() throws Exception {
        withCycleCheckerReportingCycle();
        mapResourceToModel();
    }

    @Test
    public void testHandlingOfCyclesDuringMappingPhase() throws Exception {
        withCycleCheckerReportingCycle();
        withMappedModelReturnedFromMapping();
        mapResourceToModel();
        verifyCyclecheckIsNotEnded();
        assertModelReturnedFromMapperIsOriginalModel();
    }

    @Test
    public void testHandlingOfAdvisedModelBean() throws Exception {
        withSpringAopProxyFor(TestModel.class);
        mapResourceToModel();
        verifyMapperObtainsOriginalBeanFromAdvisedProxy();
        assertModelReturnedFromMapperIsOriginalModel();
    }

    private void verifyMapperObtainsOriginalBeanFromAdvisedProxy() throws Exception {
        verify(this.targetSource).getTarget();
    }

    private void withSpringAopProxyFor(Class<TestModel> superclass) throws Exception {
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(superclass);
        enhancer.setCallback(NoOp.INSTANCE);
        enhancer.setInterfaces(new Class[]{Advised.class});
        Object enhanced = spy(enhancer.create());
        assertThat(enhanced).isNotNull();

        this.targetSource = mock(TargetSource.class);
        TestModel unwrappedBeanInstance = new TestModel();
        doReturn(this.targetSource).when((Advised) enhanced).getTargetSource();
        doReturn(unwrappedBeanInstance).when(this.targetSource).getTarget();

        this.model = (TestModel) enhanced;
    }

    private void verifyCyclecheckIsNotEnded() {
        verify(this.cyclicMappingSupport, never()).end(isA(Mapping.class));
    }

    @SuppressWarnings("unchecked")
    private void withCycleCheckerReportingCycle() {
        when(this.cyclicMappingSupport.begin(isA(Mapping.class))).thenReturn(this.ongoingMapping);
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
        this.testee.add(this.postProcessor);
    }

    @SuppressWarnings("unchecked")
    private void mapResourceToModel() {
        OsgiBeanSource<TestModel> source = mock(OsgiBeanSource.class);
        when(source.getBean()).thenReturn(this.model);
        when(source.getFactory()).thenReturn(this.factory);
        doReturn(this.beanType).when(source).getBeanType();
        this.mappedModel = this.testee.map(this.resource, source);
    }
}
