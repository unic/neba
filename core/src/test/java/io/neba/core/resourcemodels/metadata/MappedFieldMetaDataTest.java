/*
  Copyright 2013 the original author or authors.

  Licensed under the Apache License, Version 2.0 the "License";
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
*/

package io.neba.core.resourcemodels.metadata;

import io.neba.core.resourcemodels.mapping.testmodels.OtherTestResourceModel;
import io.neba.core.resourcemodels.mapping.testmodels.TestResourceModel;
import io.neba.core.resourcemodels.mapping.testmodels.TestResourceModelWithInvalidGenericFieldDeclaration;
import io.neba.core.resourcemodels.mapping.testmodels.TestResourceModelWithInvalidPathDeclaration;
import io.neba.core.resourcemodels.mapping.testmodels.TestResourceModelWithUnsupportedCollectionTypes;
import org.apache.sling.api.resource.Resource;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.List;

import static io.neba.core.util.ReflectionUtil.findField;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Olaf Otto
 */
public class MappedFieldMetaDataTest {
	private Class<?> modelType = TestResourceModel.class;

	private MappedFieldMetaData testee;

    @Test(expected = IllegalArgumentException.class)
    public void testHandlingOfNullField() {
        createMetadataForTestModelFieldWithName("this.field.does.not.exist");
    }

    @Test
	public void testPathAnnotationUsageOnPropertyField() {
		createMetadataForTestModelFieldWithName("stringFieldWithRelativePathAnnotation");
		assertFieldHasPathAnnotation();
		assertFieldIsPropertyType();

		assertFieldIsNotCollectionType();
		assertFieldIsNotReference();
		assertFieldIsNotThisReference();
	}

	@Test
	public void testPathAnnotationOnComplexReferenceField() {
		createMetadataForTestModelFieldWithName("referencedResource");
		assertFieldHasPathAnnotation();
		assertFieldIsReference();
		assertFieldIsPropertyType();

		assertFieldIsNotCollectionType();
		assertFieldIsNotThisReference();
    }

    @Test
    public void testPathAnnotationOnComplexReferenceFieldWithMetaAnnotation() {
        createMetadataForTestModelFieldWithName("referencedResourceWithMetaAnnotation");
        assertFieldHasPathAnnotation();
        assertFieldIsReference();
        assertFieldIsPropertyType();

        assertFieldIsNotCollectionType();
        assertFieldIsNotThisReference();
    }

    @Test
    public void testAppendAbsolutePathOnReference() {
        createMetadataForTestModelFieldWithName("referencedResourceModelWithAbsoluteAppendedReferencePath");
        assertReferenceHasAppendPath();
        assertReferenceAppendPathIs("/jcr:content");
    }

    @Test
    public void testAppendRelativePathOnReference() {
        createMetadataForTestModelFieldWithName("referencedResourceModelWithRelativeAppendedReferencePath");
        assertReferenceHasAppendPath();
        assertReferenceAppendPathIs("/jcr:content");
    }

    @Test
    public void testNoAppendPathOnReference() {
        createMetadataForTestModelFieldWithName("referencedResource");
        assertNoAppendPathIsPresentOnReference();
    }

    @Test
    public void testResolveBelowEveryChildOnChildren() {
        createMetadataForTestModelFieldWithName("childContentResourcesAsResources");
        assertChildrenHasResolveBelowEveryChildPath();
        assertChildrenResolveBelowEveryChildPathIs("jcr:content");
    }

    @Test
    public void testNoResolveBelowEveryChildOnChildren() {
        createMetadataForTestModelFieldWithName("childrenAsResources");
        assertChildrenDoesNotHaveResolveBelowEveryChildPath();
    }

    @Test
	public void testTypeParameterDetection() {
		createMetadataForTestModelFieldWithName("referencedResourcesListWithSimpleTypeParameter");

		assertFieldIsPropertyType();
		assertFieldIsCollectionType();
		assertTypeParameterIs(Resource.class);
	}

	@Test
	public void testThisReferenceDetection() {
		createMetadataForTestModelFieldWithName("thisResource");
		assertFieldIsThisReference();

		assertFieldIsNotPropertyType();
		assertFieldIsNotCollectionType();
		assertFieldIsNotReference();
		assertFieldHasNoPathAnnotation();
	}

    @Test
    public void testThisReferenceDetectionWithMetaAnnotation() {
        createMetadataForTestModelFieldWithName("thisResourceWithMetaAnnotation");
        assertFieldIsThisReference();

        assertFieldIsNotPropertyType();
        assertFieldIsNotCollectionType();
        assertFieldIsNotReference();
        assertFieldHasNoPathAnnotation();
    }

    @Test
	public void testPathRetrievalFromPathAnnotation() {
		createMetadataForTestModelFieldWithName("stringFieldWithAbsolutePathAnnotation");

		assertFieldHasPathAnnotation();
		assertFieldHasPath("/absolute/path");
	}

    @Test
    public void testPathRetrievalFromPathMetaAnnotation() {
        createMetadataForTestModelFieldWithName("stringFieldWithPathMetaAnnotation");

        assertFieldHasPathAnnotation();
        assertFieldHasPath("/absolute/path");
    }

