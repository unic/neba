/*
  Copyright 2013 the original author or authors.
  <p>
  Licensed under the Apache License, Version 2.0 the "License";
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at
  <p>
  http://www.apache.org/licenses/LICENSE-2.0
  <p>
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
 */

package io.neba.core.resourcemodels.mapping;

import io.neba.api.resourcemodels.Lazy;
import io.neba.api.spi.AnnotatedFieldMapper;
import io.neba.api.spi.ResourceModelFactory;
import io.neba.core.resourcemodels.metadata.MappedFieldMetaData;
import io.neba.core.util.PrimitiveAndEnumSupportingValueMap;
import io.neba.core.util.ReflectionUtil;
import io.neba.core.util.ResourcePaths;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;

import static io.neba.core.resourcemodels.mapping.AnnotatedFieldMappers.AnnotationMapping;
import static io.neba.core.util.ReflectionUtil.instantiateCollectionType;
import static io.neba.core.util.StringUtil.appendToAll;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.isBlank;

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
    private final Object model;
    private final ValueMap properties;
    private final Resource resource;
    private final AnnotatedFieldMappers annotatedFieldMappers;
    private final PlaceholderVariableResolvers placeholderVariableResolvers;

    /**
     * @param model     the model to be mapped. Must not be <code>null</code>.
     * @param resource  the source of property values for the model. Must not be <code>null</code>.
     * @param factory   must not be <code>null</code>.
     * @param mappers   must not be <code>null</code>.
     * @param resolvers must not be <code>null</code>.
     */
    FieldValueMappingCallback(
            Object model,
            Resource resource,
            ResourceModelFactory factory,
            AnnotatedFieldMappers mappers,
            PlaceholderVariableResolvers resolvers) {

        if (model == null) {
            throw new IllegalArgumentException("Constructor parameter model must not be null.");
        }
        if (resource == null) {
            throw new IllegalArgumentException("Constructor parameter resource must not be null.");
        }
        if (factory == null) {
            throw new IllegalArgumentException("Constructor parameter factory must not be null.");
        }
        if (mappers == null) {
            throw new IllegalArgumentException("Method argument mappers must not be null.");
        }
        if (resolvers == null) {
            throw new IllegalArgumentException("Method argument resolvers must not be null");
        }

        this.model = model;
        this.properties = toValueMap(resource);
        this.resource = resource;
        this.annotatedFieldMappers = mappers;
        this.placeholderVariableResolvers = resolvers;
    }

    /**
     * Invoked for each {@link io.neba.core.resourcemodels.metadata.ResourceModelMetaData#getMappableFields() mappable field}
     * of a {@link io.neba.api.annotations.ResourceModel} to map the {@link MappedFieldMetaData#getField() corresponding field's}
     * value from the resource provided to the {@link #FieldValueMappingCallback(Object, Resource, ResourceModelFactory, AnnotatedFieldMappers, PlaceholderVariableResolvers) constructor}.
     *
     * @param metaData must not be <code>null</code>.
     */
    final void doWith(final MappedFieldMetaData metaData) {
        if (metaData == null) {
            throw new IllegalArgumentException("Method argument metaData must not be null.");
        }

        // Prepare the dynamic contextual data of this mapping
        final FieldData fieldData = new FieldData(metaData, evaluateFieldPath(metaData));
        // Determine whether the mapping can result in a non-null value
        final boolean isMappable = isMappable(fieldData);

        if (metaData.isLazy()) {
            // Lazy fields are never null, regardless of whether a value is mappable.
            Lazy<Object> lazy = isMappable ? new LazyFieldValue(fieldData, this) : LazyFieldValue.EMPTY;
            setField(metaData, lazy);
            return;
        }

        Object value = null;

        if (isMappable) {
            value = resolve(fieldData);
        }

        value = postProcessResolvedValue(fieldData, value);

        if (value != null) {
            setField(metaData, value);
        }
    }

    /**
     * Resumes a mapping temporarily suspended by a {@link Lazy} field, i.e.
     * effectively loads a lazy-loaded field value.
     *
     * @param fieldData must not be <code>null</code>.
     * @return the resolved value, or <code>null</code>.
     */
    private Object resumeMapping(FieldData fieldData) {
        return postProcessResolvedValue(fieldData, resolve(fieldData));
    }

    /**
     * Implements the NEBA contracts for fields, for instance guarantees that collection-typed fields are never <code>null</code>. Applies
     * {@link AnnotatedFieldMapper custom field mappers}.
     *
     * @param fieldData must not be <code>null</code>.
     * @param value     can be <code>null</code>.
     * @return the post-processed value, can be <code>null</code>.
     */
    private Object postProcessResolvedValue(FieldData fieldData, Object value) {
        // For convenience, NEBA guarantees that any mappable collection-typed field is never <code>null</code> but rather
        // an empty collection, in case no non-<code>null</code> default value was provided and the field is not Lazy.
        boolean preventNullCollection =
                value == null &&
                        !fieldData.metaData.isLazy() &&
                        fieldData.metaData.isInstantiableCollectionType() &&
                        getField(fieldData) == null;

        @SuppressWarnings("unchecked")
        Object defaultValue = preventNullCollection ? instantiateCollectionType((Class<Collection<Object>>) fieldData.metaData.getType()) : null;

        // Provide the custom mappers with the default value in case of empty collections for convenience
        value = applyCustomMappings(fieldData, value == null ? defaultValue : value);

        return value == null ? defaultValue : value;
    }

    /**
     * Applies all {@link AnnotatedFieldMapper registered field mappers}
     * to the provided value and returns the result.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private Object applyCustomMappings(FieldData fieldData, final Object value) {
        Object result = value;
        for (final AnnotationMapping<?, ?> mapping : this.annotatedFieldMappers.get(fieldData.metaData)) {
            result = mapping.getMapper().map(new OngoingFieldMapping(this.model, result, mapping, fieldData, this.resource, this.properties));
        }
        return result;
    }

    /**
     * Resolves the field's value with regard to the {@link io.neba.core.resourcemodels.metadata.MappedFieldMetaData}
     * of the field.
     */
    private Object resolve(FieldData fieldData) {
        Object value;
        if (fieldData.metaData.isThisReference()) {
            // The field is a @This reference
            value = convertThisResourceToFieldType(fieldData);
        } else if (fieldData.metaData.isChildrenAnnotationPresent()) {
            // The field is a collection of @Children
            value = resolveChildren(fieldData);
        } else if (fieldData.metaData.isReference()) {
            // The field is a @Reference
            value = resolveReferenceValueOfField(fieldData);
        } else if (fieldData.metaData.isPropertyType()) {
            // The field points to a property of the resource
            value = resolvePropertyTypedValue(fieldData);
        } else {
            // The field points to another resource
            value = resolveResource(fieldData.path, fieldData.metaData.getType());
        }
        return value;
    }

    private Object convertThisResourceToFieldType(FieldData field) {
        return convert(this.resource, field.metaData.getType());
    }

    /**
     * <p>
     * Resolves the {@link org.apache.sling.api.resource.Resource#listChildren() children}
     * of a parent resource. This parent resource may be either:
     * </p>
     * <ul>
     * <li>The current resource, if no {@link io.neba.api.annotations.Path} or
     * {@link io.neba.api.annotations.Reference} annotations are present</li>
     * <li>A resource designated by an absolute or relative path of a path annotation</li>
     * <li>A referenced resource (which may be combined with a {@link io.neba.api.annotations.Path} annotation)</li>
     * </ul>
     */
    private Collection<?> resolveChildren(FieldData field) {
        if (field.metaData.isLazy()) {
            // The field is explicitly lazy, e.g. @Children Lazy<List<Page>> children. Thus, we are asked to load the children at this point since
            // the lazy field is trying to access the children.
            return loadChildren(field);
        } else {
            // Create a lazy loading proxy for the collection
            return (Collection<?>) field.metaData.getLazyLoadingProxy(new LazyChildrenLoader(field, this));
        }
    }

    /**
     * Loads the children for a field annotated with {@link io.neba.api.annotations.Children}.
     * Resolves the parent who's children are to be loaded (e.g. a {@link io.neba.api.annotations.Reference referenced} resource).
     * Loads all children of the parent resource, {@link #convert(org.apache.sling.api.resource.Resource, Class) adapts}
     * them if required, and adds them to a newly create collection compatible to the
     * {@link io.neba.core.resourcemodels.metadata.MappedFieldMetaData#getType() field type}, if the adaptation result is not
     * <code>null</code>.
     *
     * @return never null but rather an empty collection.
     */
    @SuppressWarnings("unchecked")
    private Collection<Object> loadChildren(FieldData field) {
        final Class<Collection<Object>> collectionType = (Class<Collection<Object>>) field.metaData.getType();
        final Collection<Object> values = instantiateCollectionType(collectionType);

        Resource parent = null;
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

        if (parent == null) {
            return values;
        }

        final Class<?> targetType = field.metaData.getTypeParameter();
        Iterator<Resource> children = parent.listChildren();

        while (children.hasNext()) {
            Resource child = children.next();
            if (field.metaData.isResolveBelowEveryChildPathPresentOnChildren()) {
                // As specified via @Children(resolveBelowEveryChild = "...")
                child = child.getChild(field.metaData.getResolveBelowEveryChildPathOnChildren());
                if (child == null) {
                    continue;
                }
            }
            Object adapted = convert(child, targetType);
            if (adapted != null) {
                values.add(adapted);
            }
        }

        return values;
    }

    /**
     * Resolves the String path(s) stored in the resource property designated by the given field to respective resources and adapts
     * them if necessary. May provide a single adapted value or a collection of references,
     * depending on the field's meta data.
     */
    private Object resolveReferenceValueOfField(FieldData field) {
        Object value = null;
        // Regardless of its path, the field references another resource.
        // fetch the field value (the path(s) to the referenced resource(s)) and resolve these resources.
        if (field.metaData.isCollectionType()) {
            String[] referencedResourcePaths = resolvePropertyTypedValue(field, String[].class);
            if (referencedResourcePaths != null) {
                value = createCollectionOfReferences(field, referencedResourcePaths);
            }
        } else {
            String referencedResourcePath = resolvePropertyTypedValue(field, String.class);
            if (referencedResourcePath != null) {
                if (field.metaData.isAppendPathPresentOnReference()) {
                    referencedResourcePath += field.metaData.getAppendPathOnReference();
                }
                value = resolveResource(referencedResourcePath, field.metaData.getType());
            }
        }
        return value;
    }

    /**
     * If the field is already {@link io.neba.core.resourcemodels.metadata.MappedFieldMetaData#isLazy() lazy},
     * {@link #loadReferences(io.neba.core.resourcemodels.mapping.FieldValueMappingCallback.FieldData, String[]) load}
     * the references. Otherwise, provides a lazy loading collection.
     *
     * @param paths relative or absolute paths to resources.
     * @return never <code>null</code> but rather an empty collection.
     */
    private Collection<Object> createCollectionOfReferences(final FieldData field, final String[] paths) {
        if (field.metaData.isLazy()) {
            // The field is explicitly lazy, e.g. Lazy<List<Resource>>.
            // Here, the lazy value tries to load the actual value, thus resolve it.
            return loadReferences(field, paths);
        }
        // Create a lazy loading proxy for the collection
        @SuppressWarnings("unchecked")
        Collection<Object> result = (Collection<Object>) field.metaData.getLazyLoadingProxy(new LazyReferencesLoader(field, paths, this));
        return result;
    }

    /**
     * Resolves and converts all resources defined in the given array of resource paths.<br />
     * The resulting instances are stored in a {@link java.util.Collection} compatible to the
     * collection type of the given {@link io.neba.core.resourcemodels.metadata.MappedFieldMetaData#getType()}.
     *
     * @param paths relative or absolute paths to resources.
     * @return never <code>null</code> but rather an empty collection.
     */
    @SuppressWarnings("unchecked")
    private Collection<Object> loadReferences(FieldData field, String[] paths) {
        final Class<Collection<Object>> collectionType = (Class<Collection<Object>>) field.metaData.getType();
        final Collection<Object> values = instantiateCollectionType(collectionType, paths.length);
        String[] resourcePaths = paths;
        if (field.metaData.isAppendPathPresentOnReference()) {
            // @Reference(append = "...")
            resourcePaths = appendToAll(field.metaData.getAppendPathOnReference(), paths);
        }

        final Class<?> componentClass = field.metaData.getTypeParameter();
        for (String path : resourcePaths) {
            Object element = resolveResource(path, componentClass);
            if (element != null) {
                values.add(element);
            }
        }
        return values;
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
        return value;
    }

    /**
     * Resolves a field's value using the field's {@link FieldValueMappingCallback.FieldData#path}.
     * {@link FieldData#isRelative() relative} or {@link  FieldData#isAbsolute() absolute} paths
     * are interpreted as references to the properties of another resource and are resolved
     * via {@link #resolvePropertyTypedValueFromForeignResource(FieldData, Class)}.
     * <br />
     * Ignores all {@link FieldValueMappingCallback.FieldData#metaData meta data}
     * except for the field path as the desired return type is explicitly specified.
     *
     * @return the resolved value, or <code>null</code>.
     */
    private <T> T resolvePropertyTypedValue(FieldData field, Class<T> propertyType) {
        if (field.isAbsolute() || field.isRelative()) {
            return resolvePropertyTypedValueFromForeignResource(field, propertyType);
        }
        if (this.properties == null) {
            throw new IllegalStateException("Tried to map the property " + field +
                    " even though the resource has no properties.");
        }
        return this.properties.get(field.path, propertyType);
    }

    /**
     * Uses the current resource's {@link org.apache.sling.api.resource.ResourceResolver}
     * to obtain the resource with the given path.
     * The path can be absolute or relative to the current resource.
     * {@link #convert(org.apache.sling.api.resource.Resource, Class) Converts} the resolved resource
     * to the given field type if necessary.
     *
     * @return the resolved and converted resource, or <code>null</code>.
     */
    private <T> T resolveResource(final String resourcePath, final Class<T> targetType) {
        Resource absoluteResource = this.resource.getResourceResolver().getResource(this.resource, resourcePath);
        return convert(absoluteResource, targetType);
    }

    /**
     * Resolves a property via a property {@link Resource}. This is used to retrieve relative or absolute references to
     * the properties of resources other than the current resource. Such references cannot be reliably retrieved using a
     * resource's {@link ValueMap} as the value map may be <code>null</code> and does not support access to properties of parent resources.
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
            return null;
        }

        return new PrimitiveAndEnumSupportingValueMap(properties).get(property.getName(), propertyType);
    }

    /**
     * The fieldType is a collection. However, the component type of
     * the collection is a property type, e.g. List&lt;String&gt;. We must
     * fetch the property with the array-type of the component type (e.g. String[])
     * and add the values to a new instance of Collection&lt;T&gt;.
     *
     * @return a collection of the resolved values, or <code>null</code> if no value could be resolved.
     */
    private Collection<?> getArrayPropertyAsCollection(FieldData field) {
        Class<?> arrayType = field.metaData.getArrayTypeOfTypeParameter();
        Object[] elements = (Object[]) resolvePropertyTypedValue(field, arrayType);

        if (elements != null) {
            @SuppressWarnings("unchecked")
            Collection<Object> collection = ReflectionUtil.instantiateCollectionType((Class<Collection<Object>>) field.metaData.getType());
            Collections.addAll(collection, elements);
            return collection;
        }

        return null;
    }

    /**
     * Evaluates the {@link ResourcePaths.ResourcePath#hasPlaceholders() variables}
     * in the {@link MappedFieldMetaData#getPath()} path} of the field, if any.
     */
    private String evaluateFieldPath(MappedFieldMetaData fieldMetaData) {
        ResourcePaths.ResourcePath path = fieldMetaData.getPath();
        return (path.hasPlaceholders() ? path.resolve(this.placeholderVariableResolvers::resolve) : path).getPath();
    }

    /**
     * Determines whether a given field's value can be mapped from either the current resource properties
     * or another (e.g. referenced) resource.
     */
    private boolean isMappable(FieldData field) {
        return this.properties != null ||
                field.metaData.isThisReference() ||
                field.isReferenceToOtherResource();
    }

    /**
     * Provides the properties of the resource as a {@link PrimitiveAndEnumSupportingValueMap}.
     *
     * @param resource must not be <code>null</code>.
     * @return the value map, or <code>null</code> if the resource has no properties,
     * e.g. if it is synthetic.
     */
    private static ValueMap toValueMap(Resource resource) {
        ValueMap propertyMap = resource.adaptTo(ValueMap.class);
        if (propertyMap != null) {
            propertyMap = new PrimitiveAndEnumSupportingValueMap(propertyMap);
        }
        return propertyMap;
    }

    private Object getField(FieldData fieldData) {
        try {
            return fieldData.metaData.getField().get(this.model);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    private void setField(MappedFieldMetaData metaData, Object value) {
        try {
            metaData.getField().set(this.model, value);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Converts the given {@link Resource} to the given target type
     * by either {@link import org.apache.sling.api.adapter.Adaptable#adaptTo(Class) adapting}
     * the resource to the target type or by returning the resource itself if the target type
     * is {@link Resource}.
     */
    @SuppressWarnings("unchecked")
    private static <T> T convert(final Resource resource, final Class<T> targetType) {
        if (resource == null) {
            return null;
        }
        if (targetType.isAssignableFrom(resource.getClass())) {
            return (T) resource;
        }
        return resource.adaptTo(targetType);
    }

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
         * <p>
         * <ul>
         * <li>it has a type that {@link MappedFieldMetaData#isPropertyType() can only be a property} or</li>
         * <li>it it is annotated with an absolute path or</li>
         * <li>it it is annotated with a relative path</li>
         * </ul>.
         */
        private boolean isReferenceToOtherResource() {
            return !this.metaData.isPropertyType() || isAbsolute() || isRelative();
        }
    }

    /**
     * Implements explicit lazy-loading via {@link io.neba.api.resourcemodels.Lazy}.
     *
     * @author Olaf Otto
     */
    private static class LazyFieldValue implements Lazy<Object> {
        static Lazy<Object> EMPTY = new Lazy<Object>() {
            @Nonnull
            @Override
            public Optional<Object> asOptional() {
                return Optional.empty();
            }
        };

        private static final Object NULL = new Object();

        private final FieldData fieldData;
        private final FieldValueMappingCallback callback;

        private Object value = NULL;

        LazyFieldValue(FieldData fieldData, FieldValueMappingCallback callback) {
            this.fieldData = fieldData;
            this.callback = callback;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        @Nonnull
        public java.util.Optional<Object> asOptional() {
            if (this.value == NULL) {
                load();
            }
            return ofNullable(this.value);
        }

        /**
         * The semantics of the value holder must adhere to the semantics of a non-lazy-loaded field value:
         * The value is loaded exactly once, subsequent or concurrent access to the field value means accessing the
         * same value. Thus, the value is retained and this method is thread-safe.
         */
        private synchronized void load() {
            if (this.value == NULL) {
                this.value = this.callback.resumeMapping(this.fieldData);
            }
        }
    }

    /**
     * Lazy-loads collections of children.
     *
     * @author Olaf Otto
     * @see #resolveChildren(FieldData)
     */
    private static class LazyChildrenLoader implements Callable<Object> {
        private final FieldData field;
        private final FieldValueMappingCallback mapper;

        LazyChildrenLoader(FieldData field, FieldValueMappingCallback callback) {
            this.field = field;
            this.mapper = callback;
        }

        @Nonnull
        @Override
        public Object call() {
            return this.mapper.loadChildren(field);
        }
    }

    /**
     * Lazy-loads collections of references.
     *
     * @author Olaf Otto
     * @see #createCollectionOfReferences(io.neba.core.resourcemodels.mapping.FieldValueMappingCallback.FieldData, String[])
     */
    private static class LazyReferencesLoader implements Callable<Object> {
        private final FieldData field;
        private final String[] paths;
        private final FieldValueMappingCallback callback;

        LazyReferencesLoader(FieldData field, String[] paths, FieldValueMappingCallback callback) {
            this.field = field;
            this.paths = paths;
            this.callback = callback;
        }

        @Override
        @Nonnull
        public Object call() {
            return this.callback.loadReferences(field, paths);
        }
    }

    /**
     * @author Olaf Otto
     */
    private static class OngoingFieldMapping<FieldType, AnnotationType extends Annotation> implements AnnotatedFieldMapper.OngoingMapping<FieldType, AnnotationType> {
        private final FieldType resolvedValue;
        private final AnnotationMapping<FieldType, AnnotationType> mapping;
        private final FieldData fieldData;
        private final Object model;
        private final Resource resource;
        private final ValueMap properties;
        private final MappedFieldMetaData metaData;

        OngoingFieldMapping(Object model,
                            FieldType resolvedValue,
                            AnnotationMapping<FieldType, AnnotationType> mapping,
                            FieldData fieldData,
                            Resource resource,
                            ValueMap properties) {

            this.model = model;
            this.resolvedValue = resolvedValue;
            this.mapping = mapping;
            this.metaData = fieldData.metaData;
            this.fieldData = fieldData;
            this.resource = resource;
            this.properties = properties;
        }

        @CheckForNull
        @Override
        public FieldType getResolvedValue() {
            return resolvedValue;
        }

        @Override
        @Nonnull
        public AnnotationType getAnnotation() {
            return mapping.getAnnotation();
        }

        @Override
        @Nonnull
        public Object getModel() {
            return model;
        }

        @Override
        @Nonnull
        public Field getField() {
            return metaData.getField();
        }

        @Override
        @Nonnull
        public Map<Class<? extends Annotation>, Annotation> getAnnotationsOfField() {
            return metaData.getAnnotations().getAnnotations();
        }

        @Override
        @Nonnull
        public Class<?> getFieldType() {
            return metaData.getType();
        }

        @Override
        @CheckForNull
        public Class<?> getFieldTypeParameter() {
            return this.metaData.getTypeParameter();
        }

        @Override
        @Nonnull
        public String getRepositoryPath() {
            return fieldData.path;
        }

        @Override
        @Nonnull
        public Resource getResource() {
            return resource;
        }

        @Override
        @Nonnull
        public ValueMap getProperties() {
            return properties;
        }
    }
}
