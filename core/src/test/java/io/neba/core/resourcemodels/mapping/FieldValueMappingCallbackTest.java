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

import io.neba.core.resourcemodels.mapping.testmodels.OtherTestResourceModel;
import io.neba.core.resourcemodels.mapping.testmodels.TestResourceModel;
import io.neba.core.resourcemodels.metadata.MappedFieldMetaData;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.SyntheticResource;
import org.apache.sling.api.resource.ValueMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;

import static java.lang.Boolean.FALSE;
import static org.apache.commons.lang.ClassUtils.primitiveToWrapper;
import static org.apache.commons.lang.StringUtils.substringAfterLast;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Olaf Otto
 */
@RunWith(MockitoJUnitRunner.class)
public class FieldValueMappingCallbackTest {
    @Mock
    private ValueMap valueMap;
    @Mock
    private ResourceResolver resourceResolver;
    @Mock
    private ConfigurableBeanFactory factory;

    private Resource resource;
    private Resource parentOfResourceTargetedByMapping;
    private Resource resourceTargetedByMapping;

    @SuppressWarnings("unused")
    private Object mappedField;
    private MappedFieldMetaData mappedFieldMetadata;

    private Object targetValue;
    private Object model = this;

    private FieldValueMappingCallback testee;

    @Before
    public void prepareTestResource() {
        withResource(mock(Resource.class));
    }

    /**
     * <pre>
     *     &#64;{@link io.neba.api.annotations.ResourceModel}(types = ...)
     *     public class MyModel {
     *         private boolean someProperty;
     *     }
     * </pre>
     */
    @Test
    public void testMappingOfPrimitiveBooleanField() throws Exception {
        mapPropertyField(boolean.class, true);
        assertFieldIsMapped();
    }

    /**
     * <pre>
     *     &#64;{@link io.neba.api.annotations.ResourceModel}(types = ...)
     *     public class MyModel {
     *         private int someProperty;
     *     }
     * </pre>
     */
    @Test
    public void testMappingOfPrimitiveIntField() throws Exception {
        mapPropertyField(int.class, 1);
        assertFieldIsMapped();
    }

    /**
     * <pre>
     *     &#64;{@link io.neba.api.annotations.ResourceModel}(types = ...)
     *     public class MyModel {
     *         private long someProperty;
     *     }
     * </pre>
     */
    @Test
    public void testMappingOfPrimitiveLongField() throws Exception {
        mapPropertyField(long.class, 1L);
        assertFieldIsMapped();
    }

    /**
     * <pre>
     *     &#64;{@link io.neba.api.annotations.ResourceModel}(types = ...)
     *     public class MyModel {
     *         private float someProperty;
     *     }
     * </pre>
     */
    @Test
    public void testMappingOfPrimitiveFloatField() throws Exception {
        mapPropertyField(float.class, 1F);
        assertFieldIsMapped();
    }

    /**
     * <pre>
     *     &#64;{@link io.neba.api.annotations.ResourceModel}(types = ...)
     *     public class MyModel {
     *         private double someProperty;
     *     }
     * </pre>
     */
    @Test
    public void testMappingOfPrimitiveDoubleField() throws Exception {
        mapPropertyField(double.class, 1D);
        assertFieldIsMapped();
    }

    /**
     * <pre>
     *     &#64;{@link io.neba.api.annotations.ResourceModel}(types = ...)
     *     public class MyModel {
     *         private short someProperty;
     *     }
     * </pre>
     */
    @Test
    public void testMappingOfPrimitiveShortField() throws Exception {
        mapPropertyField(short.class, (short) 1);
        assertFieldIsMapped();
    }

    /**
     * <pre>
     *     &#64;{@link io.neba.api.annotations.ResourceModel}(types = ...)
     *     public class MyModel {
     *         private String someProperty;
     *     }
     * </pre>
     */
    @Test
    public void testMappingOfStringField() throws Exception {
        mapPropertyField(String.class, "test");
        assertFieldIsMapped();
    }

    /**
     * Tests mapping a {@link io.neba.api.annotations.ResourceModel} from a
     * {@link org.apache.sling.api.resource.ResourceUtil#isSyntheticResource(org.apache.sling.api.resource.Resource) synthetic}
     * resource.
     */
    @Test
    public void testMappingOfSyntheticResource() throws Exception {
        withResource(mock(SyntheticResource.class));
        withNullValueMap();
        withResourceTargetedByMapping("/absolute/path");
        mapComplexFieldWithPath(Resource.class, "/absolute/path");
        assertMappedFieldIs(this.resourceTargetedByMapping);
    }

    /**
     * It is expected that the result of a retrieval of a property
     * from a value map can be <code>null</code>. This must not lead to an exception
     * or an attempt to retrieve the value from a child resource.
     */
    @Test
    public void testMappingOfFieldWithoutValue() throws Exception {
        mapPropertyField(String.class, null);
        assertFieldIsFetchedFromValueMap();
        assertChildResourceIsNotLoadedForField();
        assertMappedFieldIsNull();
    }