    @Test(expected = IllegalArgumentException.class)
	public void testTreatmentOfInvalidGenericsUsage() {
		withModelType(TestResourceModelWithInvalidGenericFieldDeclaration.class);
		createMetadataForTestModelFieldWithName("readOnlyList");
	}
	
    @Test(expected = IllegalArgumentException.class)
    public void testTreatmentOfInvalidPathAnnotation() {
    	withModelType(TestResourceModelWithInvalidPathDeclaration.class);
    	createMetadataForTestModelFieldWithName("fieldWithInvalidPathAnnotation");
    }

    @Test(expected = IllegalArgumentException.class)
	public void testTreatmentOfUnsupportedCollectionTypes() {
    	withModelType(TestResourceModelWithUnsupportedCollectionTypes.class);
    	createMetadataForTestModelFieldWithName("hashSetField");
	}

    @Test
    public void testChildrenAsResources() {
        createMetadataForTestModelFieldWithName("childrenAsResources");
        assertThat(this.testee.isChildrenAnnotationPresent()).isTrue();
    }

    @Test
    public void testChildrenAsResourcesWithMetaAnnotation() {
        createMetadataForTestModelFieldWithName("childrenAsResourcesWithMetaAnnotation");
        assertThat(this.testee.isChildrenAnnotationPresent()).isTrue();
    }

    @Test(expected =  IllegalArgumentException.class)
    public void testChildrenAnnotationOnInvalidFieldType() {
        withModelType(TestResourceModelWithUnsupportedCollectionTypes.class);
        createMetadataForTestModelFieldWithName("hashMapField");
    }

    @Test
	public void testDetectionOfPropertyTypedFields() {
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
    public void testDetectionOfCollectionTypedPropertyFields() {
        createMetadataForTestModelFieldWithName("collectionOfStrings");
        assertFieldIsPropertyType();
        assertFieldIsCollectionType();
        assertFieldIsInstantiableCollectionType();
    }

    @Test
    public void testDetectionOfPathExpression() {
        createMetadataForTestModelFieldWithName("stringFieldWithPlaceholder");
        assertFieldHasPathVariables();
    }

    @Test
    public void testDetectionOfLazyFieldWithReferenceAnnotation() {
        createMetadataForTestModelFieldWithName("lazyReferenceToOtherModel");
        assertLazyFieldIsDetected();
        assertFieldTypeIs(OtherTestResourceModel.class);
    }

    @Test
    public void testDetectionOfLazyFieldWithPointingToChildResource() {
        createMetadataForTestModelFieldWithName("lazyReferenceToChildAsOtherModel");
        assertLazyFieldIsDetected();
        assertFieldTypeIs(OtherTestResourceModel.class);
    }

    @Test
    public void testProvisioningOfLazyLoadingFactoryForChildrenCollection() {
        createMetadataForTestModelFieldWithName("childrenAsResources");
        assertLazyLoadingCollectionFactoryIsCreated();
    }

    @Test
    public void testProvisioningOfLazyLoadingFactoryForReferenceCollection() {
        createMetadataForTestModelFieldWithName("referencedResourcesListWithSimpleTypeParameter");
        assertLazyLoadingCollectionFactoryIsCreated();
    }

    @Test
    public void testResolutionOfArrayComponentType() {
        createMetadataForTestModelFieldWithName("collectionOfStrings");
        assertArrayTypeOfComponentTypeIs(String[].class);
    }

    @Test
    public void testLazyFieldsAreTransparentlyTreatedLikeTheirTargetType() {
        createMetadataForTestModelFieldWithName("lazyChildContentResourcesAsResources");
        assertFieldIsInstantiableCollectionType();
        assertFieldTypeIs(List.class);
        assertTypeParameterIs(Resource.class);
    }

    @Test
    public void testMetadataMakesFieldAccessible() throws Exception {
        TestResourceModel testResourceModel = new TestResourceModel();
        createMetadataForTestModelFieldWithName("stringField");
        this.testee.getField().set(testResourceModel, "JunitTest");
        assertThat(testResourceModel.getStringField()).isEqualTo("JunitTest");
    }

    private void assertFieldTypeIs(Class<?> type) {
        assertThat(this.testee.getType()).isEqualTo(type);
    }

    private void assertLazyLoadingCollectionFactoryIsCreated() {
        assertThat(this.testee.getCollectionProxyFactory()).isNotNull();
    }

    private void assertLazyFieldIsDetected() {
        assertThat(this.testee.isLazy()).isTrue();
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

    private void assertFieldHasPathVariables() {
        assertThat(this.testee.getPath().hasPlaceholders()).isTrue();
    }

    private void assertFieldIsInstantiableCollectionType() {
        assertThat(this.testee.isInstantiableCollectionType()).isTrue();
    }

    private void assertFieldHasPath(String path) {
		assertThat(this.testee.getPath().toString()).isEqualTo(path);
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

	private void createMetadataForTestModelFieldWithName(String fieldName) {
        Field field = findField(this.modelType, fieldName);
        this.testee = new MappedFieldMetaData(field, this.modelType);
	}

	private void withModelType(Class<?> type) {
		modelType = type;
	}
}
