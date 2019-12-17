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

import io.neba.api.spi.AnnotatedFieldMapper;
import io.neba.core.resourcemodels.metadata.MappedFieldMetaData;
import io.neba.core.util.ConcurrentDistinctMultiValueMap;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.apache.commons.lang3.ClassUtils.primitiveToWrapper;
import static org.osgi.service.component.annotations.ReferenceCardinality.MULTIPLE;
import static org.osgi.service.component.annotations.ReferencePolicy.DYNAMIC;

/**
 * Represents all registered {@link AnnotatedFieldMapper custom field mappers}
 * and provides the corresponding lookup and caching of lookup results.
 *
 * @author Olaf Otto
 */
@Component(service = AnnotatedFieldMappers.class)
public class AnnotatedFieldMappers {
    /**
     * Represents the relation of an {@link java.lang.annotation.Annotation} and and a
     * {@link AnnotatedFieldMapper#getAnnotationType() compatible mapper}.
     *
     * @author Olaf Otto
     */
    public static class AnnotationMapping {
        private final Annotation annotation;
        private final AnnotatedFieldMapper<?, ?> mapper;

        AnnotationMapping(Annotation annotation, AnnotatedFieldMapper<?, ?> mapper) {
            this.annotation = annotation;
            this.mapper = mapper;
        }

        public Annotation getAnnotation() {
            return annotation;
        }

        AnnotatedFieldMapper<?, ?> getMapper() {
            return mapper;
        }
    }

    private final ConcurrentDistinctMultiValueMap<Field, AnnotationMapping> cache = new ConcurrentDistinctMultiValueMap<>();
    private final ConcurrentDistinctMultiValueMap<Class<? extends Annotation>, AnnotatedFieldMapper<?, ?>> fieldMappers = new ConcurrentDistinctMultiValueMap<>();

    @Reference(cardinality = MULTIPLE, policy = DYNAMIC, unbind = "unbind")
    protected synchronized void bind(AnnotatedFieldMapper<?, ?> mapper) {
        if (mapper == null) {
            throw new IllegalArgumentException("Method argument mapper must not be null.");
        }
        this.fieldMappers.put(mapper.getAnnotationType(), mapper);
        this.cache.clear();
    }

    /**
     * @param mapper must not be <code>null</code>.
     */
    protected synchronized void unbind(AnnotatedFieldMapper<?, ?> mapper) {
        if (mapper == null) {
            return;
        }
        this.fieldMappers.removeValue(mapper);
        this.cache.clear();
    }

    /**
     * @param metaData must not be <code>null</code>.
     * @return never <code>null</code> but rather an empty collection.
     */
    public Collection<AnnotationMapping> get(MappedFieldMetaData metaData) {
        if (metaData == null) {
            throw new IllegalArgumentException("Method argument metaData must not be null.");
        }

        Collection<AnnotationMapping> mappers = this.cache.get(metaData.getField());

        if (mappers != null) {
            return mappers;
        }

        return this.cache.computeIfAbsent(metaData.getField(), key -> resolveCompatibleMappers(metaData));
    }

    private Collection<AnnotationMapping> resolveCompatibleMappers(MappedFieldMetaData metaData) {
        List<AnnotationMapping> compatibleMappers = new ArrayList<>();
        for (Annotation annotation : metaData.getAnnotations()) {
            Collection<AnnotatedFieldMapper<?, ?>> mappersForAnnotation = this.fieldMappers.get(annotation.annotationType());
            if (mappersForAnnotation == null) {
                continue; // with next element
            }
            for (AnnotatedFieldMapper<?, ?> mapper : mappersForAnnotation) {
                // Mappers supporting boxed types must also support the primitive equivalent,
                // e.g. Boolean / boolean, Integer / int.
                Class<?> type = primitiveToWrapper(metaData.getType());
                if (mapper.getFieldType().isAssignableFrom(type)) {
                    compatibleMappers.add(new AnnotationMapping(annotation, mapper));
                }
            }
        }
        return compatibleMappers;
    }
}
