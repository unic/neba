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
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.BeanSerializer;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.fasterxml.jackson.databind.ser.impl.BeanAsArraySerializer;
import com.fasterxml.jackson.databind.ser.impl.ObjectIdWriter;
import com.fasterxml.jackson.databind.ser.std.BeanSerializerBase;
import io.neba.api.resourcemodels.Lazy;
import io.neba.core.resourcemodels.mapping.Mapping;
import io.neba.core.resourcemodels.mapping.NestedMappingSupport;
import io.neba.core.util.ResolvedModel;
import org.apache.sling.api.resource.Resource;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Explicitly supports rendering NEBA models as JSON views by augmenting the rendered JSON with metadata
 * {@link NestedMappingSupport#beginRecordingMappings() recorded} during
 * {@link io.neba.core.resourcemodels.mapping.ResourceToModelMapper#map(Resource, ResolvedModel) resource to model mapping}.
 * In addition, registers custom {@link JsonSerializer JSON serializers} to support serialization of Sling-specific models
 * such as {@link Resource}.
 */
class JsonViewSupport extends SimpleModule {
    private final Supplier<Map<Object, Mapping<?>>> mappings;

    JsonViewSupport(@Nonnull Supplier<Map<Object, Mapping<?>>> mappings) {
        this.mappings = mappings;
        addSerializer(Lazy.class, new LazyLoadingSerializer());
        addSerializer(Resource.class, new ResourceSerializer());
    }

    @Override
    public void setupModule(@Nonnull SetupContext context) {
        super.setupModule(context);
        context.addBeanSerializerModifier(new BeanSerializerModifier() {
            @Override
            public JsonSerializer<?> modifySerializer(SerializationConfig config, BeanDescription beanDesc, JsonSerializer<?> serializer) {
                if (serializer instanceof BeanSerializer) {
                    return new ResourceModelSerializer(mappings, (BeanSerializer) serializer);
                }
                return serializer;
            }
        });
    }

    private static class ResourceModelSerializer extends BeanSerializerBase {
        private final Supplier<Map<Object, Mapping<?>>> mappings;

        ResourceModelSerializer(Supplier<Map<Object, Mapping<?>>> mappings, BeanSerializer bs) {
            super(bs);
            this.mappings = mappings;
        }

        ResourceModelSerializer(BeanSerializerBase src, ObjectIdWriter objectIdWriter, Object filterId, Supplier<Map<Object, Mapping<?>>> mappings) {
            super(src, objectIdWriter, filterId);
            this.mappings = mappings;
        }

        ResourceModelSerializer(BeanSerializerBase src, Set<String> toIgnore, Supplier<Map<Object, Mapping<?>>> mappings) {
            super(src, toIgnore);
            this.mappings = mappings;
        }

        @Override
        public ResourceModelSerializer withObjectIdWriter(ObjectIdWriter objectIdWriter) {
            return new ResourceModelSerializer(this, objectIdWriter, _propertyFilterId, mappings);
        }

        @Override
        protected ResourceModelSerializer withIgnorals(Set<String> toIgnore) {
            return new ResourceModelSerializer(this, toIgnore, mappings);
        }

        @Override
        protected BeanSerializerBase asArraySerializer() {
            /*
              See {@link BeanSerializer#asArraySerializer()}
             */
            if ((_objectIdWriter == null)
                    && (_anyGetterWriter == null)
                    && (_propertyFilterId == null)) {
                return new BeanAsArraySerializer(this);
            }
            // already is one, so:
            return this;
        }

        @Override
        public BeanSerializerBase withFilterId(Object filterId) {
            return new ResourceModelSerializer(this, _objectIdWriter, filterId, mappings);
        }

        @Override
        public void serialize(@Nonnull Object bean, @Nonnull JsonGenerator gen, @Nonnull SerializerProvider provider) throws IOException {
            gen.writeStartObject(bean);
            Mapping<?> mapping = mappings.get().get(bean);
            if (mapping != null) {
                gen.writeStringField(":type", mapping.getResourceType());
            }
            if (_objectIdWriter != null) {
                gen.setCurrentValue(bean); // [databind#631]
                _serializeWithObjectId(bean, gen, provider, false);
            } else if (_propertyFilterId != null) {
                serializeFieldsFiltered(bean, gen, provider);
            } else {
                serializeFields(bean, gen, provider);
            }
            gen.writeEndObject();
        }
    }
}
