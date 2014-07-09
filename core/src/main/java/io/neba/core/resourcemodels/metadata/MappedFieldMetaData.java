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

package io.neba.core.resourcemodels.metadata;

import io.neba.api.annotations.Children;
import io.neba.api.annotations.Path;
import io.neba.api.annotations.Reference;
import io.neba.api.annotations.This;
import io.neba.core.util.ReflectionUtil;
import org.apache.commons.lang.ClassUtils;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;

import static io.neba.core.util.ReflectionUtil.getInstantiableCollectionTypes;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.join;
import static org.springframework.util.ReflectionUtils.makeAccessible;

/**
 * Represents meta-data of a mappable {@link io.neba.api.annotations.ResourceModel resource model} field.
 * Used to prevent the costly retrieval of this meta-data upon each resource to model mapping.
 *
 * @author Olaf Otto
 */
public class MappedFieldMetaData {
    /**
     * Whether a property cannot be represented by a resource but must stem
     * from a value map representing the properties of a resource.
     */
    private static boolean isPropertyType(Class<?> type) {
        return type.isPrimitive() ||
               type == String.class ||
               type == Date.class ||
               type == Calendar.class ||
               ClassUtils.wrapperToPrimitive(type) != null;
    }

    private final Field field;
    private final String path;
    private final boolean isReference;
    private final boolean isThisReference;
    private final boolean isPathAnnotationPresent;
    private final boolean isPathExpressionPresent;
    private final boolean isPropertyType;
    private final boolean isCollectionType;
    private final boolean isInstantiableCollectionType;
    private final boolean isChildrenAnnotationPresent;
    private final Class<?> componentType;
    private final Class<?> arrayTypeOfComponentType;
    private final Class<?> fieldType;
    private final Class<?> modelType;

    /**
     * Immediately extracts all metadata for the provided field.
     *
     * @param field must not be <code>null</code>.
     */
    public MappedFieldMetaData(Field field, Class<?> modelType) {
        if (field == null) {
            throw new IllegalArgumentException("Constructor parameter field must not be null.");
        }
        if (modelType == null) {
            throw new IllegalArgumentException("Method argument modelType must not be null.");
        }

        // Atomic initialization
        this.modelType = modelType;
        this.field = field;
        this.fieldType = field.getType();
        this.isCollectionType = Collection.class.isAssignableFrom(field.getType());
        this.isPathAnnotationPresent = field.isAnnotationPresent(Path.class);
        this.isReference = field.isAnnotationPresent(Reference.class);
        this.isThisReference = field.isAnnotationPresent(This.class);
        this.isChildrenAnnotationPresent = field.isAnnotationPresent(Children.class);

        // The following initializations are not atomic but order-sensitive.
        this.componentType = resolveComponentType();
        this.arrayTypeOfComponentType = resolveArrayTypeOfComponentType();
        this.path = getPathInternal();
        this.isPathExpressionPresent = isPathExpressionPresentInternal();
        this.isPropertyType = isPropertyTypeInternal();
        this.isInstantiableCollectionType = ReflectionUtil.isInstantiableCollectionType(getType());

        enforceInstantiableCollectionTypeForExplicitlyMappedFields();
        makeAccessible(field);
    }

    /**
     * @return Whether the path name contains an expression.
     *         An expression has the form ${value}, e.g. &#64;Path("/content/${language}/homepage").
     */
    private boolean isPathExpressionPresentInternal() {
        return this.isPathAnnotationPresent && this.path.contains("$");
    }

    private Class<?> resolveComponentType() {
        Class<?> componentType = null;
        if (this.isCollectionType) {
            componentType = ReflectionUtil.getCollectionComponentType(this.modelType, this.field);
        } else if (getType().isArray()) {
            componentType = getType().getComponentType();
        }
        return componentType;
    }

    private Class<?> resolveArrayTypeOfComponentType() {
        if (this.componentType != null) {
            return Array.newInstance(componentType, 0).getClass();
        }
        return null;
    }


    private void enforceInstantiableCollectionTypeForExplicitlyMappedFields() {
        if (((this.isReference && this.isCollectionType) || this.isChildrenAnnotationPresent)
                && !this.isInstantiableCollectionType) {
            throw new IllegalArgumentException("Unsupported type of field " +
                    this.field + ": Only " + join(getInstantiableCollectionTypes(), ", ") + " are supported.");
        }
    }

