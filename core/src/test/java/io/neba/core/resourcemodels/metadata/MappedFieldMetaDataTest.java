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

import io.neba.api.resourcemodels.fieldprocessor.CustomFieldProcessor;
import io.neba.core.resourcemodels.mapping.testmodels.OtherTestResourceModel;
import io.neba.core.resourcemodels.mapping.testmodels.TestResourceModel;
import io.neba.core.resourcemodels.mapping.testmodels.TestResourceModelWithInvalidGenericFieldDeclaration;
import io.neba.core.resourcemodels.mapping.testmodels.TestResourceModelWithInvalidPathDeclaration;
import io.neba.core.resourcemodels.mapping.testmodels.TestResourceModelWithUnsupportedCollectionTypes;
import org.apache.sling.api.resource.Resource;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.util.ReflectionUtils.findField;
import static org.springframework.util.ReflectionUtils.setField;

/**
 * @author Olaf Otto
 */
public class MappedFieldMetaDataTest {
	private Class<?> modelType = TestResourceModel.class;

	private MappedFieldMetaData testee;
    private List<CustomFieldProcessor> customFieldProcessors = new LinkedList<CustomFieldProcessor>();

    @Test(expected = IllegalArgumentException.class)
    public void testHandlingOfNullField() throws Exception {
        createMetadataForTestModelFieldWithName("this.field.does.not.exist");
    }

    @Test
	public void testPathAnnotationUsageOnPropertyField() throws Exception {
		createMetadataForTestModelFieldWithName("stringFieldWithRelativePathAnnotation");
		assertFieldHasPathAnnotation();
		assertFieldIsPropertyType();

		assertFieldIsNotCollectionType();
		assertFieldIsNotReference();
		assertFieldIsNotThisReference();
	}

	@Test
	public void testPathAnnotationOnComplexReferenceField() throws Exception {
		createMetadataForTestModelFieldWithName("referencedResource");
		assertFieldHasPathAnnotation();
		assertFieldIsReference();
		assertFieldIsPropertyType();

		assertFieldIsNotCollectionType();
		assertFieldIsNotThisReference();
    }

    @Test
    public void testAppendRelativePathOnReference() throws Exception {
        createMetadataForTestModelFieldWithName("referencedResourceModelWithAppendedReferencePath");
        assertReferenceHasAppendPath();
        assertReferenceAppendPathIs("/jcr:content");
    }

    @Test
    public void testNoAppendPathOnReference() throws Exception {
        createMetadataForTestModelFieldWithName("referencedResource");
        assertNoAppendPathIsPresentOnReference();
    }

    @Test
    public void testResolveBelowEveryChildOnChildren() throws Exception {
        createMetadataForTestModelFieldWithName("childContentResourcesAsResources");
        assertChildrenHasResolveBelowEveryChildPath();
        assertChildrenResolveBelowEveryChildPathIs("jcr:content");
    }

    @Test
    public void testNoResolveBelowEveryChildOnChildren() throws Exception {
        createMetadataForTestModelFieldWithName("childrenAsResources");
        assertChildrenDoesNotHaveResolveBelowEveryChildPath();
    }

    @Test
	public void testTypeParameterDetection() throws Exception {
		createMetadataForTestModelFieldWithName("referencedResourcesListWithSimpleTypeParameter");

		assertFieldIsPropertyType();
		assertFieldIsCollectionType();
		assertTypeParameterIs(Resource.class);
	}

	@Test
	public void testThisReferenceDetection() throws Exception {
		createMetadataForTestModelFieldWithName("resource");
		assertFieldIsThisReference();

		assertFieldIsNotPropertyType();
		assertFieldIsNotCollectionType();
		assertFieldIsNotReference();
		assertFieldHasNoPathAnnotation();
	}

	@Test
	public void testPathRetrievalFromPathAnnotation() throws Exception {
		createMetadataForTestModelFieldWithName("stringFieldWithAbsolutePathAnnotation");

		assertFieldHasPathAnnotation();
		assertFieldHasPath("/absolute/path");
	}

	@Test(expected = IllegalArgumentException.class)
	public void testTreatmentOfInvalidGenericsUsage() throws Exception {
		withModelType(TestResourceModelWithInvalidGenericFieldDeclaration.class);
		createMetadataForTestModelFieldWithName("readOnlyList");
	}
	