    /**
     * A complex value (not mapped from a resource property but a resource) can be mapped from a child resource
     * who's relative path matches the field name or defined {@link io.neba.api.annotations.Path}
     * If the path of the corresponding field is not an absolute path to an explicit resource.
     * <p/>
     * <pre>
     *     &#64;{@link io.neba.api.annotations.ResourceModel}(types = ...)
     *     public class MyModel {
     *         private OtherResourceModel child;
     *     }
     * </pre>
     */
    @Test
    public void testValueRetrievalFromChildResource() throws Exception {
        withMockChildResource();
        withResourceTargetedByMappingAdaptingTo(TestResourceModel.class, new TestResourceModel());
        mapChildResourceField(TestResourceModel.class);
        assertFieldIsMapped();
    }

    /**
     * A complex value (not mapped from a resource property but a resource) can be mapped from a resource
     * who's path matches the absolute {@link io.neba.api.annotations.Path}.
     * <p/>
     * <pre>
     *     &#64;{@link io.neba.api.annotations.ResourceModel}(types = ...)
     *     public class MyModel {
     *         &#64;{@link io.neba.api.annotations.Path}("/absolute/path")
     *         private OtherResourceModel someProperty;
     *     }
     * </pre>
     */
    @Test
    public void testValueRetrievalFromAbsolutePath() throws Exception {
        withPropertyFieldWithPath(String.class, "/absolute/path/to/value");
        mapField();
        assertFieldIsNotFetchedFromValueMap();
        assertChildResourceIsNotLoadedForField();
    }

    /**
     * A child resource may also mapped directly, i.e. without any adaptation but as a plain
     * {@link org.apache.sling.api.resource.Resource}.
     * <p/>
     * <pre>
     *     &#64;{@link io.neba.api.annotations.ResourceModel}(types = ...)
     *     public class MyModel {
     *         private Resource child;
     *     }
     * </pre>
     */
    @Test
    public void testDirectMappingOfChildResourceToField() throws Exception {
        withMockChildResource();
        mapChildResourceField(Resource.class);
        assertMappedFieldIs(this.resourceTargetedByMapping);
    }

    /**
     * The resource mapped to the current {@link io.neba.api.annotations.ResourceModel} instance
     * can be obtained in the resource model using "&#64;{@link io.neba.api.annotations.This}".
     * <p/>
     * <pre>
     *     &#64;{@link io.neba.api.annotations.ResourceModel}(types = ...)
     *     public class MyModel {
     *         &#64;{@link io.neba.api.annotations.This}
     *         private Resource resource;
     *     }
     * </pre>
     */
    @Test
    public void testInjectionOfResourceWithThisAnnotation() throws Exception {
        mapThisReference();
        assertFieldIsMapped();
    }

    /**
     * A reference may be a property of a resource containing a path to another resource.
     * Such a reference is automatically resolved and adapted in the presence of a
     * {@link io.neba.api.annotations.Reference} annotation.
     * For example, the current resource may have a String property called "link", containing the value
     * "/path/stored/in/property". The corresponding resource is then resolved and injected.
     * <p/>
     * <pre>
     *     &#64;{@link io.neba.api.annotations.ResourceModel}(types = ...)
     *     public class MyModel {
     *         &#64;{@link io.neba.api.annotations.Reference}
     *         private Resource link;
     *     }
     * </pre>
     */
    @Test
    public void testReferenceResolution() throws Exception {
        withResourceTargetedByMapping("/path/stored/in/property");
        mapReferenceField(Resource.class, "/path/stored/in/property");
        assertMappedFieldIs(this.resourceTargetedByMapping);
    }

    /**
     * A reference may be a property of a resource containing an array of paths to other resources.
     * Such a references are automatically resolved and adapted in the presence of a
     * {@link io.neba.api.annotations.Reference} annotation.
     * For example, the current resource may have a String[] property called "links", containing the values
     * "/first/path/stored/in/property", "/second/path/stored/in/property".
     * The corresponding resources are then resolved and injected.
     * <p/>
     * <pre>
     *     &#64;{@link io.neba.api.annotations.ResourceModel}(types = ...)
     *     public class MyModel {
     *         &#64;{@link io.neba.api.annotations.Reference}
     *         private Collection&lt;Resource&gt; links;
     *     }
     * </pre>
     */
    @Test
    public void testCollectionOfReferencesResolution() throws Exception {
        String[] referencedResources = new String[]{"/first/path/stored/in/property", "/second/path/stored/in/property"};

        withMockResources(referencedResources);
        mapReferenceField(Collection.class, Resource.class, referencedResources);

        assertMappedFieldIsCollectionWithResourcesWithPaths(referencedResources);
    }

    /**
     * Same as {@link #testCollectionOfReferencesResolution()}, but using a {@link java.util.Set} instead
     * of a collection of references.
     */
    @Test
    public void testSetOfReferencesResolution() throws Exception {
        String[] referencedResources = new String[]{"/first/path/stored/in/property", "/second/path/stored/in/property"};

        withMockResources(referencedResources);
        mapReferenceField(Set.class, Resource.class, referencedResources);

        assertMappedFieldIsCollectionWithResourcesWithPaths(referencedResources);
    }

