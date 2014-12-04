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

package io.neba.api.resourcemodels.fieldprocessor;

import org.apache.sling.api.resource.Resource;
import org.springframework.beans.factory.BeanFactory;

import java.lang.reflect.Field;
import java.util.Collection;

/**
 * A custom field processor is a service that is registered by NEBA.
 * When NEBA maps a resource to a model, all fields that are
 * {@link #accept(Field, Object)}  accepted} by a processor, are processed by this
 * processor instead of standard NEBA handling.
 * The assignment of fields to their processors are cached.
 * It is not allowed that more than one processor accepts a field.
 * 
 * @author christoph.huber
 */
public interface CustomFieldProcessor {

    /**
     * Callback function that processes a non-collection field.
     *
     * @param field The field to be mapped. Use this read-only, the returned value is injected by NEBA.
     * @param typeParameter If the field type has parameters, the parameter is returned.
     *                      Example: <code>String</code> for type <code>Optional&lt;String&gt;</code>
     * @param resourceModel The resource model to be mapped. <code>field</code> is part of this model.
     * @param resource The resource to be mapped onto the model.
     * @param factory The Sprint bean factory.
     * @param <T> The field's type to be returned.
     * @param <P> The type parameter's type.
     * @param <M> The resource model type.
     * @return <code>null</code> if field cannot be mapped. The mapped value that is injected into the field.
     */
    <T, P, M> T processField(Field field, Class<P> typeParameter, M resourceModel,
                      Resource resource, BeanFactory factory);

    /**
     * Callback function that processes a collection field.
     *
     * @param field The field to be mapped. Use this read-only, the returned value is injected by NEBA.
     * @param initialCollection An empty collection of <code>field</code>'s type. Add the mapped properties
     *                          to this collection.
     * @param typeParameter The collection's type parameter.
     *                      Example: <code>String</code> for type <code>List&lt;String&gt;</code>
     * @param resourceModel The resource model to be mapped. <code>field</code> is part of this model.
     * @param resource The resource to be mapped onto the model.
     * @param factory The Sprint bean factory.
     * @param <P> The type parameter's type.
     * @param <M> The resource model type.
     */
    <P, M> void processCollection(Field field, Collection initialCollection, Class<P> typeParameter,
                            M resourceModel, Resource resource, BeanFactory factory);

    /**
     * If a field is accepted, NEBA will no more handle this! No matter whether there are
     * NEBA annotations defined for this field.
     * A common use case is to implemented accept() by checking the field for a custom annotation like
     * <code>return field.isAnnotationPresent(MyAnnotation.class)</code>
     * @return
     */
    <M> boolean accept(Field field, M resourceModel);
}
