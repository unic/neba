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

package io.neba.core.resourcemodels.mapping.testmodels;

import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import io.neba.api.annotations.Children;
import org.apache.sling.api.resource.Resource;

import io.neba.api.annotations.Path;
import io.neba.api.annotations.Reference;
import io.neba.api.annotations.ResourceModel;
import io.neba.api.annotations.This;
import io.neba.api.annotations.Unmapped;

/**
 * This resource model contains all possible <em>valid</em> use cases for resouce
 * to model mapping (OCM).
 * 
 * @author Olaf Otto
 */
@ResourceModel(types = "ignored/junit/test/type")
public class TestResourceModel {
    private static String STATIC_FIELD;
    private final String finalField = "finalValue";
    private String stringField;
    private Resource resourceField;
    private int primitiveIntField;
    private boolean primitiveBooleanField;
    private long primitiveLongField;
    private float primitiveFloatField;
    private double primitiveDoubleField;
    private short primitiveShortField;
    private Date dateField;
    private Calendar calendarField;

    @Reference
    @Path("resourcePath")
    private Resource referencedResource;

    @Reference
    @Path("arrayResourcePaths")
    private Resource[] referencedResourcesArray;

    @Reference
    @Path("listResourcePathsWithLowerBoundWildCard")
    private List<? super Resource> referencedResourcesListWithLowerBoundWildcard;

    @Reference
    @Path("listResourcePathsWithSimpleTypeParameter")
    private List<Resource> referencedResourcesListWithSimpleTypeParameter;

    @Reference
    @Path("setResourcePathsWithSimpleTypeParameter")
    private Set<Resource> referencedResourceSetWithSimpleTypeParameter;

    @Reference
    @Path("collectionResourcePathsWithSimpleTypeParameter")
    private Collection<Resource> referencedResourceCollectionWithSimpleTypeParameter;

    @This
    private Resource resource;

    @This
    private OtherTestResourceModel otherModelThis;
    
    @Reference
    @Path("other")
    private OtherTestResourceModel otherModelOtherPath;

    private TestResourceModel loadedFromChildResource;

    @Unmapped
    private String transientStringField;

    @Path("namespace:customName")
    private String stringFieldWithRelativePathAnnotation;

    @Path("/absolute/path")
    private String stringFieldWithAbsolutePathAnnotation;

    @Path("titleText${language}")
    private String stringFieldWithPlaceholder;

    @Children
    private List<Resource> childrenAsResources;

    private Collection<String> collectionOfStrings;

    @Inject
    private String injectedField;
    
    private Resource childResource;

	public static String getStaticField() {
		return STATIC_FIELD;
	}

	public String getFinalField() {
		return finalField;
	}

	public String getStringField() {
		return stringField;
	}

	public Resource getResourceField() {
		return resourceField;
	}

	public int getPrimitiveIntField() {
		return primitiveIntField;
	}

	public boolean isPrimitiveBooleanField() {
		return primitiveBooleanField;
	}

	public long getPrimitiveLongField() {
		return primitiveLongField;
	}

	public float getPrimitiveFloatField() {
		return primitiveFloatField;
	}

	public double getPrimitiveDoubleField() {
		return primitiveDoubleField;
	}

	public short getPrimitiveShortField() {
		return primitiveShortField;
	}

	public Resource getReferencedResource() {
		return referencedResource;
	}

	public Resource[] getReferencedResourcesArray() {
		return referencedResourcesArray;
	}

	public List<? super Resource> getReferencedResourcesListWithLowerBoundWildcard() {
		return referencedResourcesListWithLowerBoundWildcard;
	}

	public List<Resource> getReferencedResourcesListWithSimpleTypeParameter() {
		return referencedResourcesListWithSimpleTypeParameter;
	}

	public Set<Resource> getReferencedResourceSetWithSimpleTypeParameter() {
		return referencedResourceSetWithSimpleTypeParameter;
	}

	public Collection<Resource> getReferencedResourceCollectionWithSimpleTypeParameter() {
		return referencedResourceCollectionWithSimpleTypeParameter;
	}

	public Resource getResource() {
		return resource;
	}

	public OtherTestResourceModel getOtherModelThis() {
		return otherModelThis;
	}

	public OtherTestResourceModel getOtherModelOtherPath() {
		return otherModelOtherPath;
	}

	public TestResourceModel getLoadedFromChildResource() {
		return loadedFromChildResource;
	}

	public String getTransientStringField() {
		return transientStringField;
	}

	public String getStringFieldWithRelativePathAnnotation() {
		return stringFieldWithRelativePathAnnotation;
	}

	public String getStringFieldWithAbsolutePathAnnotation() {
		return stringFieldWithAbsolutePathAnnotation;
	}

	public String getStringFieldWithPlaceholder() {
		return stringFieldWithPlaceholder;
	}

	public String getInjectedField() {
		return injectedField;
	}

	public Resource getChildResource() {
		return childResource;
	}

	public static String getSTATIC_FIELD() {
		return STATIC_FIELD;
	}

	public Date getDateField() {
		return dateField;
	}

	public Calendar getCalendarField() {
		return calendarField;
	}
}
