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
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializer;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.fasterxml.jackson.databind.ser.impl.BeanAsArraySerializer;
import com.fasterxml.jackson.databind.ser.impl.ObjectIdWriter;
import com.fasterxml.jackson.databind.ser.std.BeanSerializerBase;
import io.neba.api.resourcemodels.Lazy;
import io.neba.core.resourcemodels.mapping.Mapping;
import io.neba.core.resourcemodels.mapping.NestedMappingSupport;
import io.neba.core.util.ResolvedModelSource;
import org.apache.sling.api.resource.Resource;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Explicitly supports rendering NEBA models as JSON views by augmenting the rendered JSON with metadata
 * {@link NestedMappingSupport#beginRecordingMappings() recorded} during
 * {@link io.neba.core.resourcemodels.mapping.ResourceToModelMapper#map(Resource, ResolvedModelSource) resource to model mapping}.
 * In addition, registers custom {@link JsonSerializer JSON serializers} to support serialization of Sling-specific models
 * such as {@link Resource}.
 */
class JsonViewSupport extends SimpleModule {
    private static final long serialVersionUID = -4796305586109570374L;
    private final Supplier<Map<Object, Mapping<?>>> mappings;
    private final Configuration configuration;

    JsonViewSupport(@Nonnull Supplier<Map<Object, Mapping<?>>> mappings, @Nonnull Configuration configuration) {
        this.mappings = mappings;
        this.configuration = configuration;

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
                    return new ResourceModelSerializer(mappings, (BeanSerializer) serializer, configuration);
                }
                return serializer;
            }
        });
    }

    /**
     * An augmented {@link BeanSerializer} that can add contextual data to a serialized bean representation.
     */
    private static class ResourceModelSerializer extends BeanSerializerBase {
        private static final long serialVersionUID = -2810312324356307359L;
        private final Supplier<Map<Object, Mapping<?>>> mappings;
        private final Configuration configuration;

        ResourceModelSerializer(Supplier<Map<Object, Mapping<?>>> mappings, BeanSerializer bs, Configuration configuration) {
            super(bs);
            this.mappings = mappings;
            this.configuration = configuration;
        }

        ResourceModelSerializer(BeanSerializerBase src, ObjectIdWriter objectIdWriter, Object filterId, Supplier<Map<Object, Mapping<?>>> mappings, Configuration configuration) {
            super(src, objectIdWriter, filterId);
            this.mappings = mappings;
            this.configuration = configuration;
        }

        ResourceModelSerializer(BeanSerializerBase src, Set<String> toIgnore, Supplier<Map<Object, Mapping<?>>> mappings, Configuration configuration) {
            super(src, toIgnore);
            this.mappings = mappings;
            this.configuration = configuration;
        }

        @Override
        public ResourceModelSerializer withObjectIdWriter(ObjectIdWriter objectIdWriter) {
            return new ResourceModelSerializer(this, objectIdWriter, _propertyFilterId, mappings, configuration);
        }

        @Override
        protected ResourceModelSerializer withIgnorals(Set<String> toIgnore) {
            return new ResourceModelSerializer(this, toIgnore, mappings, configuration);
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
            return new ResourceModelSerializer(this, _objectIdWriter, filterId, mappings, configuration);
        }

        @Override
        public void serialize(@Nonnull Object bean, @Nonnull JsonGenerator gen, @Nonnull SerializerProvider provider) throws IOException {
            gen.writeStartObject(bean);
            maybeAddTypeAttribute(bean, gen, provider);
            if (_objectIdWriter != null) {
                gen.setCurrentValue(bean);
                _serializeWithObjectId(bean, gen, provider, false);
            } else if (_propertyFilterId != null) {
                serializeFieldsFiltered(bean, gen, provider);
            } else {
                serializeFields(bean, gen, provider);
            }
            gen.writeEndObject();
        }

        private void maybeAddTypeAttribute(@Nonnull Object bean, @Nonnull JsonGenerator gen, @Nonnull SerializerProvider provider) throws IOException {
            if (this.configuration.addTypeAttribute()) {
                Mapping<?> mapping = mappings.get().get(bean);
                if (mapping == null) {
                    return;
                }
                // This is costly, but jackson does not provide a data structure to look up
                // bean attributes in a more efficient way.
                for (BeanPropertyWriter property : getBeanProperties(provider)) {
                    if (property.getName().equals(":type")) {
                        // Do not override user-defined ":type" properties.
                        return;
                    }
                }

                gen.writeStringField(":type", mapping.getResourceType());
            }
        }

        private BeanPropertyWriter[] getBeanProperties(@Nonnull SerializerProvider provider) {
            if (_filteredProps != null && provider.getActiveView() != null) {
                return _filteredProps;
            } else {
                return _props;
            }
        }
    }

    public interface Configuration {
        default boolean addTypeAttribute() {
            return false;
        }
    }
}
