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
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Olaf Otto
 */
public class ClassBasedModelDefinitionTest {
    private ClassBasedModelDefinition testee;

    @Test
    public void testModelDefinition() {
        withClassBasedModelDefinition(Model.class);
        assertThatModelDefinitionProvidesResourceModelAnnotation();
        assertModelDefinitionHasName("model");
        assertModelTypeIs(Model.class);
    }

    @Test
    public void testHashCodeAnEquals() {
        ClassBasedModelDefinition one = new ClassBasedModelDefinition(Model.class);
        ClassBasedModelDefinition sameOne = new ClassBasedModelDefinition(Model.class);
        ClassBasedModelDefinition two = new ClassBasedModelDefinition(OtherModel.class);

        assertThat(one).isEqualTo(sameOne);
        assertThat(sameOne).isEqualTo(one);
        assertThat(one.hashCode()).isEqualTo(sameOne.hashCode());

        assertThat(one).isEqualTo(one);
        assertThat(one).isEqualTo(one);
        assertThat(one.hashCode()).isEqualTo(one.hashCode());

        assertThat(one).isNotEqualTo(two);
        assertThat(two).isNotEqualTo(one);
        assertThat(one.hashCode()).isNotEqualTo(two.hashCode());
    }

    @Test
    public void testModelNameSpecificationInResourceModelAnnotationIsPreferred() {
        withClassBasedModelDefinition(NamedModel.class);
        assertModelDefinitionHasName("userDefinedModelName");
    }

    private void assertModelTypeIs(Class<Model> modelType) {
        assertThat(this.testee.getType()).isSameAs(modelType);
    }

    private void assertModelDefinitionHasName(String name) {
        assertThat(this.testee.getName()).isEqualTo(name);
    }

    private void assertThatModelDefinitionProvidesResourceModelAnnotation() {
        assertThat(this.testee.getResourceModel()).isSameAs(Model.class.getAnnotation(ResourceModel.class));
    }

    private void withClassBasedModelDefinition(Class<?> c) {
        this.testee = new ClassBasedModelDefinition(c);
    }

    @ResourceModel("some/type")
    private static class Model {

    }

    @ResourceModel("some/type")
    private static class OtherModel {

    }

    @ResourceModel(value = "some/type", name = "userDefinedModelName")
    private static class NamedModel {
      
    }
}