    /**
     * Resources targeted by references may be unresolvable, i.e. their resolution or adaptaion results
     * in a <code>null</code> value. In this case, the <code>null</code> value must not
     * be stored in the injected collection of references.
     */
    @Test
    public void testUnresolvableResourcesInListOfReferences() throws Exception {
        String[] referencedResources = new String[]{"/first/path/stored/in/property", "/second/path/stored/in/property"};

        withResourceTargetedByMapping(referencedResources[0]);
        mapReferenceField(Set.class, Resource.class, referencedResources);

        assertMappedFieldIsCollectionWithResourcesWithPaths(referencedResources[0]);
    }

    /**
     * {@link io.neba.api.annotations.Path} annotations may contain placeholders of the form
     * <code>${variableName}</code>. Such placeholders must be resolved using the {@link org.springframework.context.ApplicationContext}
     * of the {@link io.neba.api.annotations.ResourceModel}.
     */
    @Test
    public void testPlaceholderResolutionInPath() throws Exception {
        withConfigurableBeanFactory();
        withPlaceholderResolution("text-${language}", "text-de");
        withPropertyFieldWithPath(String.class, "text-${language}");
        withPathExpressionDetected();
        mapField();
        assertFieldMapperAttemptsToResolvePlaceholdersIn("text-${language}");
        assertFieldMapperLoadsFromValueMap("text-de");
    }

    /**
     * When no value for a placeholder in a path can be resolved, the original path including the placeholder
     * shall be used.
     *
     * @see #testPlaceholderResolutionInPath()
     */
    @Test
    public void testPlaceholderResolutionWithoutSubstitution() throws Exception {
        withConfigurableBeanFactory();
        withPropertyFieldWithPath(String.class, "text-${language}");
        withPathExpressionDetected();
        mapField();
        assertFieldMapperAttemptsToResolvePlaceholdersIn("text-${language}");
        assertFieldMapperLoadsFromValueMap("text-${language}");
    }

    /**
     * Placeholders can only occur if a {@link io.neba.api.annotations.Path} annotation
     * was used. The mapper must thus not attempt to resolve placeholders in paths
     * resolved from the field name.
     *
     * @see #testPlaceholderResolutionInPath()
     */
    @Test
    public void testPlaceholdersAreOnlyResolvedForPathAnnotationValues() throws Exception {
        withConfigurableBeanFactory();
        mapPropertyField(String.class, "someValue");
        assertFieldMapperDoesNotAttemptToResolvePlaceholders();
    }

    /**
     * A {@link io.neba.api.annotations.This} reference may also adapt the current resource
     * to a different type.
     * <p/>
     * <pre>
     *     &#64;{@link io.neba.api.annotations.ResourceModel}(types = ...)
     *     public class MyModel {
     *         &#64;{@link io.neba.api.annotations.This}
     *         private OtherResourceModel resource;
     *     }
     * </pre>
     */
    @Test
    public void testMappingToOtherTestModelAsThisReference() throws Exception {
        OtherTestResourceModel target = new OtherTestResourceModel();
        Class<OtherTestResourceModel> fieldType = OtherTestResourceModel.class;
        withResourceAdaptingTo(fieldType, target);
        mapThisReference(fieldType, target);
        assertFieldIsMapped();
    }

    /**
     * A {@link io.neba.api.annotations.Path} annotation may point to an absolute resource
     * and include an adaptation to the annotated field type.
     * <p/>
     * <pre>
     *     &#64;{@link io.neba.api.annotations.ResourceModel}(types = ...)
     *     public class MyModel {
     *         &#64;{@link io.neba.api.annotations.Path}("/another/resource")
     *         private OtherResourceModel resource;
     *     }
     * </pre>
     */
    @Test
    public void testMappingToOtherModelByPath() throws Exception {
        withResourceTargetedByMapping("/another/resource");
        withResourceTargetedByMappingAdaptingTo(OtherTestResourceModel.class, new OtherTestResourceModel());
        mapComplexFieldWithPath(OtherTestResourceModel.class, "/another/resource");
        assertFieldIsMapped();
    }

    /**
     * A mapped resource may not have any properties.
     * It may however have children that can be mapped. Ensure that the child mapping is executed
     * even if the resource's properties (value map) is null.
     */
    @Test
    public void testChildValuesAreStillResolvedIfResourceHasNoProperties() throws Exception {
        withNullValueMap();
        withMockChildResource();
        mapChildResourceField(Resource.class);
        assertMappedFieldIs(this.resourceTargetedByMapping);
    }

    /**
     * Test the retrieval of the children of the current resources.
     * <p/>
     * <pre>
     *     &#64;{@link io.neba.api.annotations.ResourceModel}(types = ...)
     *     public class MyModel {
     *         &#64;{@link io.neba.api.annotations.Children}
     *         private List&lt;Resource&gt; children;
     *     }
     * </pre>
     */
    @Test
    public void testChildrenAnnotationOnListOfResources() throws Exception {
        withField(Collection.class);
        withCollectionTypedField();
        withInstantiableCollectionTypedField();
        withComponentType(Resource.class);
        withChildrenAnnotationPresent();
        withMockChildResource();
        mapField();
        assertMappedFieldIsCollectionWithResourcesWithPaths(resourceTargetedByMapping.getPath());
    }

