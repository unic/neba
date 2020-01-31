package io.neba.spring.resourcemodels.registration;

import io.neba.api.spi.ResourceModelFactory.ContentToModelMappingCallback;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class SpringResourceModelFactoryTest {
    private static final String BEAN_NAME = "beanName";

    @Mock
    private ConfigurableListableBeanFactory factory;
    @Mock
    private SpringBasedModelDefinition modelDefinition;
    @Mock
    private ContentToModelMappingCallback<Object> callback;

    private Object model = new Object();
    private Object modelReturnedFromCallback = new Object();
    private Object providedModel;

    private SpringResourceModelFactory testee;

    @Before
    public void setUp() {
        doReturn(this.model).when(this.factory).getBean(anyString(), eq(getClass()));

        doReturn(getClass()).when(this.modelDefinition).getType();
        doReturn(BEAN_NAME).when(this.modelDefinition).getBeanName();

        doReturn(this.modelReturnedFromCallback).when(this.callback).map(any());

        this.testee = new SpringResourceModelFactory(singletonList(this.modelDefinition), this.factory);
    }

    @Test
    public void testResourceModelRetrieval() {
        assertResourceModelFactoryProvidesAllModelDefinitions();
    }

    @Test
    public void testModelRetrievalFromBeanFactoryIsUsingOriginalBeanNameAndModelTypeAndInvokesCallback() {
        provideModel();
        verifyModelBeanIsObtainedFromBeanFactory();
        assertResourceModelFactoryProvidesModelReturnedFromCallback();
    }

    private void verifyModelBeanIsObtainedFromBeanFactory() {
        verify(this.factory).getBean(BEAN_NAME, getClass());
    }

    private void assertResourceModelFactoryProvidesModelReturnedFromCallback() {
        assertThat(this.providedModel).isSameAs(this.modelReturnedFromCallback);
    }

    private void provideModel() {
        this.providedModel = this.testee.provideModel(this.modelDefinition, this.callback);
    }

    private void assertResourceModelFactoryProvidesAllModelDefinitions() {
        assertThat(this.testee.getModelDefinitions()).containsExactly(this.modelDefinition);
    }
}
