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
package io.neba.api.resourcemodels;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Map;

/**
 * OSGi services implementing this interface may customize the mapping of arbitrary fields during
 * the resource to model mapping of any resource model.
 * <p>
 * The implementing service specifies {@link #getAnnotationType() the annotation it is responsible for}
 * as well as {@link #getFieldType() the type the mapped fields} must be {@link Class#isAssignableFrom(Class) assignable to}.
 * </p>
 * <p>
 * For example, the following service would be responsible for fields annotated with &#64;MyAnnotation
 * with a type <em>assignable to</em> {@link java.util.Collection}, e.g. List&lt;Resource&gt; or Set&lt;String&gt;.
 * </p>
 * <pre>
 * &#64;Service
 * &#64;Component(immediate = true)
 * public class MyFieldMapper&lt;Collection, MyAnnotation&gt; {
 *     public Class&lt;? extends Collection&gt; getFieldType() {
 *         return Collection.class;
 *     }
 *
 *     public Class&lt;Annotation&gt; getAnnotationType() {
 *         return MyAnnotation.class;
 *     }
 *
 *     public Collection map(OngoingMapping&lt;Collection, MyAnnotation&gt; ongoingMapping) {
 *         ...
 *     }
 * }
 * </pre>
 * <p>
 * Custom mappers are always invoked <em>after</em> all of NEBA's standard mappings have occurred, but before
 * the corresponding value was set on the model's field. They may thus make use of the already resolved
 * value or choose to provide a different one.
 * </p>
 *
 * <p>
 *     <strong>It is crucial for a {@link AnnotatedFieldMapper} to always return a value that is assignment compatible
 *     to the {@link OngoingMapping#getFieldType() field type}, i.e. either of the same or of a more specific type.</strong>
 *     It is insufficient to return a type compatible to the {@link #map(AnnotatedFieldMapper.OngoingMapping) mapping methods}
 *     return type declaration. This return type only represents the type any returned value must be compatible to.<br />
 *     For instance, if a mapper is responsible for {@link java.util.Collection}, it must take care to return the field's actual
 *     collection type, e.g. {@link java.util.List} or {@link java.util.Set}. Otherwise, an exception will arise.
 * </p>
 *
 * <p>
 *     Implementations <strong>must never</strong> store any contextual data provided by the
 *     {@link io.neba.api.resourcemodels.AnnotatedFieldMapper.OngoingMapping}
 *     as this data stems from arbitrary OSGi bundles with independent life cycles.
 *     Storing any data would result in a class loader / memory leak when these bundles change.
 * </p>
 *
 * @param <FieldType> the super type of the {@link java.lang.reflect.Field#getType() field type}
 *        of the mapped type.
 * @param <AnnotationType> the exact type of the annotation this mapper is responsible for.

 * @author Olaf Otto
 */
public interface AnnotatedFieldMapper<FieldType, AnnotationType extends Annotation> {
    /**
     * Represents the contextual data of a field mapping during a resource to model mapping.
     *
     * @param <FieldType> the {@link Field#getType() field type}
     * @param <AnnotationType> the {@link Annotation type}
     *
     * @author Olaf Otto
     */
    interface OngoingMapping<FieldType, AnnotationType> {
        /**
         * @return The currently resolved value of the field,
         * or <code>null</code> if no value could be resolved for the field. This value
         * has not been set to the {@link #getField() field} at this point.
         */
        FieldType getResolvedValue();

        /**
         * @return The instance of {@link #getAnnotationType() the annotation this mapper is registered for}.
         *         Never <code>null</code>.
         */
        AnnotationType getAnnotation();

        /**
         * @return The mapped model. At this point, the mapping is still incomplete and
         *         no post-processors have been invoked on the model. Never <code>null</code>.
         */
        Object getModel();

        /**
         * @return the mapped field. Never <code>null</code>. The field's value was not changed at this point, i.e. it is likely to
         *          deviate from {@link #getResolvedValue()}. Note: do not rely on this {@link java.lang.reflect.Field#getType() field's type}
         *          but use the {@link #getFieldType() provided field type} instead, as these types may be different, for instance in case
         *          of {@link io.neba.api.resourcemodels.Optional} fields.
         */
        Field getField();

        /**
         * @return All annotations (including meta-annotations, i.e. annotations of annotations) present on the field.
         *         Never <code>null</code>.
         */
        Map<Class<? extends Annotation>, Annotation> getAnnotationsOfField();

        /**
         * @return The mapped type of the field. <em>Note: In case the field is {@link io.neba.api.resourcemodels.Optional}, this
         *         type is the component type, i.e. the type targeted by the optional field</em>. However, field mappers
         *         are not applied optional fields but to the subsequent mapping, when the {@link Optional#get() optional value is actually mapped.
         *         Never <code>null</code>.
         */
        Class<?> getFieldType();

        /**
         * @return the generic type parameter of the {@link #getFieldType() field type}, or <code>null</code> if
         *         no such parameter exists.
         */
        Class<?> getFieldTypeParameter();

        /**
         * @return the repository path that shall be resolved to the field's value, as determined by the
         *         field name or {@link io.neba.api.annotations.Path path annotation}. Placeholders
         *         in the path are resolved at this point. Never <code>null</code>.
         */
        String getRepositoryPath();

        /**
         * @return The resource that is mapped to the model. Never <code>null</code>, but may be a synthetic resource.
         */
        Resource getResource();

        /**
         * @return The {@link org.apache.sling.api.resource.ValueMap} representation of the {@link #getResource() resource}.
         *         This value map does support primitive types, e.g. {@link int.class}. May be <code>null</code> if the resource
         *         has no properties, e.g. if it is synthetic.
         */
        ValueMap getProperties();
    }

    /**
     * @return never <code>null</code>.
     */
    Class<? super FieldType> getFieldType();

    /**
     * @return never <code>null</code>.
     */
    Class<AnnotationType> getAnnotationType();

    /**
     * @param ongoingMapping never <code>null</code>.
     *
     * @return The value to be set on the mapped field during the resource to model mapping.
     *         <strong>Must return a value that is assignment-compatible to {@link OngoingMapping#getFieldType()}</strong>.
     *         Can be <code>null</code>.
     */
    FieldType map(OngoingMapping<FieldType, AnnotationType> ongoingMapping);
}
