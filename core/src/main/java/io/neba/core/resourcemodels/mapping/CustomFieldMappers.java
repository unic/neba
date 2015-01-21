/**
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 the "License";
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/
package io.neba.core.resourcemodels.mapping;

import io.neba.api.resourcemodels.FieldMapper;
import io.neba.core.resourcemodels.metadata.MappedFieldMetaData;
import io.neba.core.util.ConcurrentDistinctMultiValueMap;
import org.springframework.stereotype.Service;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static java.util.Collections.emptyList;

/**
 * @author Olaf Otto
 */
@Service
public class CustomFieldMappers {
    /**
     * @author Olaf Otto
     */
    public static class AnnotationMapping {
        private final Annotation annotation;
        private final FieldMapper mapper;

        public AnnotationMapping(Annotation annotation, FieldMapper mapper) {
            this.annotation = annotation;
            this.mapper = mapper;
        }

        public Annotation getAnnotation() {
            return annotation;
        }

        public FieldMapper getMapper() {
            return mapper;
        }
    }

    private static final Collection<AnnotationMapping> NULL = emptyList();
    private final ConcurrentDistinctMultiValueMap<Field, AnnotationMapping> cache
          = new ConcurrentDistinctMultiValueMap<Field, AnnotationMapping>();
    private final ConcurrentDistinctMultiValueMap<Class<? extends Annotation>, FieldMapper> fieldMappers
          = new ConcurrentDistinctMultiValueMap<Class<? extends Annotation>, FieldMapper>();

    public synchronized void add(FieldMapper<?, ?> mapper) {
        this.fieldMappers.put(mapper.getAnnotationType(), mapper);
        this.cache.clear();
    }

    public synchronized void remove(FieldMapper<?, ?> mapper) {
        this.fieldMappers.remove(mapper.getAnnotationType());
        this.cache.clear();
    }

    public Collection<AnnotationMapping> get(MappedFieldMetaData metaData) {
        if (metaData == null) {
            throw new IllegalArgumentException("Method argument metaData must not be null.");
        }

        Collection<AnnotationMapping> mappers = this.cache.get(metaData.getField());

        if (mappers == NULL) {
            return NULL;
        }

        if (mappers != null) {
            return mappers;
        }

        List<AnnotationMapping> compatibleMappers = new ArrayList<AnnotationMapping>();
        for (Annotation annotation : metaData.getAnnotations()) {
            Collection<FieldMapper> mappersForAnnotation = this.fieldMappers.get(annotation.annotationType());
            if (mappersForAnnotation == null) {
                continue; // with next element
            }
            for (FieldMapper<?, ?> mapper:  mappersForAnnotation) {
                if (metaData.getType().isAssignableFrom(mapper.getFieldType())) {
                    compatibleMappers.add(new AnnotationMapping(annotation, mapper));
                }
            }
        }
        mappers = compatibleMappers.isEmpty() ? NULL : compatibleMappers;

        synchronized (this) {
            this.cache.put(metaData.getField(), mappers);
        }

        return mappers;
    }
}
