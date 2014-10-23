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

import io.neba.core.resourcemodels.metadata.MappedFieldMetaData;
import io.neba.core.util.PrimitiveSupportingValueMap;
import io.neba.core.util.ReflectionUtil;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import static io.neba.core.util.ReflectionUtil.instantiateCollectionType;
import static io.neba.core.util.StringUtil.append;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.springframework.util.ReflectionUtils.getField;
import static org.springframework.util.ReflectionUtils.setField;

/**
 * Attempts to load the property or resource associated with each
 * {@link io.neba.core.resourcemodels.metadata.MappedFieldMetaData mappable field} of a
 * {@link io.neba.api.annotations.ResourceModel},
 * {@link #convert(org.apache.sling.api.resource.Resource, Class) convert} it to the suitable field type
 * and inject it into the corresponding field.
 *
 * @author Olaf Otto
 */
public class FieldValueMappingCallback {
    /**
     * Represents the the contextual data of a resource model field during
     * {@link FieldValueMappingCallback#doWith(io.neba.core.resourcemodels.metadata.MappedFieldMetaData) mapping}.
     */
    private static final class FieldData {
        private final MappedFieldMetaData metaData;
        private final String path;
        private final boolean isAbsolute;
        private final boolean isRelative;

        private FieldData(MappedFieldMetaData metaData, String path) {
            this.metaData = metaData;
            this.path = path;
            this.isAbsolute = !path.isEmpty() && path.charAt(0) == '/';
            this.isRelative = !this.isAbsolute && path.indexOf('/') != -1;
        }

        private boolean isAbsolute() {
            return this.isAbsolute;
        }

        private boolean isRelative() {
            return isRelative;
        }

        /**
         * <p>
         * Determines whether the mappedFieldMetaData represents a reference to another resource.
         * This is the case IFF:
         * </p>
         *
         * <ul>
         *   <li>it has a type that {@link MappedFieldMetaData#isPropertyType() can only be a property} or</li>
         *   <li>it it is annotated with an absolute path or</li>
         *   <li>it it is annotated with a relative path</li>
         * </ul>.
         */
        private boolean isReferenceToOtherResource() {
            return !this.metaData.isPropertyType() || isAbsolute() || isRelative();
        }
    }

    /**
     * Provides the properties of the resource as a {@link PrimitiveSupportingValueMap}.
     *
     * @param resource must not be <code>null</code>.
     * @return the value map, or <code>null</code> if the resource has no properties,
     *         e.g. if it is synthetic.
     */
    private static ValueMap toValueMap(Resource resource) {
        ValueMap propertyMap = resource.adaptTo(ValueMap.class);
        if (propertyMap != null) {
            propertyMap = new PrimitiveSupportingValueMap(propertyMap);
        }
        return propertyMap;
    }

    private final Object model;
    private final ValueMap properties;
    private final Resource resource;
    private final ConfigurableBeanFactory beanFactory;

    /**
     * @param model    the model to be mapped. Must not be null.
     * @param resource the source of property values for the model. Must not be null.
     * @param factory  must not be null.
     */
    public FieldValueMappingCallback(Object model, Resource resource, BeanFactory factory) {
        this.model = model;
        this.properties = toValueMap(resource);
        this.resource = resource;
        this.beanFactory = factory instanceof ConfigurableBeanFactory ? (ConfigurableBeanFactory) factory : null;
    }

    /**
     * Invoked for each field of a resource model type.
     */
    public final void doWith(MappedFieldMetaData metaData) {
        FieldData fieldData = new FieldData(metaData, evaluateFieldPath(metaData));
        if (isMappable(fieldData)) {

            Object value;

            if (metaData.isThisReference()) {
                value = convertThisResourceToFieldType(fieldData);
            } else if (metaData.isChildrenAnnotationPresent()) {
                value = resolveChildren(fieldData);
            } else {
                value = resolveValueOfField(fieldData);
            }

            if (value != null) {
                setField(metaData.getField(), this.model, value);
            } else if (metaData.isInstantiableCollectionType()){
                preventNullValueInMappableCollectionField(metaData);
            }
        }
    }

    private void preventNullValueInMappableCollectionField(MappedFieldMetaData metaData) {
        Object fieldValue = getField(metaData.getField(), this.model);
        if (fieldValue == null) {
            @SuppressWarnings("unchecked")
            Class<Collection> collectionType = (Class<Collection>) metaData.getType();
            setField(metaData.getField(), this.model, instantiateCollectionType(collectionType));
        }
    }

    /**
     * Evaluates the {@link io.neba.core.resourcemodels.metadata.MappedFieldMetaData#isPathExpressionPresent() path expression}
     * of the field (if any) using the {@link #beanFactory bean factory} of the models source bundle.
     */
    private String evaluateFieldPath(MappedFieldMetaData fieldMetaData) {
        String path = fieldMetaData.getPath();
        if (fieldMetaData.isPathExpressionPresent()) {
            path = evaluatePathExpression(path);
        }
        return path;
    }

