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

import io.neba.core.resourcemodels.mapping.testmodels.ExtendedTestResourceModel;
import io.neba.core.resourcemodels.mapping.testmodels.TestResourceModel;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.util.ReflectionUtils.findField;

/**
 * 
 * @author Olaf Otto
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class ResourceModelMetaDataTest {
	private Class<?> modelType;
	
	private ResourceModelMetaData testee;

	@Before
	public void prepare() {
		createMetadataFor(TestResourceModel.class);
	}
	
    @Test
    public void testStaticFieldsAreNotMappable() throws Exception {
    	assertMappableFieldsDoesNotContain("staticField");
    }

	@Test
    public void testFinalFieldsAreNotMappable() throws Exception {
		assertMappableFieldsDoesNotContain("finalField");
    }

    @Test
    public void testFieldsAnnotatedWithUnmappedAreNotMappable() throws Exception {
    	assertMappableFieldsDoesNotContain("unmappedStringField");
    }

	@Test
	public void testFieldsMetaAnnotatedWithUnmappedAreNotMappable() throws Exception {
		assertMappableFieldsDoesNotContain("unmappedStringFieldWithUnmappedMetaAnnotation");
	}


    @Test
    public void testFieldsAnnotatedWithAtInjectAreNotMappable() throws Exception {
    	assertMappableFieldsDoesNotContain("injectedField");
    }

	@Test
	public void testFieldsAnnotatedWithAutowiredAreNotMappable() throws Exception {
		assertMappableFieldsDoesNotContain("autowiredField");
	}

	@Test
	public void testFieldsAnnotatedWithAtResourceAreNotMappable() throws Exception {
		assertMappableFieldsDoesNotContain("resourceField");
	}

	@Test
    public void testMappableFieldsAreInherited() throws Exception {
    	createMetadataFor(ExtendedTestResourceModel.class);
    	assertMetadataEqualsMetadataOf(TestResourceModel.class);
    }

	@Test
	public void testToStringRepresentation() throws Exception {
		assertThat(this.testee.toString()).isEqualTo("ResourceModelMetaData[" + TestResourceModel.class.getName() + "]");
	}

	private void assertMetadataEqualsMetadataOf(Class<?> otherModel) {
		MappedFieldMetaData[] mappableFields = this.testee.getMappableFields();
        MethodMetaData[] preMappingMethods = this.testee.getPreMappingMethods();
		MethodMetaData[] postMappingMethods = this.testee.getPostMappingMethods();
		
		ResourceModelMetaData other = new ResourceModelMetaData(otherModel);
		
		Assertions.assertThat(mappableFields).isEqualTo(other.getMappableFields());
		Assertions.assertThat(postMappingMethods).isEqualTo(other.getPostMappingMethods());
        Assertions.assertThat(preMappingMethods).isEqualTo(other.getPreMappingMethods());
	}

	private void createMetadataFor(Class<?> modelType) {
		this.modelType = modelType;
		this.testee = new ResourceModelMetaData(modelType);
	}

	private void assertMappableFieldsDoesNotContain(String name) {
    	Field field = findField(this.modelType, name);
		assertThat(field).overridingErrorMessage("Field " + this.modelType.getSimpleName() + "." + name + " does not exist.").isNotNull();
		assertThat(this.testee.getMappableFields())
				.extracting("field")
				.overridingErrorMessage("The detected mappable fields must not contain the field " + field + ".")
				.doesNotContain(field);
	}
}
