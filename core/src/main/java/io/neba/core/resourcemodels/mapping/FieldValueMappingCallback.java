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

import io.neba.api.resourcemodels.AnnotatedFieldMapper;
import io.neba.api.resourcemodels.Optional;
import io.neba.core.resourcemodels.metadata.MappedFieldMetaData;
import io.neba.core.util.PrimitiveSupportingValueMap;
import io.neba.core.util.ReflectionUtil;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.cglib.proxy.LazyLoader;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.*;

import static io.neba.core.resourcemodels.mapping.AnnotatedFieldMappers.AnnotationMapping;
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
    private final Object model;
    private final ValueMap properties;
    private final Resource resource;
    private final ConfigurableBeanFactory beanFactory;
    private final AnnotatedFieldMappers annotatedFieldMappers;

    /**
     * @param model    the model to be mapped. Must not be null.
     * @param resource the source of property values for the model. Must not be null.
     * @param factory  must not be null.
     * @param annotatedFieldMappers  must not be null.
     */
    public FieldValueMappingCallback(Object model, Resource resource, BeanFactory factory, AnnotatedFieldMappers annotatedFieldMappers) {
        if (model == null) {
            throw new IllegalArgumentException("Constructor parameter model must not be null.");
        }
        if (resource == null) {
            throw new IllegalArgumentException("Constructor parameter resource must not be null.");
        }
        if (factory == null) {
            throw new IllegalArgumentException("Constructor parameter factory must not be null.");
        }
        if (annotatedFieldMappers == null) {
            throw new IllegalArgumentException("Method argument customFieldMappers must not be null.");
        }

        this.model = model;
        this.properties = toValueMap(resource);
        this.resource = resource;
        this.beanFactory = factory instanceof ConfigurableBeanFactory ? (ConfigurableBeanFactory) factory : null;
        this.annotatedFieldMappers = annotatedFieldMappers;
    }

    /**
     * Invoked for each {@link io.neba.core.resourcemodels.metadata.ResourceModelMetaData#getMappableFields() mappable field}
     * of a {@link io.neba.api.annotations.ResourceModel} to map the {@link MappedFieldMetaData#getField() corresponding field's}
     * value from the resource provided to the {@link #FieldValueMappingCallback(Object, Resource, BeanFactory, AnnotatedFieldMappers) constructor}.
     *
     * @param metaData must not be <code>null</code>.
     */
    public final void doWith(final MappedFieldMetaData metaData) {
        if (metaData == null) {
            throw new IllegalArgumentException("Method argument metaData must not be null.");
        }

        // Prepare the dynamic contextual data of this mapping
        final FieldData fieldData = new FieldData(metaData, evaluateFieldPath(metaData));

        Object value = null;

        if (isMappable(fieldData)) {
            // Explicit lazy loading: provide the lazy loading implementation, not not map anything.
            if (metaData.isOptional()) {
                setField(metaData.getField(), this.model, new OptionalFieldValue(fieldData, this));
                return;
            }

            value = resolve(fieldData);
        }

        value = postProcessResolvedValue(fieldData, value);

        if (value != null) {
            setField(metaData.getField(), this.model, value);
        }
    }

    /**
     * Resumes a mapping temporarily suspended by an {@link Optional} field, i.e.
     * effectively loads a lazy-loaded field value.
     *
     * @param fieldData must not be <code>null</code>.
     * @return the resolved value, or <code>null</code>.
     */
    private Object resumeMapping(FieldData fieldData) {
        return postProcessResolvedValue(fieldData, resolve(fieldData));
    }

    /**
     * Implements the field NEBA contracts (such as non-null collection-typed fields) and applies
     * {@link AnnotatedFieldMapper custom field mappers}.
     *
     * @param fieldData must not be <code>null</code>.
     * @param value can be <code>null</code>.
     * @return the post-processed value, can be <code>null</code>.
     */
    private Object postProcessResolvedValue(FieldData fieldData, Object value) {
        // For convenience, NEBA guarantees that any mappable collection-typed field is never <code>null</code> but rather
        // an empty collection, in case no non-<code>null</code> default value was provided and field is not Optional.
        boolean preventNullCollection =
                value == null &&
                !fieldData.metaData.isOptional() &&
                fieldData.metaData.isInstantiableCollectionType() &&
                getField(fieldData.metaData.getField(), this.model) == null;

        @SuppressWarnings("unchecked")
        Object defaultValue = preventNullCollection ? instantiateCollectionType((Class<Collection>) fieldData.metaData.getType()) : null;

        // Provide the custom mappers with the default value in case of empty collections for convenience
        value = applyCustomMappings(fieldData, value == null ? defaultValue : value);

        return value == null ? defaultValue : value;
    }

    /**
     * Applies all {@link io.neba.api.resourcemodels.AnnotatedFieldMapper registered field mappers}
     * to the provided value and returns the result.
     */
    @SuppressWarnings("unchecked")
    private Object applyCustomMappings(FieldData fieldData, final Object value) {
        Object result = value;
        for (final AnnotationMapping mapping : this.annotatedFieldMappers.get(fieldData.metaData)) {
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
     * If the field is already {@link io.neba.core.resourcemodels.metadata.MappedFieldMetaData#isOptional() optional},
     * {@link #loadChildren(io.neba.core.resourcemodels.mapping.FieldValueMappingCallback.FieldData, org.apache.sling.api.resource.Resource)} directly loads}
     * the children. Otherwise, provides a lazy loading collection.
     *
     * @return never <code>null</code> but rather an empty collection.
     */
    private Collection<?> createCollectionOfChildren(final FieldData field, final Resource parent) {
        if (field.metaData.isOptional()) {
            // The field was already lazy-loaded - do not lazy-load again.
            return loadChildren(field, parent);
        }

        // Create a lazy loading proxy for the collection
        return (Collection<?>) field.metaData.getCollectionProxyFactory().newInstance(new LazyChildrenLoader(field, parent, this));
    }

    /**
     * Loads all children of the given resource, {@link #convert(org.apache.sling.api.resource.Resource, Class) adapts}
     * them if required, and adds them to a newly create collection compatible to the
     * {@link io.neba.core.resourcemodels.metadata.MappedFieldMetaData#getType() field type}, if the adaptation result is not
     * <code>null</code>.
     *
     * @return never null but rather an empty collection.
     */
    @SuppressWarnings("unchecked")
    private Collection<Object> loadChildren(FieldData field, Resource parent) {
        final Class<Collection<Object>> collectionType = (Class<Collection<Object>>) field.metaData.getType();
        final Collection<Object> values = instantiateCollectionType(collectionType);

        final Class<?> targetType = field.metaData.getTypeParameter();
        Iterator<Resource> children = parent.listChildren();

        while (children.hasNext()) {
            Resource child = children.next();
            if (field.metaData.isResolveBelowEveryChildPathPresentOnChildren()) {
                // @Children(resolveBelowEveryChild = "...")
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
     * Resolves the String path(s) stored in the current field to the respective resources and adapts
     * them, if necessary. May provide a single adapted value or a collection of references,
     * with regard to the field's meta data.
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
     * If the field is already {@link io.neba.core.resourcemodels.metadata.MappedFieldMetaData#isOptional() optional},
     * {@link #loadReferences(io.neba.core.resourcemodels.mapping.FieldValueMappingCallback.FieldData, String[]) directly loads}
     * the references. Otherwise, provides a lazy loading collection.
     *
     * @param paths relative or absolute paths to resources.
     * @return never <code>null</code> but rather an empty collection.
     */
    private Collection<Object> createCollectionOfReferences(final FieldData field, final String[] paths) {
        if (field.metaData.isOptional()) {
            // The field was already lazy-loaded - no not lazy-load again.
            return loadReferences(field, paths);
        }
        // Create a lazy loading proxy for the collection
        @SuppressWarnings("unchecked")
		Collection<Object> result = (Collection<Object>) field.metaData.getCollectionProxyFactory().newInstance(new LazyReferencesLoader(field, paths, this));
		return result;
    }

    /**
     * Resolves and converts all references of the given array of paths. <br />
     * Afterwards, the resulting instances are stored in a {@link java.util.Collection} compatible to the
     * collection type of the given {@link io.neba.core.resourcemodels.metadata.MappedFieldMetaData#getType()}.
     *
     * @param paths relative or absolute paths to resources.
     * @return never <code>null</code> but rather an empty collection.
     */
    @SuppressWarnings({"unchecked"})
    private Collection<Object> loadReferences(FieldData field, String[] paths) {
        final Class<Collection<Object>> collectionType = (Class<Collection<Object>>) field.metaData.getType();
        final Collection<Object> values = instantiateCollectionType(collectionType, paths.length);
        String[] resourcePaths = paths;
        if (field.metaData.isAppendPathPresentOnReference()) {
            // @Reference(append = "...")
            resourcePaths = append(field.metaData.getAppendPathOnReference(), paths);
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
     * the properties of resources other than the current resource. Such references cannot be reliably retrieved from the current
     * resource's {@link ValueMap} as the value map may be <code>null</code> and does not support access to properties from parent resources.
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
     * Delegates the evaluation of expressions such as <code>/content/site/${language}/subpage</code>
     * to the bean factory.
     *
     * @see ConfigurableBeanFactory#resolveEmbeddedValue(String)
     */
    private String evaluatePathExpression(String pathWithExpression) {
        String path = pathWithExpression;
        if (this.beanFactory != null) {
            String evaluatedPath = this.beanFactory.resolveEmbeddedValue(pathWithExpression);
            if (!isBlank(evaluatedPath)) {
                path = evaluatedPath;
            }
        }
        return path;
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
     * Implements explicit lazy-loading via {@link io.neba.api.resourcemodels.Optional}. Leverages the internal
     * {@link #resolve(io.neba.core.resourcemodels.mapping.FieldValueMappingCallback.FieldData)}
     * method and {@link io.neba.core.resourcemodels.mapping.FieldValueMappingCallback.FieldData} for this purpose.
     *
     * @author Olaf Otto
     */
    private static class OptionalFieldValue implements Optional<Object> {
        private static final Object NULL = new Object();
        private final FieldData fieldData;
        private final FieldValueMappingCallback callback;
        private Object value = NULL;

        OptionalFieldValue(FieldData fieldData, FieldValueMappingCallback callback) {
            this.fieldData = fieldData;
            this.callback = callback;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Object get() {
            Object o = load();
            if (o == null) {
                throw new NoSuchElementException("The value of " + this.fieldData.metaData.getField() + " resolved to null.");
            }
            return o;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Object orElse(Object defaultValue) {
            Object o = load();
            return o == null ? defaultValue : o;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean isPresent() {
            return orElse(null) != null;
        }

        /**
         * The semantics of the value holder must adhere to the semantics of a non-lazy-loaded field value:
         * The value is loaded exactly once, subsequent or concurrent access to the field value means accessing the
         * same value. Thus, the value is retained and this method is thread-safe.
         */
        private synchronized Object load() {
            if (this.value == NULL) {
                this.value = this.callback.resumeMapping(this.fieldData);
            }
            return this.value;
        }
    }

    /**
     * Lazy-loads collections of children.
     *
     * @see #createCollectionOfChildren(io.neba.core.resourcemodels.mapping.FieldValueMappingCallback.FieldData, org.apache.sling.api.resource.Resource)
     * @author Olaf Otto
     */
    private static class LazyChildrenLoader implements LazyLoader {
        private final FieldData field;
        private final Resource resource;
        private final FieldValueMappingCallback mapper;

        LazyChildrenLoader(FieldData field, Resource resource, FieldValueMappingCallback callback) {
            this.field = field;
            this.resource = resource;
            this.mapper = callback;
        }

        @Override
        public Object loadObject() throws Exception {
            return this.mapper.loadChildren(field, resource);
        }
    }

    /**
     * Lazy-loads collections of references.
     *
     * @see #createCollectionOfReferences(io.neba.core.resourcemodels.mapping.FieldValueMappingCallback.FieldData, String[])
     * @author Olaf Otto
     */
    private static class LazyReferencesLoader implements LazyLoader {
        private final FieldData field;
        private final String[] paths;
        private final FieldValueMappingCallback callback;

        LazyReferencesLoader(FieldData field, String[] paths, FieldValueMappingCallback callback) {
            this.field = field;
            this.paths = paths;
            this.callback = callback;
        }

        @Override
        public Object loadObject() throws Exception {
            return this.callback.loadReferences(field, paths);
        }
    }

    /**
     * @author Olaf Otto
     */
    private static class OngoingFieldMapping implements AnnotatedFieldMapper.OngoingMapping {
        private final Object resolvedValue;
        private final AnnotationMapping mapping;
        private final FieldData fieldData;
        private final Object model;
        private final Resource resource;
        private final ValueMap properties;
        private final MappedFieldMetaData metaData;

        OngoingFieldMapping(Object model,
                                   Object resolvedValue,
                                   AnnotationMapping mapping,
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

        @Override
        public Object getResolvedValue() {
            return resolvedValue;
        }

        @Override
        public Object getAnnotation() {
            return mapping.getAnnotation();
        }

        @Override
        public Object getModel() {
            return model;
        }

        @Override
        public Field getField() {
            return metaData.getField();
        }

        @Override
        public Map<Class<? extends Annotation>, Annotation> getAnnotationsOfField() {
            return metaData.getAnnotations().getAnnotations();
        }

        @Override
        public Class<?> getFieldType() {
            return metaData.getType();
        }

        @Override
        public Class<?> getFieldTypeParameter() {
            return this.metaData.getTypeParameter();
        }

        @Override
        public String getRepositoryPath() {
            return fieldData.path;
        }

        @Override
        public Resource getResource() {
            return resource;
        }

        @Override
        public ValueMap getProperties() {
            return properties;
        }
    }
}