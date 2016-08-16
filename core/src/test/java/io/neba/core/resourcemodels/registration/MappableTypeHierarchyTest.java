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

package io.neba.core.resourcemodels.registration;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 * @author Olaf Otto
 */
@RunWith(MockitoJUnitRunner.class)
public class MappableTypeHierarchyTest {
	@Mock
    private Node node;
	@Mock
	private Resource resource;
    @Mock
    private NodeType nodeType;
    @Mock
    private ResourceResolver resolver;

    @Before
    public void prepareNodeTypeHierarchy() throws RepositoryException {
        doReturn(this.nodeType).when(this.node).getPrimaryNodeType();
        doReturn(this.node).when(this.resource).adaptTo(eq(Node.class));
        doReturn("myResourceType").when(this.resource).getResourceType();
        doReturn("myNodeTypeName").when(this.nodeType).getName();
    }

    @Test
    public void testSuccessiveUseOfHierarchies() throws Exception {
        iterateOnceWithMappableTypeHierarchy();
        verifyResourceTypeHierarchyAndNodeTypeHierarchyAreUsed();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testHandlingOfNullResource() throws Exception {
        withNullResource();
        iterateOnceWithMappableTypeHierarchy();
    }

    private void withNullResource() {
        this.resource = null;
    }

    private void verifyResourceTypeHierarchyAndNodeTypeHierarchyAreUsed() throws RepositoryException {
        verify(this.resource, times(2)).adaptTo(eq(Node.class));
        verify(this.resource).getResourceType();
        verify(this.node).getMixinNodeTypes();
        verify(this.node, times(2)).getPrimaryNodeType();
    }

    private void iterateOnceWithMappableTypeHierarchy() {
        Iterable<String> it = new MappableTypeHierarchy(this.resource);
        it.iterator().next();
    }
}