    /**
     * Test the retrieval of the children of the current resources with adaptation to
     * the desired target type (component type of the collection).
     * <p/>
     * <pre>
     *     &#64;{@link io.neba.api.annotations.ResourceModel}(types = ...)
     *     public class MyModel {
     *         &#64;{@link io.neba.api.annotations.Children}
     *         private List&lt;ModelForChild&gt; children;
     *     }
     * </pre>
     */
    @Test
    public void testChildrenAnnotationOnListOfModels() throws Exception {
        withField(Collection.class);
        withCollectionTypedField();
        withInstantiableCollectionTypedField();
        withComponentType(TestResourceModel.class);
        withChildrenAnnotationPresent();
        withMockChildResource();
        withResourceTargetedByMappingAdaptingTo(TestResourceModel.class, new TestResourceModel());
        mapField();
        assertMappedFieldIsCollectionContainingTargetValue();
    }


    /**
     * Test the retrieval of the children of the resource targeted by the
     * relative or absolute {@link io.neba.api.annotations.Path}
     * with adaptation to the desired target type (component type of the collection).
     * <p/>
     * <pre>
     *     &#64;{@link io.neba.api.annotations.ResourceModel}(types = ...)
     *     public class MyModel {
     *         &#64;{@link io.neba.api.annotations.Path}("some/path")
     *         &#64;{@link io.neba.api.annotations.Children}
     *         private List&lt;ModelForChild&gt; children;
     *     }
     * </pre>
     */
    @Test
    public void testChildrenAnnotationWithPathAnnotation() throws Exception {
        withResourceTargetedByMapping("field/child");
        withParentOfTargetResource("field");
        withField(Collection.class);
        withInstantiableCollectionTypedField();
        withCollectionTypedField();
        withComponentType(TestResourceModel.class);
        withPathAnnotationPresent();
        withChildrenAnnotationPresent();

        withResourceTargetedByMappingAdaptingTo(TestResourceModel.class, new TestResourceModel());
        mapField();
        assertMappedFieldIsCollectionContainingTargetValue();
    }

    /**
     * Test the retrieval of the children of the resource targeted by the
     * {@link io.neba.api.annotations.Reference} stored in the property
     * designated by the  relative or absolute {@link io.neba.api.annotations.Path}
     * with adaptation to the desired target type (component type of the collection).
     * <p/>
     * <pre>
     *     &#64;{@link io.neba.api.annotations.ResourceModel}(types = ...)
     *     public class MyModel {
     *         &#64;{@link io.neba.api.annotations.Path}("jcr:content/someLink")
     *         &#64;{@link io.neba.api.annotations.Reference}
     *         &#64;{@link io.neba.api.annotations.Children}
     *         private List&lt;ModelForChild&gt; children;
     *     }
     * </pre>
     */
    @Test
    public void testChildrenAnnotationWithPathAndReferenceAnnotations() throws Exception {
        withResourceTargetedByMapping("/referenced/path/child");
        withParentOfTargetResource("/referenced/path");
        withField(Collection.class);
        withCollectionTypedField();
        withInstantiableCollectionTypedField();
        withComponentType(TestResourceModel.class);
        withPathAnnotationPresent();
        withReferenceAnnotationPresent();
        withPropertyValue("/referenced/path");
        withChildrenAnnotationPresent();
        withResourceTargetedByMappingAdaptingTo(TestResourceModel.class, new TestResourceModel());

        mapField();

        assertFieldIsFetchedFromValueMap();
        assertMappedFieldIsCollectionContainingTargetValue();
    }

    /**
     * Test the retrieval of the children of the resource targeted by the
     * {@link io.neba.api.annotations.Reference} with adaptation to the
     * desired target type (component type of the collection).
     * <p/>
     * <pre>
     *     &#64;{@link io.neba.api.annotations.ResourceModel}(types = ...)
     *     public class MyModel {
     *         &#64;{@link io.neba.api.annotations.Reference}
     *         &#64;{@link io.neba.api.annotations.Children}
     *         private List&lt;ModelForChild&gt; link;
     *     }
     * </pre>
     */
    @Test
    public void testChildrenAnnotationWithReferenceAnnotation() throws Exception {
        withResourceTargetedByMapping("/referenced/path/child");
        withParentOfTargetResource("/referenced/path");
        withField(Collection.class);
        withCollectionTypedField();
        withInstantiableCollectionTypedField();
        withComponentType(TestResourceModel.class);
        withReferenceAnnotationPresent();
        withPropertyValue("/referenced/path");
        withChildrenAnnotationPresent();
        withResourceTargetedByMappingAdaptingTo(TestResourceModel.class, new TestResourceModel());

        mapField();

        assertFieldIsFetchedFromValueMap();
        assertMappedFieldIsCollectionContainingTargetValue();
    }

    /**
     * A retrieved child may be <code>null</code>, e.g. due to an unsuccessful adaptation.
     * Such a <code>null</code> value must not be inserted into the injected collection of children.
     */
    @Test
    public void testChildrenWithNullValuesAsAdaptationResult() throws Exception {
        withField(Collection.class);
        withCollectionTypedField();
        withInstantiableCollectionTypedField();
        withComponentType(TestResourceModel.class);
        withChildrenAnnotationPresent();
        withMockChildResource();
        mapField();
        assertMappedFieldIsEmptyCollection();
    }