    @Test(expected = IllegalArgumentException.class)
    public void testTreatmentOfInvalidPathAnnotation() throws Exception {
    	withModelType(TestResourceModelWithInvalidPathDeclaration.class);
    	createMetadataForTestModelFieldWithName("fieldWithInvalidPathAnnotation");
    }

    @Test(expected = IllegalArgumentException.class)
	public void testTreatmentOfUnsupportedCollectionTypes() throws Exception {
    	withModelType(TestResourceModelWithUnsupportedCollectionTypes.class);
    	createMetadataForTestModelFieldWithName("hashSetField");
	}

    @Test
    public void testChildrenAsResources() throws Exception {
        createMetadataForTestModelFieldWithName("childrenAsResources");
        assertThat(this.testee.isChildrenAnnotationPresent()).isTrue();
    }

    @Test(expected =  IllegalArgumentException.class)
    public void testChildrenAnnotationOnInvalidFieldType() {
        withModelType(TestResourceModelWithUnsupportedCollectionTypes.class);
        createMetadataForTestModelFieldWithName("hashMapField");
    }

    @Test
	public void testDetectionOfPropertyTypedFields() throws Exception {
		createMetadataForTestModelFieldWithName("stringField");
		assertFieldIsPropertyType();
		
		createMetadataForTestModelFieldWithName("primitiveIntField");
		assertFieldIsPropertyType();
		
		createMetadataForTestModelFieldWithName("primitiveBooleanField");
		assertFieldIsPropertyType();
		
		createMetadataForTestModelFieldWithName("primitiveLongField");
		assertFieldIsPropertyType();
		
		createMetadataForTestModelFieldWithName("primitiveFloatField");
		assertFieldIsPropertyType();
		
		createMetadataForTestModelFieldWithName("primitiveDoubleField");
		assertFieldIsPropertyType();
		
		createMetadataForTestModelFieldWithName("primitiveShortField");
		assertFieldIsPropertyType();
		
		createMetadataForTestModelFieldWithName("dateField");
		assertFieldIsPropertyType();
		
		createMetadataForTestModelFieldWithName("calendarField");
		assertFieldIsPropertyType();
	}

    @Test
    public void testDetectionOfCollectionTypedPropertyFields() throws Exception {
        createMetadataForTestModelFieldWithName("collectionOfStrings");
        assertFieldIsPropertyType();
        assertFieldIsCollectionType();
        assertFieldIsInstantiableCollectionType();
    }

    @Test
    public void testDetectionOfPathExpression() throws Exception {
        createMetadataForTestModelFieldWithName("stringFieldWithPlaceholder");
        assertFieldHasPathExpression();
    }

    @Test
    public void testDetectionOfOptionalFieldWithReferenceAnnotation() throws Exception {
        createMetadataForTestModelFieldWithName("lazyReferenceToOtherModel");
        assertOptionalFieldIsDetected();
        assertFieldTypeIs(OtherTestResourceModel.class);
    }

    @Test
    public void testDetectionOfOptionalFieldWithPointingToChildResource() throws Exception {
        createMetadataForTestModelFieldWithName("lazyReferenceToChildAsOtherModel");
        assertOptionalFieldIsDetected();
        assertFieldTypeIs(OtherTestResourceModel.class);
    }

    @Test
    public void testProvisioningOfLazyLoadingFactoryForChildrenCollection() throws Exception {
        createMetadataForTestModelFieldWithName("childrenAsResources");
        assertLazyLoadingCollectionFactoryIsCreated();
    }

    @Test
    public void testProvisioningOfLazyLoadingFactoryForReferenceCollection() throws Exception {
        createMetadataForTestModelFieldWithName("referencedResourcesListWithSimpleTypeParameter");
        assertLazyLoadingCollectionFactoryIsCreated();
    }

    @Test
    public void testResolutionOfArrayComponentType() throws Exception {
        createMetadataForTestModelFieldWithName("collectionOfStrings");
        assertArrayTypeOfComponentTypeIs(String[].class);
    }

    @Test
    public void testOptionalFieldsAreTransparentlyTreatedLikeTheirTargetType() throws Exception {
        createMetadataForTestModelFieldWithName("optionalChildContentResourcesAsResources");
        assertFieldIsInstantiableCollectionType();
        assertFieldTypeIs(List.class);
        assertTypeParameterIs(Resource.class);
    }

    @Test
    public void testMetadataMakesFieldAccessible() throws Exception {
        TestResourceModel testResourceModel = new TestResourceModel();
        createMetadataForTestModelFieldWithName("stringField");
        setField(this.testee.getField(), testResourceModel, "JunitTest");
        assertThat(testResourceModel.getStringField()).isEqualTo("JunitTest");
    }

