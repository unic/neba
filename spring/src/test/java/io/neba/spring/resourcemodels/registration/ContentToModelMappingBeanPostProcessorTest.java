package io.neba.spring.resourcemodels.registration;

import io.neba.api.spi.ResourceModelFactory.ModelDefinition;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import static io.neba.api.spi.ResourceModelFactory.ContentToModelMappingCallback;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class ContentToModelMappingBeanPostProcessorTest {
    private static final String BEAN_NAME = "beanName";

    @Mock
    private ModelDefinition<?> modelDefinition;
    @Mock
    private ContentToModelMappingCallback<Object> parentCallback;
    @Mock
    private ContentToModelMappingCallback<Object> childCallback;

    private Object model = new Object();
    private Object postProcessedBean;

    private ContentToModelMappingBeanPostProcessor testee;

    @Before
    public void setUp() {
        Answer returnModel = inv -> inv.getArguments()[0];
        doAnswer(returnModel).when(parentCallback).map(any());
        doAnswer(returnModel).when(childCallback).map(any());
        doReturn(BEAN_NAME).when(this.modelDefinition).getName();

        this.testee = new ContentToModelMappingBeanPostProcessor(singletonList(this.modelDefinition));
    }

    @Test
    public void testPostProcessorSupportsNestedResourceModelInstantiation() {
        push(this.parentCallback);
        postProcessBean();
        verifyPostProcessorInvokes(this.parentCallback);

        push(this.childCallback);
        postProcessBean();
        verifyPostProcessorInvokes(this.childCallback);

        pop();
        postProcessBean();
        verifyPostProcessorInvokesAgain(this.parentCallback);

        pop();
        postProcessBean();
        assertNoCallbackWasAvailable();
    }

    @Test
    public void testPostProcessorIgnoresBeansWithUnknownName() {
        push(parentCallback);
        postProcessBeanWithName("unknownName");
        assertPostProcessorSignalsNoChange();
        verifyNoMappingIsDoneUsing(this.parentCallback);
    }

    private void verifyNoMappingIsDoneUsing(ContentToModelMappingCallback<Object> cb) {
        verify(cb, never()).map(any());
    }

    private void postProcessBeanWithName(String beanName) {
        this.postProcessedBean = this.testee.postProcessAfterInitialization(this.model, beanName);
    }

    private void assertPostProcessorSignalsNoChange() {
        assertThat(this.postProcessedBean).isNull();
    }

    private void assertNoCallbackWasAvailable() {
        assertThat(this.postProcessedBean).isNull();
    }

    private void verifyPostProcessorInvokesAgain(ContentToModelMappingCallback<Object> cb) {
        verify(cb, times(2)).map(this.model);
    }

    private void pop() {
        this.testee.pop();
    }

    private <T> void verifyPostProcessorInvokes(ContentToModelMappingCallback<Object> cb) {
        verify(cb).map(this.model);
    }

    private void postProcessBean() {
        this.postProcessedBean = this.testee.postProcessBeforeInitialization(this.model, BEAN_NAME);
    }

    private void push(ContentToModelMappingCallback cb) {
        this.testee.push(cb);
    }
}