    /**
     * Properties of a resource may be arrays. In this case, one may use the corresponding collection
     * types instead of arrays, e.g. <code>List&lt;String&gt;</code> instead of <code>String[]</code>.
     */
    @Test
    public void testMappingOfArrayPropertyToCollection() throws Exception {
        String[] propertyValues = {"first value", "second value"};

        withField(Collection.class);
        withInstantiableCollectionTypedField();
        withComponentType(String.class);
        withPropertyTypedField();
        withPropertyValue(propertyValues);

        mapField();

        assertFieldIsFetchedFromValueMapAs(String[].class);
        assertMappedFieldIsCollectionWithEntries(propertyValues);
    }

    /**
     * Resource models can also be mapped from resources without properties - i.e. synthetic resources.
     * In this case, only fields with absolute or relative mapping paths or non-property types can be resolved.
     * Test that a property type is mapped from an absolute path
     */
    @Test
    public void testResolutionOfPropertyWithAbsolutePath() throws Exception {
        withPropertyFieldWithPath(String.class, "/other/resource/propertyName");
        withResourceTargetedByMapping("/other/resource/propertyName");
        withResourceTargetedByMappingAdaptingTo(String.class, "propertyValue");

        mapField();

        assertFieldIsMapped();
    }

    /**
     * Resource models can also be mapped from resources without properties - i.e. synthetic resources.
     * In this case, only fields with absolute or relative mapping paths or non-property types can be resolved.
     * Test that a non-property type is mapped, e.g. from a child resource.
     */
    @Test
    public void testResolutionOfChildResourceOccursEvenIfResourceHasNoProperties() throws Exception {
        withNullValueMap();
        withField(Resource.class);
        withFieldPath("field");
        withMockChildResource();

        mapField();

        assertMappedFieldIs(this.resourceTargetedByMapping);
    }

    /**
     * Resource models can also be mapped from resources without properties - i.e. synthetic resources.
     * In this case, only fields with absolute or relative mapping paths or non-property types can be resolved.
     * Test that a property type is mapped from an absolute path
     */
    @Test
    public void testResolutionOfPropertyWithAbsolutePathOccursEvenIfResourceHasNoProperties() throws Exception {
        withNullValueMap();
        withPropertyFieldWithPath(String.class, "/other/resource/propertyName");
        withResourceTargetedByMapping("/other/resource/propertyName");
        withResourceTargetedByMappingAdaptingTo(String.class, "propertyValue");

        mapField();

        assertFieldIsMapped();
    }

    /**
     * Resource models can also be mapped from resources without properties - i.e. synthetic resources.
     * In this case, only fields with absolute or relative mapping paths or non-property types can be resolved.
     * Test that a property type is mapped from a relative path
     */
    @Test
    public void testResolutionOfPropertyWithRelativePathOccursEvenIfResourceHasNoProperties() throws Exception {
        withNullValueMap();
        withPropertyFieldWithPath(String.class, "../other/resource/propertyName");
        withResourceTargetedByMapping("../other/resource/propertyName");
        withResourceTargetedByMappingAdaptingTo(String.class, "propertyValue");

        mapField();

        assertFieldIsMapped();
    }

    /**
     * Resource models can also be mapped from resources without properties - i.e. synthetic resources.
     * In this case, only fields with absolute or relative mapping paths or non-property types can be resolved.
     * Test that a property type is mapped from an absolute path and that property conversion to Boolean
     * occurs by retrieval of the property via the parent's {@link ValueMap} representation.
     */
    @Test
    public void testResolutionOfPropertyWithAbsolutePathUsesValueMapToRetrieveNonStringValues() throws Exception {
        withPropertyFieldWithPath(Boolean.class, "/other/resource/propertyName");
        withResourceTargetedByMapping("/other/resource/propertyName");
        withParentOfTargetResource("/other/resource");
        withParentOfTargetResourceProperty("propertyName", FALSE);
        mapField();

        assertFieldIsMapped();
    }

    /**
     * Resource models can also be mapped from resources without properties - i.e. synthetic resources.
     * In this case, only fields with absolute or relative mapping paths or non-property types can be resolved.
     * Test that a property type is mapped from a relative path and that property conversion to Boolean
     * occurs by retrieval of the property via the parent's {@link ValueMap} representation.
     */
    @Test
    public void testResolutionOfPropertyWithRelativePathUsesValueMapToRetrieveNonStringValues() throws Exception {
        withPropertyFieldWithPath(Boolean.class, "../other/resource/propertyName");
        withResourceTargetedByMapping("../other/resource/propertyName");
        withParentOfTargetResource("../other/resource");
        withParentOfTargetResourceProperty("propertyName", FALSE);
        mapField();

        assertFieldIsMapped();
    }

    /**
     * Resource models can also be mapped from resources without properties - i.e. synthetic resources.
     * In this case, only fields with absolute or relative mapping paths or non-property types can be resolved.
     * Test that the mapping tolerates if the parent of a mapped property does not exist (e.g., mapping to root nodes)
     */
    @Test
    public void testResolutionOfNonStringPropertyFromForeignResourceToleratesNullParent() throws Exception {
        withPropertyFieldWithPath(Boolean.class, "/other/resource/propertyName");
        withResourceTargetedByMapping("/other/resource/propertyName");
        mapField();

        assertMappedFieldIsNull();
    }

