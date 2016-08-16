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

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Queue;

import static java.util.Arrays.asList;

/**
 * Iterates the {@link NodeType node type} hierarchy of a {@link Node}.
 * Starts with the {@link Node#getPrimaryNodeType() primary node type} followed by the
 * {@link Node#getMixinNodeTypes() mixin types directly applied to the node},
 * followed by the {@link NodeType#getDeclaredSupertypes() super types of the primary node type},
 * recursively.<br />
 * The order of a nodes super types is not specified by the JCR API. It is thus considered an implementation
 * detail of the JCR repository and ignored in this context.
 * 
 * @author Olaf Otto
 */
public class NodeTypeHierarchyIterator implements Iterator<String>, Iterable<String> {

    /**
     * @param node must not be <code>null</code>.
     * @return never <code>null</code>.
     */
    public static NodeTypeHierarchyIterator typeHierarchyOf(Node node) {
        return new NodeTypeHierarchyIterator(node);
    }

    private final Queue<NodeType> queue = new LinkedList<>();
    private NodeType current;
    private NodeType next = null;

    /**
     * @param node must not be <code>null</code>.
     */
    public NodeTypeHierarchyIterator(final Node node) {
        if (node == null) {
            throw new IllegalArgumentException("Constructor argument node must not be null.");
        }
        try {
            this.current = node.getPrimaryNodeType();
            this.next = current;
            // Add the mixin node types applied to the current content node only.
            final NodeType[] mixinNodeTypes = node.getMixinNodeTypes();
            if (mixinNodeTypes != null && mixinNodeTypes.length != 0) {
                this.queue.addAll(asList(mixinNodeTypes));
            }
        } catch (RepositoryException e) {
            throw new RuntimeException("Unable to prepare the given node for node type hierarchy traversal.", e);
        }
    }

    public Iterator<String> iterator() {
        return this;
    }

    public boolean hasNext() {
        return this.next != null || this.current != null && resolveNext();
    }

    private boolean resolveNext() {
        NodeType[] superTypes = this.current.getDeclaredSupertypes();
        if (superTypes != null && superTypes.length != 0) {
            queue.addAll(asList(superTypes));
        }
        if (!queue.isEmpty()) {
            this.next = this.queue.poll();
        }
        return this.next != null;
    }

    public String next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        this.current = this.next;
        this.next = null;
        return this.current.getName();
    }

    public void remove() {
        throw new UnsupportedOperationException("Remove is unsupported - this is a read-only iterator.");
    }
}
