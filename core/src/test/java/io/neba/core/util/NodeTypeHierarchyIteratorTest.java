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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.jcr.Node;
import javax.jcr.nodetype.NodeType;

import static io.neba.core.util.NodeTypeHierarchyIterator.typeHierarchyOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Olaf Otto
 */
@RunWith(MockitoJUnitRunner.class)
public class NodeTypeHierarchyIteratorTest {
	@Mock
    private Node node;

    @Before
    public void prepare() throws Exception {
       NodeType[] mixins = new NodeType[2];
       mixins[0] = mockNodeType("mixin1");
       mixins[1] = mockNodeType("mixin2");
       when(this.node.getMixinNodeTypes()).thenReturn(mixins);

       NodeType primaryType = mockNodeType("primaryType");
       when(this.node.getPrimaryNodeType()).thenReturn(primaryType);
       
       NodeType[] superTypes = new NodeType[2];
       superTypes[0] = mockNodeType("superType1");
       superTypes[1] = mockNodeType("superType2");
       when(primaryType.getDeclaredSupertypes()).thenReturn(superTypes);
    }

    @Test
    public void testHierarchyTraversal() {
        NodeTypeHierarchyIterator it = typeHierarchyOf(this.node);
        assertThat(it.next()).isEqualTo("primaryType");
        assertThat(it.next()).isEqualTo("mixin1");
        assertThat(it.next()).isEqualTo("mixin2");
        assertThat(it.next()).isEqualTo("superType1");
        assertThat(it.next()).isEqualTo("superType2");
        assertThat(it.hasNext()).isFalse();
    }
    
    @Test(expected = UnsupportedOperationException.class)
    public void testIteratorModification() throws Exception {
        NodeTypeHierarchyIterator it = typeHierarchyOf(this.node);
        it.remove();
    }
    
    private NodeType mockNodeType(String typeName) {
        final NodeType mock = mock(NodeType.class);
        when(mock.getName()).thenReturn(typeName);
        return mock;
    }
}
