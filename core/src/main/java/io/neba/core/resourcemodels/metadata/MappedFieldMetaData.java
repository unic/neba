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
import io.neba.api.resourcemodels.fieldprocessor.CustomFieldProcessor;
import io.neba.api.resourcemodels.Optional;
import io.neba.core.util.ReflectionUtil;
import org.apache.commons.lang.ClassUtils;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.Factory;
import org.springframework.cglib.proxy.LazyLoader;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import static io.neba.core.util.ReflectionUtil.getInstantiableCollectionTypes;
import static io.neba.core.util.ReflectionUtil.getLowerBoundOfSingleTypeParameter;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.join;
import static org.apache.commons.lang3.reflect.TypeUtils.getRawType;
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
    private final boolean isAppendPathPresentOnReference;
    private final String appendPathOnReference;
    private final boolean isThisReference;
    private final boolean isPathAnnotationPresent;
    private final boolean isPathExpressionPresent;
    private final boolean isPropertyType;
    private final boolean isCollectionType;
    private final boolean isInstantiableCollectionType;
    private final boolean isChildrenAnnotationPresent;
    private final boolean isResolveBelowEveryChildPathPresentOnChildren;
    private final String resolveBelowEveryChildPathOnChildren;
    private final boolean isOptional;
    private final boolean isCustomProcessorPresent;
    private CustomFieldProcessor customFieldProcessor;

    private final Class<?> typeParameter;
    private final Class<?> arrayTypeOfComponentType;
    private final Type genericFieldType;
    private final Class<?> fieldType;
    private final Class<?> modelType;
    private final Factory collectionProxyFactory;


    /**
     * Immediately extracts all metadata for the provided field.
     *
     * @param field must not be <code>null</code>.
     */
    public MappedFieldMetaData(Field field, Class<?> modelType,
                               List<CustomFieldProcessor> customFieldProcessors) {
        if (field == null) {
            throw new IllegalArgumentException("Constructor parameter field must not be null.");
        }
        if (modelType == null) {
            throw new IllegalArgumentException("Method argument modelType must not be null.");
        }

        // Atomic initialization
        this.modelType = modelType;
        this.field = field;
        this.isOptional = field.getType() == Optional.class;
        // Treat Optional<X> fields transparently like X fields: This way, anyone operating on the metadata is not
        // forced to be aware of the lazy-loading value holder indirection but can operate on the target type directly.
        this.genericFieldType = this.isOptional ? getParameterTypeOf(field.getGenericType()) : field.getGenericType();
        this.fieldType = this.isOptional ? getRawType(this.genericFieldType, this.modelType) : field.getType();
        this.isCollectionType = Collection.class.isAssignableFrom(this.fieldType);
        this.isPathAnnotationPresent = field.isAnnotationPresent(Path.class);
        this.isReference = field.isAnnotationPresent(Reference.class);
        this.isThisReference = field.isAnnotationPresent(This.class);
        this.isChildrenAnnotationPresent = field.isAnnotationPresent(Children.class);

        for (CustomFieldProcessor customFieldProcessor : customFieldProcessors) {
            if (customFieldProcessor.accept(field, modelType)) {
                if (this.customFieldProcessor != null) {
                    throw new IllegalStateException("There are multiple processors accepting field " + field.getName() +
                        " in " + modelType.getName() + ": " + this.customFieldProcessor.getClass().getName() + " and " +
                        customFieldProcessor.getClass().getName());
                }
                this.customFieldProcessor = customFieldProcessor;
            }
        }
        isCustomProcessorPresent = customFieldProcessor != null;

        // The following initializations are not atomic but order-sensitive.
        this.isAppendPathPresentOnReference = isAppendPathPresentOnReferenceInternal(field);
        this.appendPathOnReference = getAppendPathFromReference(field);
        this.isResolveBelowEveryChildPathPresentOnChildren = isResolveBelowEveryChildPathPresentOnChildrenInternal(field);
        this.resolveBelowEveryChildPathOnChildren = getResolveBelowEveryChildPathFromChildren(field);
        this.typeParameter = resolveTypeParameter();
        this.arrayTypeOfComponentType = resolveArrayTypeOfComponentType();
        this.path = getPathInternal();
        this.isPathExpressionPresent = isPathExpressionPresentInternal();
        this.isPropertyType = isPropertyTypeInternal();
        this.isInstantiableCollectionType = ReflectionUtil.isInstantiableCollectionType(this.fieldType);

        enforceInstantiableCollectionTypeForExplicitlyMappedFields();
        this.collectionProxyFactory = prepareProxyFactoryForCollectionTypes();

        makeAccessible(field);
    }

    /**
     * Wraps {@link io.neba.core.util.ReflectionUtil#getLowerBoundOfSingleTypeParameter(java.lang.reflect.Type)}
     * in order to provide a field-related error message to signal users which field is affected.
     */
    private Type getParameterTypeOf(Type type) {
        try {
            return getLowerBoundOfSingleTypeParameter(type);
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to resolve a generic parameter type of the mapped field " + this.field + ".", e);
        }
    }

    /**
     * Prepares a proxy instance of a collection type for use as a {@link org.springframework.cglib.proxy.Factory}.
     * Proxy instances are always {@link org.springframework.cglib.proxy.Factory factories}.
     * Using {@link org.springframework.cglib.proxy.Factory#newInstance(org.springframework.cglib.proxy.Callback)}
     * is significantly more efficient than using {@link org.springframework.cglib.proxy.Enhancer#create(Class, org.springframework.cglib.proxy.Callback)}.
     */
    private Factory prepareProxyFactoryForCollectionTypes() {
        if (this.isInstantiableCollectionType) {
            return (Factory) Enhancer.create(this.fieldType, new LazyLoader() {
                @Override
                public Object loadObject() throws Exception {
                    return null;
                }
            });
        }
        return null;
    }

    public Factory getCollectionProxyFactory() {
        return this.collectionProxyFactory;
    }

    private String getAppendPathFromReference(Field field) {
        return this.isAppendPathPresentOnReference ? getAppendPathOfReference(field) : null;
    }

    private boolean isAppendPathPresentOnReferenceInternal(Field field) {
        return isReference && !isBlank(getAppendPathOfReference(field));
    }

    private String getAppendPathOfReference(Field field) {
        return field.getAnnotation(Reference.class).append();
    }

    private boolean isResolveBelowEveryChildPathPresentOnChildrenInternal(Field field) {
        return this.isChildrenAnnotationPresent && !isBlank(getResolveBelowEveryChildPathOfChildren(field));
    }

    private String getResolveBelowEveryChildPathFromChildren(Field field) {
        return this.isResolveBelowEveryChildPathPresentOnChildren ?
                getResolveBelowEveryChildPathOfChildren(field) : null;
    }

    private String getResolveBelowEveryChildPathOfChildren(Field field) {
        String relativePath = field.getAnnotation(Children.class).resolveBelowEveryChild();
        // The path must be relative, otherwise resource#getChild will be equivalent to
        // resolver.getResource("/..."), i.e. the resolution will not be relative.
        return isResolveBelowEveryChildPathPresentOnChildren &&
               relativePath.charAt(0) == '/' ? relativePath.substring(1) : relativePath;
    }

    /**
     * @return Whether the path name contains an expression.
     * An expression has the form ${value}, e.g. &#64;Path("/content/${language}/homepage").
     */
    private boolean isPathExpressionPresentInternal() {
        return this.isPathAnnotationPresent && this.path.contains("$");
    }

    private Class<?> resolveTypeParameter() {
        Class<?> typeParameter = null;
        if (this.isCollectionType) {
            typeParameter = getRawType(getParameterTypeOf(this.genericFieldType), this.modelType);
        } else if (getType().isArray()) {
            typeParameter = getType().getComponentType();
        }
        return typeParameter;
    }

    private Class<?> resolveArrayTypeOfComponentType() {
        if (this.typeParameter != null) {
            return Array.newInstance(typeParameter, 0).getClass();
        }
        return null;
    }


    private void enforceInstantiableCollectionTypeForExplicitlyMappedFields() {
        if (((this.isReference && this.isCollectionType) || this.isChildrenAnnotationPresent
            || (this.isCustomProcessorPresent && this.isCollectionType))
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
                    || (type.isArray() || isCollectionType) && isPropertyType(getTypeParameter());
    }

    /**
     * @return The {@link org.springframework.util.ReflectionUtils#makeAccessible(java.lang.reflect.Field) accessible}
     * {@link java.lang.reflect.Field} represented by this meta data.
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
     * @return Whether this field has a {@link io.neba.api.annotations.Reference} annotation with a  non-empty
     * {@link io.neba.api.annotations.Reference#append() append path}.
     */
    public boolean isAppendPathPresentOnReference() {
        return isAppendPathPresentOnReference;
    }

    /**
     * @return the {@link io.neba.api.annotations.Reference#append() append path} of the
     * {@link io.neba.api.annotations.Reference} annotation, or <code>null</code> if either
     * the annotation or the value for the append path are not present.
     */
    public String getAppendPathOnReference() {
        return appendPathOnReference;
    }

    /**
     * @return Whether this field is annotated with {@link io.neba.api.annotations.This}.
     */
    public boolean isThisReference() {
        return this.isThisReference;
    }

    public boolean isCustomProcessorPresent() {
        return this.isCustomProcessorPresent;
    }
    public CustomFieldProcessor getCustomFieldProcessor() {
        return this.customFieldProcessor;
    }

    /**
     * @return The path from which this field's value shall be mapped; may stem
     * from the field name or a {@link io.neba.api.annotations.Path} annotation.
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
     * that can be {@link io.neba.core.util.ReflectionUtil#instantiateCollectionType(Class) instantiated}.
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
     * @return whether a {@link io.neba.api.annotations.Children} annotation is present with a non-empty
     * {@link io.neba.api.annotations.Children#resolveBelowEveryChild()} path.
     */
    public boolean isResolveBelowEveryChildPathPresentOnChildren() {
        return isResolveBelowEveryChildPathPresentOnChildren;
    }

    /**
     * @return the {@link io.neba.api.annotations.Children#resolveBelowEveryChild()} path, or
     * <code>null</code> if no such annotation or path exist.
     */
    public String getResolveBelowEveryChildPathOnChildren() {
        return resolveBelowEveryChildPathOnChildren;
    }

    /**
     * @return The generic type of this field if it has a generic type declaration, such as <code>List&lt;MyModel&gt; field;</code>
     * or <code>Optional&lt;MyModel&gt; field;</code>
     */
    public Class<?> getTypeParameter() {
        return this.typeParameter;
    }

    /**
     * @return the array type representation of the {@link #getTypeParameter() component type},
     * or <code>null</code> if the component type is <code>null</code>.
     */
    public Class<?> getArrayTypeOfTypeParameter() {
        return arrayTypeOfComponentType;
    }

    /**
     * @return whether the field is of type {@link io.neba.api.resourcemodels.Optional}.
     */
    public boolean isOptional() {
        return isOptional;
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
