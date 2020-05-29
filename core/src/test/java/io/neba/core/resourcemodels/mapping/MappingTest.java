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

package io.neba.core.resourcemodels.mapping;

import io.neba.core.resourcemodels.metadata.ResourceModelMetaData;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * @author Olaf Otto
 */
@RunWith(MockitoJUnitRunner.class)
public class MappingTest {
    @Mock
    private ResourceModelMetaData metaData;

    private String source;
    private String mappingAsString;
    private Object model = new Object();

    private Mapping<Object> testee;

    @Before
    public void prepareMapping() {
        this.source = "/src/path";
        this.testee = new Mapping<>(this.source, this.metaData, "resource/type");

        doReturn("junit.test.Type")
                .when(this.metaData)
                .getTypeName();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMappingConstructorRequiresNonNullResourcePath() {
        new Mapping<>(null, this.metaData, "resource/type");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMappingConstructorRequiresNonNullMetaData() {
        new Mapping<>("/some/resource/path", null, "resource/type");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testMappingConstructorRequiresNonNullResourceType() {
        new Mapping<>("/some/resource/path", this.metaData, null);
    }

    @Test
    public void testHashCodeAndEquals() {
        Mapping<?> secondMapping = new Mapping<>(this.source, this.metaData, "resource/type");
        assertThat(this.testee.hashCode()).isEqualTo(secondMapping.hashCode());
        assertThat(this.testee).isEqualTo(secondMapping);

        secondMapping = new Mapping<>("/other/source", this.metaData, "resource/type");
        assertThat(this.testee.hashCode()).isNotEqualTo(secondMapping.hashCode());
        assertThat(this.testee).isNotEqualTo(secondMapping);

        secondMapping = new Mapping<>(this.source, mock(ResourceModelMetaData.class), "resource/type");
        assertThat(this.testee.hashCode()).isNotEqualTo(secondMapping.hashCode());
        assertThat(this.testee).isNotEqualTo(secondMapping);
    }

    @Test
    public void testStringRepresentation() {
        mappingToString();
        assertMappingAsStringIs("Mapping [/src/path -> " + this.metaData.getTypeName() + "]");
    }

    @Test
    public void testModelTransportation() {
        setModel();
        assertGetterReturnsOriginalModel();
    }

    @Test
    public void testResourceTypeGetter() {
        assertThat(this.testee.getResourceType()).isEqualTo("resource/type");
    }

    @Test
    public void testMetadataGetter() {
        assertThat(this.testee.getMetadata()).isEqualTo(this.metaData);
    }

    private void assertGetterReturnsOriginalModel() {
        assertThat(this.testee.getMappedModel()).isSameAs(this.model);
    }

    private void setModel() {
        this.testee.setMappedModel(this.model);
    }

    private void assertMappingAsStringIs(final String expected) {
        assertThat(this.mappingAsString).isEqualTo(expected);
    }

    private void mappingToString() {
        this.mappingAsString = this.testee.toString();
    }
}