    /**
     * <p>
     * Resolves the {@link org.apache.sling.api.resource.Resource#listChildren() children}
     * of a targeted resource. This parent resource may be either:
     * </p>
     * <ul>
     * <li>The current resource, if no {@link io.neba.api.annotations.Path} or
     * {@link io.neba.api.annotations.Reference} annotations are present</li>
     * <li>A resource designated by an absolute or relative path of a path annotation</li>
     * <li>A referenced resource (which may be combined with a path annotation)</li>
     * </ul>
     */
    private Collection<?> resolveChildren(FieldData field) {
        Resource parent = null;
        Collection<?> children = null;

        if (field.metaData.isReference()) {
            String referencedPath = resolvePropertyTypedValue(field, String.class);
            if (!isBlank(referencedPath)) {
                parent = resolveResource(referencedPath, Resource.class);
            }
        } else if (field.metaData.isPathAnnotationPresent()) {
            parent = resolveResource(field.path, Resource.class);
        } else {
            parent = this.resource;
        }

        if (parent != null) {
            children = createCollectionOfChildren(field, parent);
        }

        return children;
    }

    /**
     * Obtains the {@link MappedFieldMetaData#getPath field path},
     * resolves the resource(s) associated with the path and returns the corresponding value.
     * Supports mapping multiple resources (e.g. an array of references) to a collection or array typed field.
     *
     * @return the resolved value, or <code>null</code>.
     */
    private Object resolveValueOfField(FieldData field) {
        final Class<?> fieldType = field.metaData.getType();
        Object value;

        if (field.metaData.isReference()) {
            value = resolveReferenceValueOfField(field, fieldType);
        } else if (field.metaData.isPropertyType()) {
            // The field points to a property as it is not adaptable from a resource
            value = resolvePropertyTypedValue(field);
        } else {
            // The field must point to a resource as it is not resolvable from a property
            value = resolveResource(field.path, fieldType);
        }

        return value;
    }

    private Object resolveReferenceValueOfField(FieldData field, Class<?> fieldType) {
        Object value = null;
        // Regardless of its path, the field references another resource.
        // fetch the field value (the path(s) to the referenced resource(s)) and resolve these resources.
        if (field.metaData.isCollectionType()) {
            String[] referencedResourcePaths = resolvePropertyTypedValue(field, String[].class);
            if (referencedResourcePaths != null) {
                value = resolveCollectionOfReferences(field, referencedResourcePaths);
            }
        } else {
            String referencedResourcePath = resolvePropertyTypedValue(field, String.class);
            if (referencedResourcePath != null) {
                if (field.metaData.isAppendPathPresentOnReference()) {
                    referencedResourcePath += field.metaData.getAppendPathOnReference();
                }
                value = resolveResource(referencedResourcePath, fieldType);
            }
        }
        return value;
    }

    /**
     * This method resolves and converts
     * all references of the given array of paths. <br />
     * Afterwards, the resulting instances are stored in a {@link Collection} compatible to the
     * collection type of the given {@link MappedFieldMetaData}.
     *
     * @param paths relative or absolute paths to resources.
     * @return never <code>null</code> but rather an empty collection.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private Collection resolveCollectionOfReferences(FieldData field, String[] paths) {
        final Class<Collection> collectionType = (Class<Collection>) field.metaData.getType();
        final Collection values = instantiateCollectionType(collectionType, paths.length);

        if (field.metaData.isAppendPathPresentOnReference()) {
            paths = append(field.metaData.getAppendPathOnReference(), paths);
        }

        final Class<?> componentClass = field.metaData.getComponentType();
        for (String resourcePath : paths) {
            Object element = resolveResource(resourcePath, componentClass);
            if (element != null) {
                values.add(element);
            }
        }
        return values;
    }

    @SuppressWarnings("unchecked")
    private Collection createCollectionOfChildren(FieldData field, Resource resource) {
        Class<Collection> collectionType = (Class<Collection>) field.metaData.getType();
        final Collection values = instantiateCollectionType(collectionType);

        final Class<?> componentClass = field.metaData.getComponentType();
        Iterator<Resource> children = resource.listChildren();

        while (children.hasNext()) {
            Resource child = children.next();
            if (field.metaData.isResolveBelowEveryChildPathPresentOnChildren()) {
                // @Children(resolveBelowEveryChild = "...")
                child = child.getChild(field.metaData.getResolveBelowEveryChildPathOnChildren());
                if (child == null) {
                    continue;
                }
            }
            Object adapted = convert(child, componentClass);
            if (adapted != null) {
                values.add(adapted);
            }
        }

        return values;
    }

    /**
     * Uses the current resource's {@link org.apache.sling.api.resource.ResourceResolver}
     * to obtain the resource with the given path.
     * The path can be absolute or relative  to the current resource.
     * {@link #convert(org.apache.sling.api.resource.Resource, Class) Converts} the resolved resource
     * to the given field type if necessary.
     *
     * @return the resolved and converted resource, or <code>null</code>.
     */
    private <T> T resolveResource(final String resourcePath, final Class<T> fieldType) {
        Resource absoluteResource = this.resource.getResourceResolver().getResource(this.resource, resourcePath);
        return convert(absoluteResource, fieldType);
    }

