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
import io.neba.api.resourcemodels.Lazy;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.io.IOException;

/**
 * Supports serialization of NEBA's {@link Lazy} fields, thus enabling declaration of lazy fields that are only loaded
 * when a model is serialized to JSON.
 */
public class LazyLoadingSerializer extends StdSerializer<Lazy> {
    private static final long serialVersionUID = -4291272104765126426L;

    LazyLoadingSerializer() {
        super(Lazy.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void serialize(@CheckForNull Lazy value, @Nonnull JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeObject(value == null ? null : value.orElse(null));
    }

    @Override
    public boolean isEmpty(SerializerProvider provider, Lazy value) {
        return value == null || !value.isPresent();
    }
}
