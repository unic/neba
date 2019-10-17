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

import io.neba.api.resourcemodels.Lazy;
import io.neba.core.resourcemodels.mapping.Mapping;
import org.apache.sling.api.resource.Resource;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

@RunWith(MockitoJUnitRunner.class)
public class Jackson2ModelSerializerTest {
    private Map<Object, Mapping<?>> mappings;
    private boolean addTypeAttribute = false;
    private String[] settings;
    private StringWriter out;
    private Object testModel;

    private Jackson2ModelSerializer testee;

    @Before
    public void setUp() {
        withTestModel(new TestModel());
        this.mappings = null;

        // We need a stable property order for assertions.
        withSettings("MapperFeature.SORT_PROPERTIES_ALPHABETICALLY=true");

        initializeModelSerializer();
    }

    @Test
    public void testInvalidSettingsAreTolerated() {
        withSettings("Invalid.Setting=EXPECTED ERROR", "broken", "", "==", "MapperFeature.DOES_NOT_EXIST=true");
        initializeModelSerializer();
    }

    @Test
    public void testInvalidSettingsAreSkippedAndValidSettingsAreApplied() throws IOException {
        withTestModel(new TestModelWithDate());

        withSettings("Invalid.Setting=EXPECTED ERROR", "SerializationFeature.WRITE_DATES_AS_TIMESTAMPS=false");
        initializeModelSerializer();

        serialize();
        assertJsonIs("{\"date\":\"1970-01-01T00:00:00.000+0000\"}");

        withSettings("Invalid.Setting=EXPECTED ERROR", "SerializationFeature.WRITE_DATES_AS_TIMESTAMPS=true");
        initializeModelSerializer();

        serialize();
        assertJsonIs("{\"date\":0}");
    }

    @Test
    public void testSerializationWithoutTypeAttribute() throws IOException {
        serialize();
        assertJsonIs("{\"helloWorld\":\"Hello, world\",\"lazy\":\"Lazy value\",\"resource\":\"/some/resource/path\"}");
    }

    @Test
    public void testTypeGeneration() throws IOException {
        withRecordedMapping();
        withTypeGenerationEnabled();
        initializeModelSerializer();
        serialize();

        assertJsonIs("{\":type\":\"some/resource/type\",\"helloWorld\":\"Hello, world\",\"lazy\":\"Lazy value\",\"resource\":\"/some/resource/path\"}");
    }

    private void withTypeGenerationEnabled() {
        this.addTypeAttribute = true;
    }

    private void withSettings(String... settings) {
        this.settings = settings;
    }

    private void withRecordedMapping() {
        this.mappings = new HashMap<>();
        Mapping<?> mapping = mock(Mapping.class);
        doReturn("some/resource/type").when(mapping).getResourceType();
        this.mappings.put(this.testModel, mapping);
    }

    private void assertJsonIs(String expected) {
        assertThat(this.out.toString()).isEqualTo(expected);
    }

    private void serialize() throws IOException {
        this.out = new StringWriter();
        PrintWriter writer = new PrintWriter(this.out);
        this.testee.serialize(writer, this.testModel);
    }

    private Map<Object, Mapping<?>> getMappings() {
        return mappings;
    }

    private void initializeModelSerializer() {
        this.testee = new Jackson2ModelSerializer(this::getMappings, this.settings, this.addTypeAttribute);
    }

    private void withTestModel(Object model) {
        this.testModel = model;
    }

    /**
     * To ensure all expected features are enabled by the testee, this model contains getters
     * that must be handled by NEBA-specific jackson features, such as {@link ResourceSerializer} or {@link LazyLoadingSerializer}.
     */
    @SuppressWarnings("unused")
    private static class TestModel {
        public String getHelloWorld() {
            return "Hello, world";
        }

        public Resource getResource() {
            Resource resource = mock(Resource.class);
            doReturn("/some/resource/path").when(resource).getPath();
            return resource;
        }

        public Lazy<?> getLazy() {
            return new Lazy<Object>() {
                @Nonnull
                @Override
                public Optional<Object> asOptional() {
                    return Optional.of("Lazy value");
                }
            };
        }
    }

    @SuppressWarnings("unused")
    private static class TestModelWithDate {
        public Date getDate() {
            return new Date(0);
        }
    }
}