    /**
     * Determines the relative or absolute JCR path of a field.
     * The path is derived from either the field name (this is the default)
     * or from an explicit {@link io.neba.api.annotations.Path} annotation.
     */
    private String getPathInternal() {
        String resolvedPath;
        if (isPathAnnotationPresent()) {
            Path path = field.getAnnotation(Path.class);
            if (isBlank(path.value())) {
                throw new IllegalArgumentException("The value of the @" + Path.class.getSimpleName() +
                        " annotation on " + field + " must not be empty");
            }
            resolvedPath = path.value();
        } else {
            resolvedPath = field.getName();
        }
        return resolvedPath;
    }

    /**
     * Determines whether the {@link java.lang.reflect.Field#getType() field type}
     * can only be represented by a property of a {@link org.apache.sling.api.resource.Resource},
     * not a {@link org.apache.sling.api.resource.Resource} itself.
     */
    private boolean isPropertyTypeInternal() {
        Class<?> type = getType();
        return
                // References are always contained in properties of type String or String[].
                isReference()
                        || isPropertyType(type)
                        || (type.isArray() || isCollectionType) && isPropertyType(getComponentType());
    }

    /**
     * @return The {@link org.springframework.util.ReflectionUtils#makeAccessible(java.lang.reflect.Field) accessible}
     *         {@link java.lang.reflect.Field} represented by this meta data.
     */
    public Field getField() {
        return this.field;
    }

    /**
     * @return Whether this field is annotated with {@link io.neba.api.annotations.Reference}.
     */
    public boolean isReference() {
        return this.isReference;
    }

    /**
     * @return Whether this field is annotated with {@link io.neba.api.annotations.This}.
     */
    public boolean isThisReference() {
        return this.isThisReference;
    }

    /**
     * @return The path from which this field's value shall be mapped; may stem
     *         from the field name or a {@link io.neba.api.annotations.Path} annotation.
     */
    public String getPath() {
        return this.path;
    }

    /**
     * @return the {@link java.lang.reflect.Field#getType() field type}.
     */
    public Class<?> getType() {
        return this.fieldType;
    }

    /**
     * @return Whether this field has a {@link io.neba.api.annotations.Path} annotation.
     */
    public boolean isPathAnnotationPresent() {
        return this.isPathAnnotationPresent;
    }


    /**
     * @ return Whether this field has a {@link io.neba.api.annotations.Path} annotation
     * containing an expression such as <code>${path}</code>.
     */
    public boolean isPathExpressionPresent() {
        return this.isPathExpressionPresent;
    }

    /**
     * Whether the type of this field can only be represented by a resource property (and not a resource).
     */
    public boolean isPropertyType() {
        return this.isPropertyType;
    }

    /**
     * Whether the type of this field is assignable from {@link java.util.Collection}.
     */
    public boolean isCollectionType() {
        return this.isCollectionType;
    }

    /**
     * @return whether this field {@link #isCollectionType() is a collection type}
     *         that can be {@link io.neba.core.util.ReflectionUtil#instantiateCollectionType(Class) instantiated}.
     */
    public boolean isInstantiableCollectionType() {
        return isInstantiableCollectionType;
    }

    /**
     * Whether the field has a {@link io.neba.api.annotations.Children} annotation.
     */
    public boolean isChildrenAnnotationPresent() {
        return this.isChildrenAnnotationPresent;
    }

    /**
     * @return The component (generic) type of this collection if this field
     *         {@link #isCollectionType()} with one defined component type,
     *         or <code>null</code>.
     */
    public Class<?> getComponentType() {
        return this.componentType;
    }

    /**
     * @return the array type representation of the {@link #getComponentType() component type},
     * or <code>null</code> if the component type is <code>null</code>.
     */
    public Class<?> getArrayTypeOfComponentType() {
        return arrayTypeOfComponentType;
    }

    @Override
    public int hashCode() {
        return this.field.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this ||
                (
                        obj != null && obj.getClass() == getClass() &&
                                ((MappedFieldMetaData) obj).field.equals(this.field)
                );
    }

    @Override
    public String toString() {
        return getClass().getName() + " [" + this.field + "]";
    }
}