    /**
     * Resource models can also be mapped from resources without properties - i.e. synthetic resources.
     * In this case, only fields with absolute or relative mapping paths or non-property types can be resolved.
     * Test that the mapping tolerates if the parent of a mapped property cannot be adapted to {@link ValueMap}
     * (e.g. in case of a synthetic resource).
     */
    @Test
    public void testResolutionOfNonStringPropertyFromForeignResourceToleratesNullValueMap() throws Exception {
        withNullValueMap();
        withPropertyFieldWithPath(Boolean.class, "/other/resource/propertyName");
        withResourceTargetedByMapping("/other/resource/propertyName");
        withParentOfTargetResource("/other/resource");

        mapField();

        assertMappedFieldIsNull();
    }

    /**
     * Resource models can also be mapped from resources without properties - i.e. synthetic resources.
     * In this case, only fields with absolute or relative mapping paths or non-property types can be resolved.
     * Test that the mapping supports resolution of string arrays through direct adaptation from the
     * property resource.
     */
    @Test
    public void testResolutionOfArrayStringPropertyFromForeignResource() throws Exception {
        withPropertyFieldWithPath(String[].class, "/other/resource/propertyName");
        withResourceTargetedByMapping("/other/resource/propertyName");
        withParentOfTargetResource("/other/resource");
        withResourceTargetedByMappingAdaptingTo(String[].class, new String[]{"first value", "second value"});

        mapField();

        assertFieldIsMapped();
    }

    /**
     * It is possible for a developer to define a field
     * that could be mapped from a resource property (not final, not static,
     * not {@link io.neba.api.annotations.Unmapped} etc.), but with a
     * type supported by neither {@link FieldValueMappingCallback} nor
     * {@link org.apache.sling.api.resource.ValueMap}.
     * In this case, the value must not be mapped.
     */
    @Test
    public void testMappingOfPropertyToUnsupportedType() throws Exception {
        withField(Vector.class);
        withComponentType(String.class);
        withPropertyTypedField();
        withPropertyValue(new String[]{"first value", "second value"});

        mapField();

        assertMappedFieldIsNull();
    }

    @Test
    public void testPreventionOfNullValuesInReferenceCollectionFieldWithoutDefaultValue() throws Exception {
        withField(Collection.class);
        withInstantiableCollectionTypedField();
        withReferenceAnnotationPresent();

        mapField();

        assertMappedFieldIsEmptyCollection();
    }

    @Test
    public void testPreventionOfNullValuesInMappableCollectionFieldWithoutDefaultValue() throws Exception {
        withField(Collection.class);
        withInstantiableCollectionTypedField();
        withPropertyTypedField();
        withComponentType(String.class);

        mapField();

        assertMappedFieldIsEmptyCollection();
    }

    @Test
    public void testDefaultValueOfMappableCollectionTypedFieldIsNotOverwritten() throws Exception {
        withField(Collection.class);
        withInstantiableCollectionTypedField();
        withPropertyTypedField();
        withComponentType(String.class);

        Collection defaultValue = mock(Collection.class);
        withDefaultFieldValue(defaultValue);

        mapField();

        assertMappedFieldIs(defaultValue);
    }

    @Test
    public void testDefaultValueOfMappableCollectionTypedReferenceFieldIsNotOverwritten() throws Exception {
        withField(Collection.class);
        withInstantiableCollectionTypedField();
        withReferenceAnnotationPresent();

        Collection defaultValue = mock(Collection.class);
        withDefaultFieldValue(defaultValue);

        mapField();

        assertMappedFieldIs(defaultValue);
    }

    private void withDefaultFieldValue(Object value) {
        this.mappedField = value;
    }

    private void withInstantiableCollectionTypedField() {
        doReturn(true).when(this.mappedFieldMetadata).isInstantiableCollectionType();
    }

    private void assertMappedFieldIsEmptyCollection() {
        assertThat(this.mappedField).isInstanceOf(Collection.class);
        assertThat((Collection) this.mappedField).isEmpty();
    }

    private void assertMappedFieldIsCollectionWithEntries(Object... entries) {
        assertThat(this.mappedField).isInstanceOf(Collection.class);
        assertThat((Collection<?>) this.mappedField).containsOnly(entries);
    }

    private void withParentOfTargetResource(String path) {
        this.parentOfResourceTargetedByMapping = mock(Resource.class);
        when(this.parentOfResourceTargetedByMapping.getPath()).thenReturn(path);
        when(this.resourceResolver.getResource(eq(this.resource), eq(path)))
                .thenReturn(this.parentOfResourceTargetedByMapping);
        @SuppressWarnings("unchecked")
        Iterator<Resource> it = mock(Iterator.class);
        when(it.hasNext()).thenReturn(true, false);
        when(it.next()).thenReturn(this.resourceTargetedByMapping).thenThrow(new IllegalStateException());
        when(this.parentOfResourceTargetedByMapping.listChildren()).thenReturn(it);

        when(this.resourceTargetedByMapping.getParent()).thenReturn(this.parentOfResourceTargetedByMapping);
    }