    @Test
    public void testCustomFieldProcessor() throws Exception {
        withFieldProcessorAcceptingAllFields();
        createMetadataForTestModelFieldWithName("stringField");
        assertFieldTypeIs(String.class);
        assertFieldHasCustomProcessor();
    }

    private void assertFieldTypeIs(Class<?> type) {
        assertThat(this.testee.getType()).isEqualTo(type);
    }

    private void assertLazyLoadingCollectionFactoryIsCreated() {
        assertThat(this.testee.getCollectionProxyFactory()).isNotNull();
    }

    private void assertOptionalFieldIsDetected() {
        assertThat(this.testee.isOptional()).isTrue();
    }

    private void assertChildrenHasResolveBelowEveryChildPath() {
        assertThat(this.testee.isResolveBelowEveryChildPathPresentOnChildren()).isTrue();
    }

    private void assertChildrenDoesNotHaveResolveBelowEveryChildPath() {
        assertThat(this.testee.isResolveBelowEveryChildPathPresentOnChildren()).isFalse();
    }

    private void assertChildrenResolveBelowEveryChildPathIs(String path) {
        assertThat(this.testee.getResolveBelowEveryChildPathOnChildren()).isEqualTo(path);
    }

    private void assertFieldHasPathExpression() {
        assertThat(this.testee.isPathExpressionPresent()).isTrue();
    }

    private void assertFieldIsInstantiableCollectionType() {
        assertThat(this.testee.isInstantiableCollectionType()).isTrue();
    }

    private void assertFieldHasPath(String path) {
		assertThat(this.testee.getPath()).isEqualTo(path);
	}

	private void assertFieldHasPathAnnotation() {
		assertThat(this.testee.isPathAnnotationPresent()).isTrue();
	}

	private void assertFieldHasNoPathAnnotation() {
		assertThat(this.testee.isPathAnnotationPresent()).isFalse();
	}

	private void assertFieldIsThisReference() {
		assertThat(this.testee.isThisReference()).isTrue();
	}

	private void assertFieldIsNotThisReference() {
		assertThat(this.testee.isThisReference()).isFalse();
	}

	private void assertTypeParameterIs(Class<?> type) {
		assertThat(this.testee.getTypeParameter()).isEqualTo(type);
	}

    private void assertArrayTypeOfComponentTypeIs(Class<?> type) {
        assertThat(this.testee.getArrayTypeOfTypeParameter()).isEqualTo(type);
    }

    private void assertFieldIsCollectionType() {
		assertThat(this.testee.isCollectionType()).isTrue();
	}

	private void assertFieldIsNotCollectionType() {
		assertThat(this.testee.isCollectionType()).isFalse();
	}

	private void assertFieldIsReference() {
		assertThat(this.testee.isReference()).isTrue();
	}

	private void assertFieldIsNotReference() {
		assertThat(this.testee.isReference()).isFalse();
	}

    private void assertNoAppendPathIsPresentOnReference() {
        assertThat(this.testee.isAppendPathPresentOnReference()).isFalse();
    }

    private void assertReferenceHasAppendPath() {
        assertThat(this.testee.isAppendPathPresentOnReference()).isTrue();
    }

    private void assertReferenceAppendPathIs(String path) {
        assertThat(this.testee.getAppendPathOnReference()).isEqualTo(path);
    }

    private void assertFieldIsPropertyType() {
		assertThat(this.testee.isPropertyType()).isTrue();
	}

	private void assertFieldIsNotPropertyType() {
		assertThat(this.testee.isPropertyType()).isFalse();
	}

    private void assertFieldHasCustomProcessor() {
        assertThat(this.testee.isCustomProcessorPresent()).isTrue();
        assertThat(this.testee.getCustomFieldProcessor()).isEqualTo(customFieldProcessors.get(0));
    }

	private void createMetadataForTestModelFieldWithName(String fieldName) {
		Field field = findField(this.modelType, fieldName);
		this.testee = new MappedFieldMetaData(field, this.modelType, this.customFieldProcessors);
	}

    private void withFieldProcessorAcceptingAllFields() {
        CustomFieldProcessor processor = mock(CustomFieldProcessor.class);
        when(processor.accept(any(Field.class), any())).thenReturn(true);
        this.customFieldProcessors.add(processor);
    }

	private void withModelType(Class<?> type) {
		modelType = type;
	}
}
