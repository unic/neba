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

package io.neba.core.util;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.SyntheticResource;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.jcr.Node;
import javax.jcr.nodetype.NodeType;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

import static io.neba.api.Constants.SYNTHETIC_RESOURCETYPE_ROOT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Olaf Otto
 */
@RunWith(MockitoJUnitRunner.class)
public class ResourceTypeHierarchyIteratorTest {
    @Mock
    private ResourceResolver resolver;

	private Resource resource;
    private List<String> resourceHierarchy = new LinkedList<>();

    private ResourceTypeHierarchyIterator testee;

    @Before
    public void prepareResource() {
        withResource(mock(Resource.class));
    }

    @Test
    public void testHandlingOfSyntheticResources() {
        withSyntheticResource();
        withResourceType("virtual/resource/type");
        createIterator();
        resolveResourceHierarchy();
        assertHierarchyIs("virtual/resource/type", SYNTHETIC_RESOURCETYPE_ROOT);
    }
    
    @Test
    public void testIteratorDoesNotUsePrimaryType() throws Exception {
        withPrimaryType("cq:Page");
        createIterator();
        resolveResourceHierarchy();
        assertHierarchyIsEmpty();
    }

    @Test
    public void testResolutionOfTypeHierarchy() {
        withResourceType("junit/test1");
        withResourceSupertype("junit/test1", "junit/test2");
        withResourceSupertype("junit/test2", "junit/test3");
        withResourceSupertype("junit/test3", null);
        createIterator();
        resolveResourceHierarchy();
        assertHierarchyIs("junit/test1", "junit/test2", "junit/test3");
    }


    @Test(expected = NoSuchElementException.class)
    public void testNextInvocationWithoutNextElement() {
        withResourceType("/junit/test1");
        createIterator();
        getNextElement();
        getNextElement();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testResourceMustNotBeNull() {
        new ResourceTypeHierarchyIterator(null);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testIteratorIsReadOnly() {
        withResourceType("junit/test1");
        createIterator();
        removeElement();
    }

    private void removeElement() {
        this.testee.remove();
    }

    private void withResourceType(String type) {
        doReturn(type).when(resource).getResourceType();
    }

    private void withSyntheticResource() {
        withResource(mock(SyntheticResource.class));
    }

    private void withResource(final Resource mock) {
        this.resource = mock;
        doReturn(this.resolver).when(this.resource).getResourceResolver();
    }

    private void withPrimaryType(String primaryType) throws Exception {
        when(this.resource.getResourceType()).thenReturn(primaryType);
        Node resourceNode = mock(Node.class);
        NodeType resourceNodeType = mock(NodeType.class);
        when(resourceNodeType.getName()).thenReturn(primaryType);
        when(resourceNode.getPrimaryNodeType()).thenReturn(resourceNodeType);
        when(this.resource.adaptTo(eq(Node.class))).thenReturn(resourceNode);
    }

    private void getNextElement() {
        this.testee.next();
    }

    private void assertHierarchyIsEmpty() {
        assertHierarchyIs();
    }

    private void assertHierarchyIs(String... expectedHierarchy) {
        assertThat(this.resourceHierarchy).containsExactly(expectedHierarchy);
    }

    private void resolveResourceHierarchy() {
        for (String resourceType : this.testee) {
            this.resourceHierarchy.add(resourceType);
        }
    }

    private void createIterator() {
        this.testee = new ResourceTypeHierarchyIterator(this.resource);
    }

    private void withResourceSupertype(String resourceType, String superType) {
        when(this.resolver.getParentResourceType(resourceType)).thenReturn(superType);
    }
}
