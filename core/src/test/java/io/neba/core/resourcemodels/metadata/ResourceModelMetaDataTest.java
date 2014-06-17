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
import org.fest.assertions.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.lang.reflect.Field;

import static org.fest.assertions.Assertions.assertThat;
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
    public void testMappingOfStaticField() throws Exception {
    	assertMappableFieldsDoesNotContain("STATIC_FIELD");
    }

	@Test
    public void testMappingOfFinalField() throws Exception {
		assertMappableFieldsDoesNotContain("finalField");
    }

    @Test
    public void testMappingOfUnmappedField() throws Exception {
    	assertMappableFieldsDoesNotContain("transientStringField");
    }

    @Test
    public void testMappingOfInjectedField() throws Exception {
    	assertMappableFieldsDoesNotContain("injectedField");
    }

    @Test
    public void testMappingOfInheritedPrivateFields() throws Exception {
    	createMetadataFor(ExtendedTestResourceModel.class);
    	assertMetadataEqualsMetadataOf(TestResourceModel.class);
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
		for (MappedFieldMetaData fm : this.testee.getMappableFields()) {
			assertThat(fm.getField()).isNotEqualTo(field);
		}
	}
}