    private <T> void withParentOfTargetResourceProperty(String propertyName, T propertyValue) {
        this.targetValue = propertyValue;
        ValueMap properties = mock(ValueMap.class);
        when(this.parentOfResourceTargetedByMapping.adaptTo(eq(ValueMap.class))).thenReturn(properties);
        when(properties.get(eq(propertyName), eq(propertyValue.getClass()))).thenReturn(propertyValue);
    }

    private void assertMappedFieldIsCollectionContainingTargetValue() {
        assertThat(this.mappedField).isInstanceOf(Collection.class);
        assertThat((Collection) this.mappedField).containsOnly(this.targetValue);
    }

    private void withChildrenAnnotationPresent() {
        doReturn(true).when(this.mappedFieldMetadata).isChildrenAnnotationPresent();
    }

    private void mapPropertyField(Class<?> fieldType, Object propertyValue) throws SecurityException, NoSuchFieldException {
        withPropertyField(fieldType, propertyValue);
        mapField();
        this.targetValue = propertyValue;
    }

    private void mapReferenceField(Class<?> fieldType, Object propertyValue) throws SecurityException, NoSuchFieldException {
        withPropertyField(fieldType, propertyValue);
        withReferenceAnnotationPresent();
        mapField();
        this.targetValue = propertyValue;
    }

    @SuppressWarnings("rawtypes")
    private void mapReferenceField(Class<?> fieldType, Class<?> componentType, Object propertyValue) throws NoSuchFieldException {
        withPropertyField(fieldType, propertyValue);
        withComponentType(componentType);
        withReferenceAnnotationPresent();
        withInstantiableCollectionTypedField();
        withCollectionTypedField();
        mapField();
        this.targetValue = propertyValue;
    }

    private void mapComplexFieldWithPath(Class<?> fieldType, String fieldPath) throws NoSuchFieldException {
        withField(fieldType);
        withFieldPath(fieldPath);
        mapField();
    }

    private void withPropertyFieldWithPath(Class<?> fieldType, String fieldPath) throws NoSuchFieldException {
        withField(fieldType);
        withFieldPath(fieldPath);
        withPathAnnotationPresent();
        withPropertyTypedField();
    }

    private void withPathExpressionDetected() {
        when(this.mappedFieldMetadata.isPathExpressionPresent()).thenReturn(true);
    }

    private void mapChildResourceField(Class<?> fieldType) throws NoSuchFieldException {
        withField(fieldType);
        mapField();
    }

    private void mapThisReference() throws NoSuchFieldException {
        mapThisReference(Resource.class, this.resource);
    }

    private <T> void mapThisReference(Class<T> fieldType, T targetValue) throws NoSuchFieldException {
        withField(fieldType);
        withThisReferenceTypedField();
        this.targetValue = targetValue;
        mapField();
    }

    /**
     * Initializes <code>resource</code> with <code>valueMap</code> and <code>resourceResolver</code>.
     */
    private void withResource(final Resource mock) {
        this.resource = mock;
        when(this.resource.adaptTo(eq(ValueMap.class))).thenReturn(this.valueMap);
        when(this.resource.getResourceResolver()).thenReturn(this.resourceResolver);
        when(this.resource.getPath()).thenReturn("/test/resource/path");
    }

    private <T> void withResourceAdaptingTo(Class<T> type, T target) {
        when(this.resource.adaptTo(eq(type))).thenReturn(target);
        this.targetValue = target;
    }

    private void withPlaceholderResolution(String key, String value) {
        when(this.factory.resolveEmbeddedValue(key)).thenReturn(value);
    }

    private void withConfigurableBeanFactory() {
        this.factory = mock(ConfigurableBeanFactory.class);
    }

    private <T> void withResourceTargetedByMappingAdaptingTo(Class<T> type, T value) {
        this.targetValue = value;
        when(this.resourceTargetedByMapping.adaptTo(eq(type))).thenReturn(value);
    }

    private void withMockChildResource() {
        this.resourceTargetedByMapping = mock(Resource.class);
        final String absoluteChildPath = this.resource.getPath() + "/field";
        when(this.resourceTargetedByMapping.getPath()).thenReturn(absoluteChildPath);
        when(this.resource.getChild("field")).thenReturn(resourceTargetedByMapping);
        when(this.resourceResolver.getResource(eq(this.resource), eq("field"))).thenReturn(resourceTargetedByMapping);
        @SuppressWarnings("unchecked")
        Iterator<Resource> ci = mock(Iterator.class);
        when(ci.hasNext()).thenReturn(true, false);
        when(ci.next()).thenReturn(this.resourceTargetedByMapping).thenThrow(new IllegalStateException());
        doReturn(ci).when(this.resource).listChildren();
    }

    /**
     * Creates a resource mock <code>resourceTargetedByMapping</code> that can be resolved with
     * <code>path</code> and returns the path.
     */
    private void withResourceTargetedByMapping(String path) {
        this.resourceTargetedByMapping = mock(Resource.class);
        when(this.resourceTargetedByMapping.getPath()).thenReturn(path);
        when(this.resourceResolver.getResource(eq(this.resource), eq(path)))
                .thenReturn(this.resourceTargetedByMapping);
        when(this.resourceTargetedByMapping.getName()).thenReturn(substringAfterLast(path, "/"));
    }

