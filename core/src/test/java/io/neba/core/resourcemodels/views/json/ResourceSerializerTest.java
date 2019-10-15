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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.apache.sling.api.resource.Resource;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class ResourceSerializerTest {
    @Mock
    private Resource resource;
    @Mock
    private SerializerProvider provider;
    @Mock
    private JsonGenerator generator;

    private ResourceSerializer testee;

    @Before
    public void setUp() {
        this.testee = new ResourceSerializer();
    }

    @Test
    public void testSerializerWritesResourcePath() throws IOException {
        withResourcePath("/some/resource/path");
        serializeResource();

        verifySerializerWritesResourcePath("/some/resource/path");
    }

    /**
     * If configured, serializers may receive null values to serialize.
     */
    @Test
    public void testSerializerToleratesNullValues() throws IOException {
        withNullResource();
        serializeResource();

        verifySerializerWritesResourcePath(null);
    }

    private void withNullResource() {
        this.resource = null;
    }

    private void verifySerializerWritesResourcePath(String path) throws IOException {
        verify(this.generator).writeString(path);
    }

    private void serializeResource() throws IOException {
        this.testee.serialize(this.resource, this.generator, this.provider);
    }

    private void withResourcePath(String path) {
        doReturn(path).when(this.resource).getPath();
    }
}
