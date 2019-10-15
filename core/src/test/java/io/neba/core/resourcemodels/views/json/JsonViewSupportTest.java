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
package io.neba.core.resourcemodels.views.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.neba.api.resourcemodels.Lazy;
import io.neba.core.resourcemodels.mapping.Mapping;
import org.apache.sling.api.resource.Resource;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@RunWith(MockitoJUnitRunner.class)
public class JsonViewSupportTest {
    private Map<Object, Mapping<?>> mappings;
    @Mock
    private JsonViewSupport.Configuration configuration;

    private Writer writer;
    private String json;
    private TestModel testModel;

    private JsonViewSupport testee;

    @Before
    public void setUp() {
        this.testModel = new TestModel();
        this.testee = new JsonViewSupport(this::getMappings, configuration);
        this.writer = new StringWriter();
    }

    @Test
    public void testSerializationWithLazyLoadingAndResource() throws IOException {
        serialize();
        assertJsonIs(
                "{" +
                        "\"emptyLazyReference\":null," +
                        "\"nonEmptyLazyReference\":{" +
                        "\"title\":\"referenced Model" +
                        "\"}," +
                        "\"nullResource\":null," +
                        "\"resource\":\"/resource/path\"," +
                        "\"title\":\"title text" +
                        "\"}");
    }

    @Test
    public void testTypeAttributeIsGeneratedWhenModelIsRecordedAndTypeAttributeGenerationIsEnabled() throws IOException {
        withRecordedMapping();
        withTypeAttributeGenerationEnabled();
        serialize();
        assertJsonIs("{" +
                "\":type\":\"some/resource/type\"," +
                "\"emptyLazyReference\":null," +
                "\"nonEmptyLazyReference\":{" +
                "\"title\":\"referenced Model" +
                "\"}," +
                "\"nullResource\":null," +
                "\"resource\":\"/resource/path\"," +
                "\"title\":\"title text" +
                "\"}");
    }

    @Test
    public void testGeneratedTypeAttributeDoesNotOverrideExistingTypeAttribute() throws IOException {
        withModelWithExistingTypeAttribute();
        withRecordedMapping();
        withTypeAttributeGenerationEnabled();
        serialize();
        assertJsonIs("{" +
                "\":type\":\"custom/resource/type\"," +
                "\"emptyLazyReference\":null," +
                "\"nonEmptyLazyReference\":{" +
                "\"title\":\"referenced Model" +
                "\"}," +
                "\"nullResource\":null," +
                "\"resource\":\"/resource/path\"," +
                "\"title\":\"title text" +
                "\"}");
    }

    private void withModelWithExistingTypeAttribute() {
        this.testModel = new TestModelWithExistingTypeAttribute();
    }

    private void withRecordedMapping() {
        this.mappings = new HashMap<>();
        Mapping<?> mapping = mock(Mapping.class);
        doReturn("some/resource/type").when(mapping).getResourceType();
        this.mappings.put(this.testModel, mapping);
    }

    private void serialize() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(this.testee);
        // We need a reliable order for assertions.
        mapper.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);
        JsonFactory jsonFactory = new JsonFactory();
        jsonFactory
                .createGenerator(this.writer)
                .setCodec(mapper)
                .writeObject(this.testModel);
        this.json = this.writer.toString();
    }

    private void withTypeAttributeGenerationEnabled() {
        doReturn(true).when(this.configuration).addTypeAttribute();
    }

    private void assertJsonIs(String expected) {
        assertThat(this.json).isEqualTo(expected);
    }

    private Map<Object, Mapping<?>> getMappings() {
        return this.mappings;
    }

    @SuppressWarnings("unused")
    private static class TestModel {
        private Lazy<ReferencedTestModel> emptyLazyReference = new Lazy<ReferencedTestModel>() {
            @Nonnull
            @Override
            public Optional<ReferencedTestModel> asOptional() {
                return Optional.empty();
            }
        };
        private Lazy<ReferencedTestModel> nonEmptyLazyReference = new Lazy<ReferencedTestModel>() {
            @Nonnull
            @Override
            public Optional<ReferencedTestModel> asOptional() {
                return Optional.of(new ReferencedTestModel());
            }
        };

        private Resource resource = Mockito.mock(Resource.class);

        TestModel() {
            doReturn("/resource/path").when(this.resource).getPath();
        }

        public String getTitle() {
            return "title text";
        }

        public Lazy<ReferencedTestModel> getEmptyLazyReference() {
            return emptyLazyReference;
        }

        public Lazy<ReferencedTestModel> getNonEmptyLazyReference() {
            return nonEmptyLazyReference;
        }

        public Resource getResource() {
            return resource;
        }

        public Resource getNullResource() {
            return null;
        }
    }

    @SuppressWarnings("unused")
    private static class ReferencedTestModel {

        public String getTitle() {
            return "referenced Model";
        }
    }

    @SuppressWarnings("unused")
    private static class TestModelWithExistingTypeAttribute extends TestModel {
        @JsonProperty(":type")
        public String getType() {
            return "custom/resource/type";
        }
    }
}
