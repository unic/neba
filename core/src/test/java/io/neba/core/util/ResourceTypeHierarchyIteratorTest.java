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

package io.neba.core.util;

import io.neba.api.Constants;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.SyntheticResource;
import org.apache.sling.api.resource.ValueMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.matchers.JUnitMatchers;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.jcr.Node;
import javax.jcr.nodetype.NodeType;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Olaf Otto
 */
@RunWith(MockitoJUnitRunner.class)
public class ResourceTypeHierarchyIteratorTest {
    @Mock
    private ResourceResolver administrativeResourceResolver;

	private Resource resource;
    private List<String> resourceHierarchy = new LinkedList<String>();
    private String resourceType;
    private String resourceSupertype;
    private String supertypeResourceType;
    private Node resourceNode;
    private NodeType resourceNodeType;

    private ResourceTypeHierarchyIterator testee;

    @Before
    public void prepareResource() {
        withResource(mock(Resource.class));
    }

    @Test
    public void testHandlingOfSyntheticResources() throws Exception {
        withSearchPath("/apps/", "/libs/", "/etc/");
        withSyntheticResource();
        withResourceType("virtual/resource/type");
        createIterator();
        resolveResourceHierarchy();
        assertHierarchyIs("virtual/resource/type", Constants.SYNTHETIC_RESOURCETYPE_ROOT);
    }
    
    @Test
    public void testIteratorDoesNotUsePrimaryType() throws Exception {
        withSearchPath("/apps/", "/libs/", "/etc/");
        withPrimaryType("cq:Page");
        createIterator();
        resolveResourceHierarchy();
        assertHierarchyIsEmpty();
    }

    @Test
    public void testPrecedenceOfDirectTypeHierarchy() throws Exception {
        withResourceType("/junit/test1");
        withResourceSupertype("/junit/test2");
        withSupertypeResource("/junit/test3");
        createIterator();
        resolveResourceHierarchy();
        assertHierarchyIsBasedOnDirectResourceSupertype();
    }

    @Test
    public void testResoultionOfIndirectTypeHierarchWithRelativePath() throws Exception {
        withSearchPath("/apps/", "/libs/", "/etc/");
        withResourceType("junit/test1");
        withResourceSupertype(null);
        withResource("/libs/junit/test1", "junit/testrelative", "junit/test2");
        withResource("/libs/junit/test2", "junit/test2", null);
        createIterator();
        resolveResourceHierarchy();
        assertHierarchyIs("junit/test1", "junit/test2");
    }

    @Test
    public void testUsageOfIndirectTypeHierarchy() throws Exception {
        withResourceType("/junit/test1");
        withResourceSupertype(null);
        withSupertypeResource("/junit/test3");
        createIterator();
        resolveResourceHierarchy();
        assertResourceHierarchyIsBasedOnSuperResourceType();
    }
    
    @Test(expected = NoSuchElementException.class)
    public void testNextInvocationWithoutNextElement() throws Exception {
        withResourceType("/junit/test1");
        withResourceSupertype(null);
        createIterator();
        getNextElement();
        getNextElement();
    }

    private void withSyntheticResource() {
        withoutValueMap();
        withResource(mock(SyntheticResource.class));
    }

    private void withResource(final Resource mock) {
        this.resource = mock;
    }

    private void withoutValueMap() {
        when(this.resource.adaptTo(eq(ValueMap.class))).thenReturn(null);
    }

    private void withSearchPath(String... paths) {
        when(this.administrativeResourceResolver.getSearchPath()).thenReturn(paths);
    }

    private void withPrimaryType(String primaryType) throws Exception {
        this.resourceType = primaryType;
        when(this.resource.getResourceType()).thenReturn(primaryType);
        this.resourceNode = mock(Node.class);
        this.resourceNodeType = mock(NodeType.class);
        when(this.resourceNodeType.getName()).thenReturn(primaryType);
        when(this.resourceNode.getPrimaryNodeType()).thenReturn(this.resourceNodeType);
        when(this.resource.adaptTo(eq(Node.class))).thenReturn(this.resourceNode);
    }

    private void getNextElement() {
        this.testee.next();
    }

    private void assertHierarchyIsEmpty() {
        assertHierarchyIs();
    }

    private void assertHierarchyIsBasedOnDirectResourceSupertype() {
        assertHierarchyIs(this.resourceType, this.resourceSupertype);
    }

    private void assertResourceHierarchyIsBasedOnSuperResourceType() {
        assertHierarchyIs(this.resourceType, this.supertypeResourceType);
    }

    private void assertHierarchyIs(String... expectedHierarchy) {
        assertThat(this.resourceHierarchy.size(), is(expectedHierarchy.length));
        assertThat(this.resourceHierarchy, JUnitMatchers.hasItems(expectedHierarchy));
    }

    private void resolveResourceHierarchy() {
        for (String resourceType : this.testee) {
            this.resourceHierarchy.add(resourceType);
        }
    }

    private void withResource(String resourcePath, String resourceType, String resourceSuperType) {
        Resource resource = mock(Resource.class);
        when(resource.getResourceType()).thenReturn(resourceType);
        when(resource.getResourceSuperType()).thenReturn(resourceSuperType);
        when(resource.getResourceResolver()).thenReturn(this.administrativeResourceResolver);
        when(this.administrativeResourceResolver.getResource(eq(resourcePath))).thenReturn(resource);
    }
    
    private void withSupertypeResource(String value) {
        this.supertypeResourceType = value;
        Resource resourceTypeResource = mock(Resource.class);
        when(resourceTypeResource.getResourceResolver()).thenReturn(this.administrativeResourceResolver);
        when(resourceTypeResource.getResourceSuperType()).thenReturn(this.supertypeResourceType);
        when(this.administrativeResourceResolver.getResource(eq(this.resource.getResourceType()))).thenReturn(resourceTypeResource);
    }

    private void createIterator() {
        this.testee = new ResourceTypeHierarchyIterator(this.resource, this.administrativeResourceResolver);
    }

    private void withResourceSupertype(String value) {
        this.resourceSupertype = value;
        when(this.resource.getResourceSuperType()).thenReturn(this.resourceSupertype);
    }

    private void withResourceType(String value) {
        this.resourceType = value;
        when(this.resource.getResourceType()).thenReturn(this.resourceType);
    }
}
