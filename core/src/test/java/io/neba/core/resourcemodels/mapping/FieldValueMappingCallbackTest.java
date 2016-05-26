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
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.cglib.proxy.Factory;
import org.springframework.cglib.proxy.LazyLoader;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.*;

import static io.neba.api.resourcemodels.AnnotatedFieldMapper.OngoingMapping;
import static io.neba.core.resourcemodels.mapping.AnnotatedFieldMappers.AnnotationMapping;
import static java.lang.Boolean.FALSE;
import static java.util.Collections.emptyList;
import static org.apache.commons.lang.ClassUtils.primitiveToWrapper;
import static org.apache.commons.lang.StringUtils.substringAfterLast;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.*;

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
    @Mock
    private MappedFieldMetaData mappedFieldMetadata;
    @Mock
    private Factory lazyLoadingCollectionFactory;
    @Mock
    private AnnotatedFieldMappers annotatedFieldMappers;
    @Mock
    private AnnotatedFieldMapper annotatedFieldMapper;

    private LazyLoader lazyLoadingCollectionCallback;

    private Resource resource;
    private Resource parentOfResourceTargetedByMapping;
    private Resource resourceTargetedByMapping;

    @SuppressWarnings("unused")
    private Object mappedFieldOfTypeObject;
    @SuppressWarnings("unused")
    private String mappedFieldOfTypeString;

    private Field mappedField;

    private Object targetValue;
    private Object model = this;

    private OngoingMapping ongoingMapping;

    @Before
    public void prepareMappedField() throws Exception {
        withmappedField("mappedFieldOfTypeObject");
    }

    @Before
    public void prepareTestResource() {
        withResource(mock(Resource.class));
    }

    @Before
    public void mockLazyLoadingCollectionFactory() {
        Answer<Object> loadImmediately = invocationOnMock -> {
            lazyLoadingCollectionCallback = (LazyLoader) invocationOnMock.getArguments()[0];
            return lazyLoadingCollectionCallback.loadObject();
        };
        doAnswer(loadImmediately).when(lazyLoadingCollectionFactory).newInstance(isA(LazyLoader.class));
        doReturn(lazyLoadingCollectionFactory).when(this.mappedFieldMetadata).getCollectionProxyFactory();
    }

    @Before
    public void prepareCustomFieldMappers() throws Exception {
        doReturn(emptyList()).when(this.annotatedFieldMappers).get(isA(MappedFieldMetaData.class));
    }

    /**
     * The factory must not accept null arguments to its constructor.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testHandlingOfNullModelInConstructor() throws Exception {
        new FieldValueMappingCallback(null, this.resource, this.factory, this.annotatedFieldMappers);
    }

    /**
     * The factory must not accept null arguments to its constructor.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testHandlingOfNullResourceInConstructor() throws Exception {
        new FieldValueMappingCallback(this.model, null, this.factory, this.annotatedFieldMappers);
    }

    /**
     * The factory must not accept null arguments to its constructor.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testHandlingOfNullFactoryInConstructor() throws Exception {
        new FieldValueMappingCallback(this.model, this.resource, null, this.annotatedFieldMappers);
    }

    /**
     * The factory must not accept null arguments to its callback method.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testHandlingOfNullFactoryInMapping() throws Exception {
        new FieldValueMappingCallback(this.model, this.resource, this.factory, this.annotatedFieldMappers).doWith(null);
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
        assertMappedFieldValueIs(this.resourceTargetedByMapping);
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
        assertMappedFieldValueIsNull();
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
        withResourceTargetedByMapping(child("field"));
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
        withResourceTargetedByMapping(child("field"));
        mapChildResourceField(Resource.class);
        assertMappedFieldValueIs(this.resourceTargetedByMapping);
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
        mapSingleReferenceField(Resource.class, "/path/stored/in/property");
        assertMappedFieldValueIs(this.resourceTargetedByMapping);
    }

    /**
     * A model may declare lazy 1:1 relationships to other models using the
     * {@link io.neba.api.resourcemodels.Optional} interface. An implementation
     * of this interface must be provided automatically and must load the
     * target value when requested. Example:
     * <p/>
     * <pre>
     *     &#64;{@link io.neba.api.annotations.ResourceModel}(types = ...)
     *     public class MyModel {
     *         &#64;{@link io.neba.api.annotations.Reference}
     *         private {@link io.neba.api.resourcemodels.Optional}&lt;Resource&gt; link;
     *     }
     * </pre>
     */
    @Test
    public void testLazyLoadingReferenceResolution() throws Exception {
        withResourceTargetedByMapping("/path/stored/in/property");
        withOptionalField();
        mapSingleReferenceField(Resource.class, "/path/stored/in/property");
        assertOptionalFieldHasValue(this.resourceTargetedByMapping);
    }

    /**
     * When an {@link Optional} reference points to a non-existing target (i.e. a null value),
     * invoking {@link io.neba.api.resourcemodels.Optional#get()} must result in a
     * {@link java.util.NoSuchElementException}, as only {@link io.neba.api.resourcemodels.Optional#orElse(Object)}
     * is null-safe.
     */
    @Test(expected = NoSuchElementException.class)
    public void testHandlingOfNullWhenNullIsNotAllowedInOptionalReference() throws Exception {
        withOptionalField();
        mapSingleReferenceField(Resource.class, "/path/stored/in/property");
        assertOptionalFieldHasValue(null);
        getOptionalValue();
    }

    /**
     * When an {@link Optional} reference points to a non-existing target (i.e. a null value),
     * invoking {@link io.neba.api.resourcemodels.Optional#isPresent()} must yield <code>false</code>.
     */
    @Test
    public void testIsPresentIsFalseWhenOptionalValueIsNull() throws Exception {
        withTypeParameter(Resource.class);
        withOptionalField();
        mapSingleReferenceField(Optional.class, "/path/stored/in/property");
        assertOptionalFieldHasValue(null);
        assertOptionalValueIsNotPresent();
    }

    /**
     * When an {@link Optional} reference points to an existing target,
     * invoking {@link io.neba.api.resourcemodels.Optional#isPresent()} must yield <code>true</code>.
     */
    @Test
    public void testIsPresentIsFalseWhenOptionalValueIsNotNull() throws Exception {
        withResourceTargetedByMapping("/path/stored/in/property");
        withOptionalField();
        mapSingleReferenceField(Resource.class, "/path/stored/in/property");
        assertOptionalFieldHasValue(this.resourceTargetedByMapping);
        assertOptionalValueIsPresent();
    }

    /**
     * A reference may be a property of a resource containing an array of paths to other resources.
     * Such a references are automatically resolved and adapted in the presence of a
     * {@link io.neba.api.annotations.Reference} annotation.
     * For example, the current resource may have a String[] property called "links", containing the values
     * "/first/path/stored/in/property", "/second/path/stored/in/property".
     * The corresponding resources are then resolved and injected.
     * Here, the reference is also declared {@link io.neba.api.resourcemodels.Optional}.
     * <p/>
     * <pre>
     *     &#64;{@link io.neba.api.annotations.ResourceModel}(types = ...)
     *     public class MyModel {
     *         &#64;{@link io.neba.api.annotations.Reference}
     *         private {@link io.neba.api.resourcemodels.Optional}&lt;Collection&lt;Resource&gt;&gt; links;
     *     }
     * </pre>
     */
    @Test
    public void testOptionalCollectionOfReferencesIsExclusivelyLazyLoadedViaOptional() throws Exception {
        String[] referencedResources = new String[]{"/first/path/stored/in/property", "/second/path/stored/in/property"};
        withMockResources(referencedResources);
        withOptionalField();
        mapReferenceCollectionField(Collection.class, Resource.class, referencedResources);
        assertMappedFieldValueIsOptional();
        loadOptionalField();
        assertMappedFieldValueIsCollectionWithResourcesWithPaths(referencedResources);
        assertNoLazyLoadingProxyIsCreated();
    }

    /**
     * Here, the above collection of references is not declared {@link io.neba.api.resourcemodels.Optional}.
     * Thus, it must be provided as a lazy loading proxy instead.
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
    public void testCollectionOfReferencesIsLazyLoadedViaProxy() throws Exception {
        String[] referencedResources = new String[]{"/first/path/stored/in/property", "/second/path/stored/in/property"};
        withMockResources(referencedResources);
        mapReferenceCollectionField(Collection.class, Resource.class, referencedResources);
        assertMappedFieldValueIsCollectionWithResourcesWithPaths(referencedResources);
        assertLazyLoadingProxyIsCreated();
    }

    /**
     * Test the explicitly lazy retrieval of the children of the current resources with adaptation to
     * the desired target type (component type of the collection).
     * <p/>
     * <pre>
     *     &#64;{@link io.neba.api.annotations.ResourceModel}(types = ...)
     *     public class MyModel {
     *         &#64;{@link io.neba.api.annotations.Children}
     *         private {@link io.neba.api.resourcemodels.Optional}&lt;List&lt;ModelForChild&gt;&gt; children;
     *     }
     * </pre>
     */
    @Test
    public void testOptionalCollectionOfChildrenIsExclusivelyLazyLoadedViaOptional() throws Exception {
        withField(Collection.class);
        withOptionalField();
        withCollectionTypedField();
        withInstantiableCollectionTypedField();
        withTypeParameter(TestResourceModel.class);
        withChildrenAnnotationPresent();
        withResourceTargetedByMapping(child("field"));
        withResourceTargetedByMappingAdaptingTo(TestResourceModel.class, new TestResourceModel());

        mapField();

        assertMappedFieldValueIsOptional();
        loadOptionalField();
        assertMappedFieldValueIsCollectionContainingTargetValue();
    }

    /**
     * Tests that {@link AnnotatedFieldMapper annotated field mappers} are supported on
     * {@link Optional lazy-oding} resource model fields, i.e. that these mappers are invoked when the
     * lazy loading callback is triggered in a case such as this:
     * <p />
     * <pre>
     *     &#64;{@link io.neba.api.annotations.ResourceModel}(types = ...)
     *     public class MyModel {
     *         &#64;MyCustomAnnotation
     *         private {@link io.neba.api.resourcemodels.Optional}&lt;AnyType&gt; anyField;
     *     }
     * </pre>
     */
    @Test
    public void testCustomMappersAreAppliedWhenOptionalFieldsAreLoaded() throws Exception {
        withField(Collection.class);
        withOptionalField();
        withCollectionTypedField();
        withInstantiableCollectionTypedField();
        withCustomFieldMapperMappingTo(new ArrayList<>());

        mapField();

        assertMappedFieldValueIsOptional();

        assertCustomFieldMapperIsNotObtained();
        assertCustomFieldMapperIsNotUsedToMapField();

        loadOptionalField();

        assertCustomFieldMapperIsObtained();
        assertCustomFieldMapperIsUsedToMapField();
    }

    /**
     * Test the implicitly lazy retrieval of the children of the current resources with adaptation to
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
    public void testCollectionOfChildrenIsLazyLoadedViaProxy() throws Exception {
        withField(Collection.class);
        withCollectionTypedField();
        withInstantiableCollectionTypedField();
        withTypeParameter(TestResourceModel.class);
        withChildrenAnnotationPresent();
        withResourceTargetedByMapping(child("field"));
        withResourceTargetedByMappingAdaptingTo(TestResourceModel.class, new TestResourceModel());

        mapField();

        assertMappedFieldValueIsCollectionContainingTargetValue();
        assertLazyLoadingProxyIsCreated();
    }

    /**
     * A {@link io.neba.api.annotations.Reference} may specify an additional
     * {@link io.neba.api.annotations.Reference#append() relative path} that is appended to the reference path(s)
     * prior to resolution. This way, a resource model can directly use children or parents of referenced resources
     * without further programmatic steps, for instance like so:
     * <p/>
     * <pre>
     *     &#64;{@link io.neba.api.annotations.ResourceModel}(types = ...)
     *     public class MyModel {
     *         &#64;{@link io.neba.api.annotations.Reference}(append = "/jcr:content")
     *         &#64;{@link io.neba.api.annotations.Path}("page")
     *         private ValueMap pageContent;
     *     }
     * </pre>
     */
    @Test
    public void testReferenceResolutionWithAppendedRelativePath() throws Exception {
        withResourceTargetedByMapping("/content/resource/child");
        withAppendReferenceAppendPath("/child");
        mapSingleReferenceField(Resource.class, "/content/resource");
        assertMappedFieldValueIs(this.resourceTargetedByMapping);
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
        mapReferenceCollectionField(Collection.class, Resource.class, referencedResources);

        assertMappedFieldValueIsCollectionWithResourcesWithPaths(referencedResources);
    }

    /**
     * Same as {@link #testCollectionOfReferencesResolution()}, but using a {@link java.util.Set} instead
     * of a collection of references.
     */
    @Test
    public void testSetOfReferencesResolution() throws Exception {
        String[] referencedResources = new String[]{"/first/path/stored/in/property", "/second/path/stored/in/property"};

        withMockResources(referencedResources);
        mapReferenceCollectionField(Set.class, Resource.class, referencedResources);

        assertMappedFieldValueIsCollectionWithResourcesWithPaths(referencedResources);
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
        mapReferenceCollectionField(Set.class, Resource.class, referencedResources);

        assertMappedFieldValueIsCollectionWithResourcesWithPaths(referencedResources[0]);
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
        withResourceTargetedByMapping(child("field"));
        mapChildResourceField(Resource.class);
        assertMappedFieldValueIs(this.resourceTargetedByMapping);
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
        withTypeParameter(Resource.class);
        withChildrenAnnotationPresent();
        withResourceTargetedByMapping(child("field"));

        mapField();

        assertMappedFieldValueIsCollectionWithResourcesWithPaths(resourceTargetedByMapping.getPath());
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
        withTypeParameter(TestResourceModel.class);
        withChildrenAnnotationPresent();
        withResourceTargetedByMapping(child("field"));
        withResourceTargetedByMappingAdaptingTo(TestResourceModel.class, new TestResourceModel());

        mapField();

        assertMappedFieldValueIsCollectionContainingTargetValue();
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
        withTypeParameter(TestResourceModel.class);
        withPathAnnotationPresent();
        withChildrenAnnotationPresent();
        withResourceTargetedByMappingAdaptingTo(TestResourceModel.class, new TestResourceModel());

        mapField();

        assertMappedFieldValueIsCollectionContainingTargetValue();
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
        withTypeParameter(TestResourceModel.class);
        withPathAnnotationPresent();
        withReferenceAnnotationPresent();
        withPropertyValue("/referenced/path");
        withChildrenAnnotationPresent();
        withResourceTargetedByMappingAdaptingTo(TestResourceModel.class, new TestResourceModel());

        mapField();

        assertFieldIsFetchedFromValueMap();
        assertMappedFieldValueIsCollectionContainingTargetValue();
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
        withTypeParameter(TestResourceModel.class);
        withReferenceAnnotationPresent();
        withPropertyValue("/referenced/path");
        withChildrenAnnotationPresent();
        withResourceTargetedByMappingAdaptingTo(TestResourceModel.class, new TestResourceModel());

        mapField();

        assertFieldIsFetchedFromValueMap();
        assertMappedFieldValueIsCollectionContainingTargetValue();
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
        withTypeParameter(TestResourceModel.class);
        withChildrenAnnotationPresent();
        withResourceTargetedByMapping(child("field"));

        mapField();

        assertMappedFieldValueIsEmptyCollection();
    }

    /**
     * A child may not be retrieved directly, but the children colleciton may also contain children of the children
     * in case a {@link io.neba.api.annotations.Children#resolveBelowEveryChild()} path is specified, like so:
     * <p/>
     * <p>
     * <pre>
     *     &#64;{@link io.neba.api.annotations.ResourceModel}(types = ...)
     *     public class MyModel {
     *         &#64;{@link io.neba.api.annotations.Children}(resolveBelowEveryChild = "/jcr:content")
     *         private List&lt;ModelForChild&gt; link;
     *     }
     *  </pre>
     * </p>
     */
    @Test
    public void testChildrenWithResolveBelowEveryChildPath() throws Exception {
        withField(Collection.class);
        withCollectionTypedField();
        withInstantiableCollectionTypedField();
        withTypeParameter(Resource.class);
        withChildrenAnnotationPresent();
        withResolveBelowChildPathOnChildren("jcr:content");
        Resource content = child(
                "child",
                "jcr:content");
        mapField();
        assertMappedFieldValueIsCollectionWithEntries(content);
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
        withTypeParameter(String.class);
        withPropertyTypedField();
        withPropertyValue(propertyValues);

        mapField();

        assertFieldIsFetchedFromValueMapAs(String[].class);
        assertMappedFieldValueIsCollectionWithEntries((Object[]) propertyValues);
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
        withResourceTargetedByMapping(child("field"));

        mapField();

        assertMappedFieldValueIs(this.resourceTargetedByMapping);
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

        assertMappedFieldValueIsNull();
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

        assertMappedFieldValueIsNull();
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
        withTypeParameter(String.class);
        withPropertyTypedField();
        withPropertyValue(new String[]{"first value", "second value"});

        mapField();

        assertMappedFieldValueIsNull();
    }

    /**
     * NEBA guarantees that Collection-typed mappable fields are not null. This
     * shall hold true regardless of the field semantics.
     */
    @Test
    public void testPreventionOfNullValuesInReferenceCollectionFieldWithoutDefaultValue() throws Exception {
        withField(Collection.class);
        withInstantiableCollectionTypedField();
        withReferenceAnnotationPresent();

        mapField();

        assertMappedFieldValueIsEmptyCollection();
    }

    /**
     * NEBA guarantees that Collection-typed mappable fields are not null. This
     * shall hold true regardless of the field semantics.
     */
    @Test
    public void testPreventionOfNullValuesInMappableCollectionFieldWithoutDefaultValue() throws Exception {
        withField(Collection.class);
        withInstantiableCollectionTypedField();

        mapField();

        assertMappedFieldValueIsEmptyCollection();
    }

    /**
     * NEBA guarantees that Collection-typed mappable fields are not null. This shall not hold true for
     * {@link Optional} collection-typed fields, as those have an explicit {@link Optional#isPresent() empty state}. For example.
     * <p/>
     * <pre>
     *     &#64;{@link io.neba.api.annotations.ResourceModel}(types = ...)
     *     public class MyModel {
     *         &#64;some.Annotation
     *         private Optional&lt;List&lt;SomeModel&lt;&lt; optionalList;
     *     }
     * </pre>
     */
    @Test
    public void testNullValuesAreNotPreventedInOptionalCollectionTypedFields() throws Exception {
        withField(Collection.class);
        withOptionalField();
        withInstantiableCollectionTypedField();

        mapField();

        assertMappedFieldValueIsOptional();
        assertOptionalValueIsNotPresent();
    }

    /**
     * NEBA guarantees that Collection-typed mappable fields are not null. This
     * shall hold true regardless of the field semantics.
     */
    @Test
    public void testPreventionOfNullValuesInMappableCollectionFieldOfSyntheticResource() throws Exception {
        withField(Collection.class);
        withInstantiableCollectionTypedField();
        withNullValueMap();

        mapField();

        assertMappedFieldValueIsEmptyCollection();
    }

    /**
     * NEBA guarantees that Collection-typed mappable fields are not null. This
     * shall hold true regardless of the field semantics. However, if the field already has a non-null default
     * value, this value must not be overwritten.
     */
    @Test
    public void testDefaultValueOfMappableCollectionTypedFieldIsNotOverwritten() throws Exception {
        withField(Collection.class);
        withInstantiableCollectionTypedField();

        Collection<?> defaultValue = mock(Collection.class);
        withDefaultFieldValue(defaultValue);

        mapField();

        assertMappedFieldValueIs(defaultValue);
    }

    /**
     * NEBA guarantees that Collection-typed mappable fields are not null. This
     * shall hold true regardless of the field semantics. However, if the field already has a non-null default
     * value, this value must not be overwritten.
     */
    @Test
    public void testDefaultValueOfMappableCollectionTypedReferenceFieldIsNotOverwritten() throws Exception {
        withField(Collection.class);
        withInstantiableCollectionTypedField();
        withReferenceAnnotationPresent();

        Collection<?> defaultValue = mock(Collection.class);
        withDefaultFieldValue(defaultValue);

        mapField();

        assertMappedFieldValueIs(defaultValue);
    }

    /**
     * {@link io.neba.api.resourcemodels.AnnotatedFieldMapper Field mappers}
     * are applied to all field mappings after the field value was resolved by NEBA,
     * i.e. they may override the resolved value. This test scenario verifies that
     * a field mapper is applied and can override an already resolved value.
     */
    @Test
    public void testApplicationOfFieldMappersToResoledFieldValue() throws Exception {
        withCustomFieldMapperMappingTo("CustomMappedValue");

        mapPropertyField(String.class, "PropertyValue");

        assertMappedFieldValueIs("CustomMappedValue");
    }

    /**
     * {@link io.neba.api.resourcemodels.AnnotatedFieldMapper Field mappers} receive extensive
     * contextual mapping data, such as the current field, the value that was resolved for it,
     * the model, resource and so forth. This test verifies that this contextual data is correct.
     */
    @Test
    public void testOngoingMappingContainsAccurateMappingData() throws Exception {
        withCustomFieldMapperMappingTo("CustomMappedValue");

        mapPropertyField(String.class, "PropertyValue");

        assertOngoingMappingDataIsAccurate();
    }

    /**
     * To prevent implementations of field mappers from having to worry about instantiating
     * suitable collection types for collection-typed fields, NEBA extends its guarantee (mappable collection-typed
     * members are never null) to the {@link io.neba.api.resourcemodels.AnnotatedFieldMapper field mappers}.
     * This test verifies that field mappers do not receive null for such a collection, even if no value could be resolved.
     * Instead, they should receive an empty default value.
     */
    @Test
    public void testNullCollectionValuesAreSetToDefaultValueBeforeInvokingFieldMappers() throws Exception {
        withField(Collection.class);
        withInstantiableCollectionTypedField();
        withPropertyTypedField();
        withTypeParameter(String.class);

        withCustomFieldMapperMappingTo(new ArrayList<String>());

        mapField();
        assertOngoingMappingsResolvedValueIsNotNull();
    }

    /**
     * While NEBA guarantees non-null collections, no such guarantee exists for any other
     * field types. This test verifies that null values for non-collection typed fields
     * are passed to the {@link io.neba.api.resourcemodels.AnnotatedFieldMapper field mappers}.
     */
    @Test
    public void testNullNonCollectionValuesAreNullWhenInvokingFieldMappers() throws Exception {
        withCustomFieldMapperMappingTo("CustomMappedValue");
        mapPropertyField(String.class, null);
        assertOngoingMappingsResolvedValueIsNull();
    }

    /**
     * A {@link io.neba.api.resourcemodels.AnnotatedFieldMapper} implementation must take
     * care to return an assignment-compatible value as a mapping result. However,
     * there are no enforce this at compile time. This test verifies that a suitable exception
     * is thrown in case a field mapper returns an incompatible value at runtime.
     */
    @Test
    public void testHandlingOfIncompatibleReturnValueFromCustomFieldMapper() throws Exception {
        withCustomFieldMapperMappingTo(new ArrayList<String>());
        withmappedField("mappedFieldOfTypeString");

        Exception e = null;
        try {
            mapPropertyField(String.class, null);
        } catch (Exception ex) {
            e = ex;
        }

        assertThat(e)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Can not set java.lang.String field " +
                        "io.neba.core.resourcemodels.mapping.FieldValueMappingCallbackTest.mappedFieldOfTypeString " +
                        "to java.util.ArrayList");
    }

    private void assertOngoingMappingsResolvedValueIsNull() {
        assertThat(this.ongoingMapping.getResolvedValue()).isNull();
    }

    private void assertOngoingMappingsResolvedValueIsNotNull() {
        assertThat(this.ongoingMapping.getResolvedValue()).isNotNull();
    }

    private void assertOngoingMappingDataIsAccurate() {
        assertThat(this.ongoingMapping.getField()).isEqualTo(this.mappedField);
        assertThat(this.ongoingMapping.getRepositoryPath()).isEqualTo("field");
        assertThat(this.ongoingMapping.getFieldType()).isEqualTo(this.mappedFieldMetadata.getType());
        assertThat(this.ongoingMapping.getModel()).isEqualTo(this.model);
        assertThat(this.ongoingMapping.getProperties()).isNotNull();
        assertThat(this.ongoingMapping.getResolvedValue()).isEqualTo(this.targetValue);
        assertThat(this.ongoingMapping.getResource()).isSameAs(this.resource);
        assertThat(this.ongoingMapping.getFieldTypeParameter()).isSameAs(this.mappedFieldMetadata.getTypeParameter());
    }

    @SuppressWarnings("unchecked")
    private void withCustomFieldMapperMappingTo(final Object value) {
        AnnotationMapping mapping = mock(AnnotationMapping.class);
        doReturn(this.annotatedFieldMapper).when(mapping).getMapper();

        Collection<AnnotationMapping> mappings = new ArrayList<>();
        mappings.add(mapping);

        doReturn(mappings).when(this.annotatedFieldMappers).get(isA(MappedFieldMetaData.class));

        Answer retainMappingContext = invocationOnMock -> {
            ongoingMapping = (OngoingMapping) invocationOnMock.getArguments()[0];
            return value;
        };
        doAnswer(retainMappingContext).when(this.annotatedFieldMapper).map(isA(OngoingMapping.class));
    }

    private void withmappedField(String fieldName) throws NoSuchFieldException {
        this.mappedField = getClass().getDeclaredField(fieldName);
    }

    private void withDefaultFieldValue(Object value) {
        this.mappedFieldOfTypeObject = value;
    }

    private void withInstantiableCollectionTypedField() {
        doReturn(true).when(this.mappedFieldMetadata).isInstantiableCollectionType();
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

    @SuppressWarnings("unchecked")
	private <T> void withParentOfTargetResourceProperty(String propertyName, T propertyValue) {
        this.targetValue = propertyValue;
        ValueMap properties = mock(ValueMap.class);
        when(this.parentOfResourceTargetedByMapping.adaptTo(eq(ValueMap.class))).thenReturn(properties);
        when(properties.get(eq(propertyName), eq((Class<T>) propertyValue.getClass()))).thenReturn(propertyValue);
    }

    private void withChildrenAnnotationPresent() {
        doReturn(true).when(this.mappedFieldMetadata).isChildrenAnnotationPresent();
    }

    private void withResolveBelowChildPathOnChildren(String path) {
        doReturn(true).when(this.mappedFieldMetadata).isResolveBelowEveryChildPathPresentOnChildren();
        doReturn(path).when(this.mappedFieldMetadata).getResolveBelowEveryChildPathOnChildren();
    }

    private void mapPropertyField(Class<?> fieldType, Object propertyValue) throws NoSuchFieldException {
        withPropertyField(fieldType, propertyValue);
        mapField();
        this.targetValue = propertyValue;
    }

    private void mapSingleReferenceField(Class<?> fieldType, String referencePath) throws NoSuchFieldException {
        withPropertyField(fieldType, referencePath);
        withReferenceAnnotationPresent();
        mapField();
    }

    private void mapReferenceCollectionField(
    		@SuppressWarnings("rawtypes") Class<? extends Collection> collectionType,
    		Class<?> componentType, String[] referencePaths)
    				throws NoSuchFieldException {
        withPropertyField(collectionType, referencePaths);
        withTypeParameter(componentType);
        withReferenceAnnotationPresent();
        withInstantiableCollectionTypedField();
        withCollectionTypedField();
        mapField();
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

    private Resource withChildResource(Resource parent, String childName) {
        Resource child = mock(Resource.class);
        doReturn(child).when(parent).getChild(eq(childName));
        String path = parent.getPath() + "/" + childName;
        doReturn(path).when(child).getPath();
        when(this.resourceResolver.getResource(eq(parent), eq(childName))).thenReturn(child);

        @SuppressWarnings("unchecked")
        Iterator<Resource> ci = mock(Iterator.class);
        when(ci.hasNext()).thenReturn(true, false);
        when(ci.next()).thenReturn(child).thenThrow(new IllegalStateException());

        doReturn(ci).when(parent).listChildren();


        return child;
    }

    private Resource child(String... childHierarchy) {
        Resource currentParent = this.resource;
        for (String childName : childHierarchy) {
            currentParent = withChildResource(currentParent, childName);
        }
        return currentParent;
    }

    private void withOptionalField() {
        doReturn(true).when(this.mappedFieldMetadata).isOptional();
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

    private void withResourceTargetedByMapping(Resource resource) {
        this.resourceTargetedByMapping = resource;
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

    private void withTypeParameter(Class<?> parameter) {
        doReturn(parameter).when(this.mappedFieldMetadata).getTypeParameter();
        doReturn(Array.newInstance(parameter, 0).getClass()).when(this.mappedFieldMetadata).getArrayTypeOfTypeParameter();
    }

    private void withCollectionTypedField() {
        doReturn(true).when(this.mappedFieldMetadata).isCollectionType();
    }

    private void withPathAnnotationPresent() {
        doReturn(true).when(this.mappedFieldMetadata).isPathAnnotationPresent();
    }

    private void withFieldPath(String path) {
        doReturn(path).when(this.mappedFieldMetadata).getPath();
    }

    private void withReferenceAnnotationPresent() {
        doReturn(true).when(this.mappedFieldMetadata).isReference();
        doReturn(true).when(this.mappedFieldMetadata).isPropertyType();
    }

    private void withAppendReferenceAppendPath(String relativeAppendPath) {
        doReturn(true).when(this.mappedFieldMetadata).isAppendPathPresentOnReference();
        doReturn(relativeAppendPath).when(this.mappedFieldMetadata).getAppendPathOnReference();
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
        mappedField.setAccessible(true);
        doReturn(mappedField).when(this.mappedFieldMetadata).getField();
        doReturn("field").when(this.mappedFieldMetadata).getPath();
        doReturn(fieldType).when(this.mappedFieldMetadata).getType();
    }

    private void mapField() {
        new FieldValueMappingCallback(this.model, this.resource, this.factory, this.annotatedFieldMappers)
                .doWith(this.mappedFieldMetadata);
    }

    private void withThisReferenceTypedField() {
        doReturn(true).when(this.mappedFieldMetadata).isThisReference();
    }

    private void loadOptionalField() {
        this.mappedFieldOfTypeObject = ((Optional<?>) this.mappedFieldOfTypeObject).orElse(null);
    }

    private void assertMappedFieldValueIsOptional() {
        assertThat(this.mappedFieldOfTypeObject).isInstanceOf(Optional.class);
    }

    private void assertNoLazyLoadingProxyIsCreated() {
        verify(this.lazyLoadingCollectionFactory, never()).newInstance(isA(LazyLoader.class));
    }

    private void assertLazyLoadingProxyIsCreated() {
        verify(this.lazyLoadingCollectionFactory).newInstance(isA(LazyLoader.class));
    }

    private void assertOptionalFieldHasValue(Object expected) {
        assertThat(this.mappedFieldOfTypeObject).isInstanceOf(Optional.class);
        assertThat(((Optional<?>) this.mappedFieldOfTypeObject).orElse(null)).isEqualTo(expected);
    }

    private void assertOptionalValueIsPresent() {
        assertThat(((Optional<?>) this.mappedFieldOfTypeObject).isPresent()).describedAs("the optional field is present").isTrue();
    }

    private void assertOptionalValueIsNotPresent() {
        assertThat(((Optional<?>) this.mappedFieldOfTypeObject).isPresent()).describedAs("the optional field is present").isFalse();
    }

    private void getOptionalValue() {
        assertThat(this.mappedFieldOfTypeObject).isInstanceOf(Optional.class);
        ((Optional<?>) this.mappedFieldOfTypeObject).get();
    }

    private void assertMappedFieldValueIsCollectionWithResourcesWithPaths(String... referencedResources) {
        assertThat(this.mappedFieldOfTypeObject).isInstanceOf(Collection.class);
        @SuppressWarnings("unchecked")
        Collection<Resource> resources = (Collection<Resource>) this.mappedFieldOfTypeObject;
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
        assertMappedFieldValueIs(this.targetValue);
    }

    private void assertMappedFieldValueIs(Object value) {
        assertThat(this.mappedFieldOfTypeObject).isEqualTo(value);
    }

    private void assertMappedFieldValueIsNull() {
        assertThat(this.mappedFieldOfTypeObject).isNull();
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

    private void assertMappedFieldValueIsCollectionContainingTargetValue() {
        assertThat(this.mappedFieldOfTypeObject).isInstanceOf(Collection.class);
        assertThat((Collection<?>) this.mappedFieldOfTypeObject).containsOnly(this.targetValue);
    }

    private void assertMappedFieldValueIsEmptyCollection() {
        assertThat(this.mappedFieldOfTypeObject).isInstanceOf(Collection.class);
        assertThat((Collection<?>) this.mappedFieldOfTypeObject).isEmpty();
    }

    private void assertMappedFieldValueIsCollectionWithEntries(Object... entries) {
        assertThat(this.mappedFieldOfTypeObject).isInstanceOf(Collection.class);
        assertThat((Collection<?>) this.mappedFieldOfTypeObject).containsOnly(entries);
    }

    @SuppressWarnings("unchecked")
    private void assertCustomFieldMapperIsUsedToMapField() {
        verify(this.annotatedFieldMapper).map(eq(this.ongoingMapping));
    }

    private void assertCustomFieldMapperIsObtained() {
        verify(this.annotatedFieldMappers).get(eq(this.mappedFieldMetadata));
    }

    @SuppressWarnings("unchecked")
    private void assertCustomFieldMapperIsNotUsedToMapField() {
        verify(this.annotatedFieldMapper, never()).map(any());
    }

    private void assertCustomFieldMapperIsNotObtained() {
        verify(this.annotatedFieldMappers, never()).get(any());
    }
}