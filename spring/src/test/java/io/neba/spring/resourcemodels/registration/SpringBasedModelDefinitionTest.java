package io.neba.spring.resourcemodels.registration;

import io.neba.api.annotations.ResourceModel;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

@RunWith(MockitoJUnitRunner.class)
public class SpringBasedModelDefinitionTest {
    @Mock
    private ResourceModel resourceModelAnnotation;

    private SpringBasedModelDefinition testee;

    @Before
    public void setUp() {
        doReturn("").when(this.resourceModelAnnotation).name();
        this.testee = new SpringBasedModelDefinition(this.resourceModelAnnotation, "beanName", getClass());
    }

    @Test
    public void testModelNameIsBeanNameByDefault() {
        assertModelNameIs("beanName");
    }

    @Test
    public void testExplicitModelNameOverridesBeanName() {
        withExplicitModelName("explicitModelName");
        assertModelNameIs("explicitModelName");
    }

    @Test
    public void testModelTypeRetrieval() {
        assertModelTypeIsReturnedAsIs();
    }

    @Test
    public void testResourceModelAnnotationRetrieval() {
        assertResourceModelAnnotationIsReturnedAsIs();
    }

    private void assertResourceModelAnnotationIsReturnedAsIs() {
        assertThat(this.testee.getResourceModel()).isSameAs(this.resourceModelAnnotation);
    }

    private void assertModelTypeIsReturnedAsIs() {
        assertThat(this.testee.getType()).isSameAs(getClass());
    }

    private void withExplicitModelName(String name) {
        doReturn(name).when(this.resourceModelAnnotation).name();
    }

    private void assertModelNameIs(String name) {
        assertThat(this.testee.getName()).isEqualTo(name);
    }
}
