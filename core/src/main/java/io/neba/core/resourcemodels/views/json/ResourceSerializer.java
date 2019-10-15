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
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.apache.sling.api.resource.Resource;

import javax.annotation.Nonnull;
import java.io.IOException;

/**
 * Serializes Sling {@link Resource resources} in the most straightforward manner available: By providing the
 * {@link Resource#getPath() resource path}.
 */
public class ResourceSerializer extends StdSerializer<Resource> {

    ResourceSerializer() {
        super(Resource.class);
    }

    @Override
    public void serialize(@Nonnull Resource value, @Nonnull JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeString(value.getPath());
    }
}