    /**
     * Resolves a field's value via the {@link FieldValueMappingCallback.FieldData#path field path}.
     * Supports conversion from array properties (such as String[]) to the desired collection type of the field.
     *
     * @return the resolved value, or <code>null</code>.
     */
    private Object resolvePropertyTypedValue(FieldData field) {
        Object value;
        if (field.metaData.isInstantiableCollectionType()) {
            value = getArrayPropertyAsCollection(field);
        } else {
            value = resolvePropertyTypedValue(field, field.metaData.getType());
        }
        return  value;
    }

    /**
     * Resolves a field's value using the field's {@link FieldValueMappingCallback.FieldData#path}. If the
     * resource does not have any properties, the field path is absolute
     * (see {@link #isMappable(io.neba.core.resourcemodels.mapping.FieldValueMappingCallback.FieldData)}),
     * in which case the property is resolved via the resource resolver, i.e. the path is an absolute reference
     * to the property of another resource.
     * <br />
     * Ignores all {@link FieldValueMappingCallback.FieldData#metaData meta data}
     * except for the field path as the desired return type is explicitly specified.
     *
     * @return the resolved value, or <code>null</code>.
     */
    private <T> T resolvePropertyTypedValue(FieldData field, Class<T> propertyType) {
        T value;
        if (field.isAbsolute() || field.isRelative()) {
            value = resolvePropertyTypedValueFromForeignResource(field, propertyType);
        } else  {
            if (this.properties == null) {
                throw new IllegalStateException("Tried to map the property " + field +
                                                " even though the resource has no properties.");
            }
            value = this.properties.get(field.path, propertyType);
        }
        return  value;
    }

    /**
     * Resolves a property via a property {@link Resource}. This is used to retrieve relative or absolute references to
     * the properties of resources other than the current resource. Such references cannot be reliably retrieved from the current
     * resource's {@link ValueMap} as it may be <code>null</code> and does not support access to properties from parent resources.
     *
     * @return the resolved value, or <code>null</code>.
     */
    private <T> T resolvePropertyTypedValueFromForeignResource(FieldData field, Class<T> propertyType) {
        Resource property = this.resource.getResourceResolver().getResource(this.resource, field.path);
        if (property == null) {
            return null;
        }

        // Only adaptation to String-types is supported by the property resource
        if (propertyType == String.class || propertyType == String[].class) {
            return property.adaptTo(propertyType);
        }

        // Obtain the ValueMap representation of the parent containing the property to use property conversion
        Resource parent = property.getParent();
        if (parent == null) {
            return null;
        }

        ValueMap properties = parent.adaptTo(ValueMap.class);
        if (properties == null) {
            return  null;
        }

        return new PrimitiveSupportingValueMap(properties).get(property.getName(), propertyType);
    }

    /**
     * The fieldType is a collection type; however the component type of
     * the collection is a property type. Example: List<String>. We must
     * fetch the property with the array-type of the component type (e.g. String[]).
     * and register the values into a new instance of Collection<T>.
     *
     * @return a collection of the resolved values, or <code>null</code> if no value could be resolved.
     */
    @SuppressWarnings("unchecked")
    private <T> T getArrayPropertyAsCollection(FieldData field) {
        Class<?> arrayType = field.metaData.getArrayTypeOfComponentType();
        Object[] elements = (Object[]) resolvePropertyTypedValue(field, arrayType);

        if (elements != null) {
            Collection collection = ReflectionUtil.instantiateCollectionType((Class<Collection>) field.metaData.getType());
            Collections.addAll(collection, elements);
            return (T) collection;
        }

        return null;
    }

    /**
     * Converts the given {@link Resource} to the given target type
     * by either {@link import org.apache.sling.api.adapter.Adaptable#adaptTo(Class) adapting}
     * the resource to the target type or by returning the resource itself if the target type
     * is {@link Resource}.
     */
    @SuppressWarnings("unchecked")
    private <T> T convert(final Resource resource, final Class<T> targetType) {
        T value = null;
        if (resource != null) {
            if (targetType.isAssignableFrom(resource.getClass())) {
                value = (T) resource;
            } else {
                value = resource.adaptTo(targetType);
            }
        }
        return value;
    }

    private Object convertThisResourceToFieldType(FieldData field) {
        return convert(this.resource, field.metaData.getType());
    }

    /**
     * Delegates the evaluation of expressions such as <code>/content/site/${language}/subpage</code>
     * to the bean factory.
     *
     * @see ConfigurableBeanFactory#resolveEmbeddedValue(String)
     */
    private String evaluatePathExpression(String resolvedPath) {
        if (this.beanFactory != null) {
            String evaluatedPath = this.beanFactory.resolveEmbeddedValue(resolvedPath);
            if (!isBlank(evaluatedPath)) {
                resolvedPath = evaluatedPath;
            }
        }
        return resolvedPath;
    }

    /**
     * Determines whether a given field's value
     * can be mapped from either the current resource properties
     * or another (e.g. referenced) resource.
     */
    private boolean isMappable(FieldData field) {
        return this.properties != null ||
                field.metaData.isThisReference() ||
                field.isReferenceToOtherResource();
    }
}