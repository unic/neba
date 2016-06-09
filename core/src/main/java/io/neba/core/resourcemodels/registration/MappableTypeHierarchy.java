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

import org.apache.commons.collections.IteratorUtils;
import org.apache.sling.api.resource.Resource;

import javax.jcr.Node;
import java.util.Iterator;

import static io.neba.core.util.NodeTypeHierarchyIterator.typeHierarchyOf;
import static io.neba.core.util.ResourceTypeHierarchyIterator.typeHierarchyOf;

/**
 * Represents all type names to which a 
 * {@link io.neba.api.annotations.ResourceModel} may apply.
 * <br />
 * Iterates over the resource type hierarchy of a {@link Resource},
 * followed by the {@link Node} type hierarchy.
 * 
 * @see io.neba.core.util.ResourceTypeHierarchyIterator
 * @see io.neba.core.util.NodeTypeHierarchyIterator
 * 
 * @author Olaf Otto
 */
public class MappableTypeHierarchy implements Iterable<String> {
    private final Resource resource;

    public static MappableTypeHierarchy mappableTypeHierarchyOf(final Resource resource) {
    	return new MappableTypeHierarchy(resource);
    }
    
    public MappableTypeHierarchy(final Resource resource) {
        if (resource == null) {
            throw new IllegalArgumentException("Constructor argument resource must not be null.");
        }
        this.resource = resource;
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public Iterator<String> iterator() {
        Iterator<String> it;
        final Iterator<String> resourceTypeIterator = typeHierarchyOf(this.resource);
        final Node node = this.resource.adaptTo(Node.class);
        // A virtual resource must not have a node.
        if (node != null) {
            final Iterator<String> nodeTypeIterator = typeHierarchyOf(node);
            it = IteratorUtils.chainedIterator(resourceTypeIterator, nodeTypeIterator);
        } else {
            it = resourceTypeIterator;
        }
        return it;
    }
}