    private void withMockResources(String... absoluteResourcePaths) {
        for (String path : absoluteResourcePaths) {
            Resource resource = mock(Resource.class);
            when(resource.getPath()).thenReturn(path);
            when(this.resourceResolver.getResource(eq(this.resource), eq(path)))
                    .thenReturn(resource);
        }
    }

    private void withNullValueMap() {
        when(this.resource.adaptTo(eq(ValueMap.class))).thenReturn(null);
    }

    private void withPropertyField(Class<?> fieldType, Object propertyValue) throws NoSuchFieldException {
        withField(fieldType);
        withPropertyTypedField();
        withPropertyValue(propertyValue);
    }

    private void withComponentType(Class<?> componentType) {
        doReturn(componentType).when(this.mappedFieldMetadata).getComponentType();
        doReturn(Array.newInstance(componentType, 0).getClass()).when(this.mappedFieldMetadata).getArrayTypeOfComponentType();
    }

    private void withCollectionTypedField() {
        doReturn(true).when(this.mappedFieldMetadata).isCollectionType();
    }

    private void withPathAnnotationPresent() {
        doReturn(true).when(this.mappedFieldMetadata).isPathAnnotationPresent();
    }

    private void withFieldPath(String referencePath) {
        doReturn(referencePath).when(this.mappedFieldMetadata).getPath();
    }

    private void withReferenceAnnotationPresent() {
        doReturn(true).when(this.mappedFieldMetadata).isReference();
        doReturn(true).when(this.mappedFieldMetadata).isPropertyType();
    }

    private void withPropertyTypedField() {
        doReturn(true).when(this.mappedFieldMetadata).isPropertyType();
    }

    private <T> void withPropertyValue(T value) {
        Class<?> type = value == null ? this.mappedFieldMetadata.getType() : value.getClass();
        // primitive types are boxed before retrieval from the value map.
        Class<?> retrievedType = primitiveToWrapper(type);
        doReturn(value).when(this.valueMap).get(eq("field"), eq(retrievedType));
    }

    private <T> void withField(Class<T> fieldType) throws NoSuchFieldException {
        this.mappedFieldMetadata = mock(MappedFieldMetaData.class);
        Field field = getClass().getDeclaredField("mappedField");
        field.setAccessible(true);
        doReturn(field).when(this.mappedFieldMetadata).getField();
        doReturn("field").when(this.mappedFieldMetadata).getPath();
        doReturn(fieldType).when(this.mappedFieldMetadata).getType();
    }

    private void mapField() {
        this.testee = new FieldValueMappingCallback(this.model, this.resource, this.factory);
        this.testee.doWith(this.mappedFieldMetadata);
    }

    private void withThisReferenceTypedField() {
        doReturn(true).when(this.mappedFieldMetadata).isThisReference();
    }

    private void assertMappedFieldIsCollectionWithResourcesWithPaths(String... referencedResources) {
        assertThat(this.mappedField).isNotNull();
        @SuppressWarnings("unchecked")
        Collection<Resource> resources = (Collection<Resource>) this.mappedField;
        assertArrayHoldsResourcesWithPaths(resources.toArray(new Resource[resources.size()]), referencedResources);
    }

    private void assertArrayHoldsResourcesWithPaths(Resource[] array, String... resourcePaths) {
        assertThat(array).hasSize(resourcePaths.length);

        for (int i = 0; i < resourcePaths.length; ++i) {
            assertThat(array[i]).isNotNull();
            assertThat(array[i].getPath()).isEqualTo(resourcePaths[i]);
        }
    }

    private void assertFieldMapperDoesNotAttemptToResolvePlaceholders() {
        verify(this.factory, never()).resolveEmbeddedValue(anyString());
    }

    private void assertChildResourceIsNotLoadedForField() {
        verify(this.resource, never()).getChild(eq("field"));
    }

    private void assertFieldIsMapped() {
        assertMappedFieldIs(this.targetValue);
    }

    private void assertMappedFieldIs(Object value) {
        assertThat(this.mappedField).isEqualTo(value);
    }

    private void assertMappedFieldIsNull() {
        assertThat(this.mappedField).isNull();
    }

    private void assertFieldIsFetchedFromValueMap() {
        String fieldPath = this.mappedFieldMetadata.getPath();
        verify(this.valueMap).get(eq(fieldPath), eq(String.class));
    }

    private void assertFieldIsNotFetchedFromValueMap() {
        String fieldPath = this.mappedFieldMetadata.getPath();
        verify(this.valueMap, never()).get(eq(fieldPath), eq(String.class));
    }

    private void assertFieldIsFetchedFromValueMapAs(Class<?> expectedPropertyType) {
        String fieldPath = this.mappedFieldMetadata.getPath();
        verify(this.valueMap).get(fieldPath, expectedPropertyType);
    }

    private void assertFieldMapperLoadsFromValueMap(String key) {
        verify(this.valueMap).get(eq(key), eq(String.class));
    }

    private void assertFieldMapperAttemptsToResolvePlaceholdersIn(String placeholder) {
        verify(this.factory).resolveEmbeddedValue(eq(placeholder));
    }
}
