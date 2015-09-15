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

import io.neba.api.annotations.*;
import io.neba.api.resourcemodels.Optional;
import org.apache.sling.api.resource.Resource;
import org.springframework.beans.factory.annotation.Autowired;

import javax.inject.Inject;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * This resource model contains use cases for resource to model mapping (OCM) and is used
 * by unit tests to test the model metadata.
 *
 * @author Olaf Otto
 */
@ResourceModel(types = "ignored/junit/test/type")
public class TestResourceModel {
    private static String staticField;
    private final String finalField = "finalValue";
    private String stringField;
    private int primitiveIntField;
    private boolean primitiveBooleanField;
    private long primitiveLongField;
    private float primitiveFloatField;
    private double primitiveDoubleField;
    private short primitiveShortField;
    private Date dateField;
    private Calendar calendarField;

    @This
    private Resource thisResource;

    @CustomAnnotationWithThisMetaAnnotation
    private Resource thisResourceWithMetaAnnotation;

    @Reference
    @Path("resourcePath")
    private Resource referencedResource;

    @CustomAnnotationWithReferenceMetaAnnotation
    @Path("resourcePath")
    private Resource referencedResourceWithMetaAnnotation;

    @Reference
    @Path("listResourcePathsWithSimpleTypeParameter")
    private List<Resource> referencedResourcesListWithSimpleTypeParameter;

    @Reference(append = "/jcr:content")
    private OtherTestResourceModel referencedResourceModelWithAbsoluteAppendedReferencePath;

    @Reference(append = "jcr:content")
    private OtherTestResourceModel referencedResourceModelWithRelativeAppendedReferencePath;

    @Path("namespace:customName")
    private String stringFieldWithRelativePathAnnotation;

    @Path("/absolute/path")
    private String stringFieldWithAbsolutePathAnnotation;

    @CustomAnnotationWithPathMetaAnnotation
    private String stringFieldWithPathMetaAnnotation;

    @Path("titleText${language}")
    private String stringFieldWithPlaceholder;

    @Children
    private List<Resource> childrenAsResources;

    @CustomAnnotationWithChildrenMetaAnnotation
    private List<Resource> childrenAsResourcesWithMetaAnnotation;

    @Children(resolveBelowEveryChild = "/jcr:content")
    private List<Resource> childContentResourcesAsResources;

    @Children
    private Optional<List<Resource>> optionalChildContentResourcesAsResources;

    @Reference
    private Optional<OtherTestResourceModel> lazyReferenceToOtherModel;

    private Optional<OtherTestResourceModel> lazyReferenceToChildAsOtherModel;

    private Collection<String> collectionOfStrings;

    @Unmapped
    private String unmappedStringField;

    @CustomAnnotationWithUnmappedMetaAnnotation
    private String unmappedStringFieldWithUnmappedMetaAnnotation;

    @Inject
    private String injectedField;

    @Autowired
    private String autowiredField;

    @javax.annotation.Resource
    private String resourceField;

    public String getStringField() {
        return stringField;
    }

    public Resource getResource() {
        return thisResource;
    }
}
