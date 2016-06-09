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

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.util.Iterator;
import java.util.NoSuchElementException;

import static io.neba.api.Constants.SYNTHETIC_RESOURCETYPE_ROOT;
import static org.apache.sling.api.resource.ResourceUtil.isSyntheticResource;

/**
 * Iterates the type hierarchy of a {@link Resource} starting with the resource's
 * "sling:resourceType". {@link #next()} will either yield the current resource
 * {@link Resource#getResourceSuperType() supertype} or, if this supertype does
 * not exist, the resource supertype of the resource identified by the current
 * {@link Resource#getResourceType() resource type}. <br />
 * Note that this iterator can be empty. {@link Resource#getResourceType()}
 * falls back to the {@link javax.jcr.Node#getPrimaryNodeType() primary node type}
 * if no "sling:resourceType" property is set; however the node type hierarchy
 * is covered by the {@link NodeTypeHierarchyIterator} and is not used by this iterator.
 * 
 * @see ResourceResolver#getParentResourceType(String)
 * @author Olaf Otto
 */
public class ResourceTypeHierarchyIterator implements Iterator<String>, Iterable<String> {
    /**
     * @param resource must not be <code>null</code>.
     * @return never <code>null</code>.
     */
    public static ResourceTypeHierarchyIterator typeHierarchyOf(final Resource resource) {
        return new ResourceTypeHierarchyIterator(resource);
    }

    private final ResourceResolver resolver;

    private String currentResourceType;
    private String nextResourceType;
    private boolean isSyntheticResource;

    /**
     * @param resource must not be <code>null</code>.
     */
    public ResourceTypeHierarchyIterator(final Resource resource) {
        if (resource == null) {
            throw new IllegalArgumentException("Constructor parameter resource must not be null.");
        }
        this.resolver = resource.getResourceResolver();
        this.isSyntheticResource = isSyntheticResource(resource);

        if (this.isSyntheticResource) {
            // Synthetic resources do not represent nodes, thus their type is
            // intentionally provided by the resource implementation
            // and does not fall back to the primary type of a node.
            this.currentResourceType = resource.getResourceType();
        } else {
            String resourceType = resource.getResourceType();
            Node node = resource.adaptTo(Node.class);
            if (node != null) {
                // If a resource represents a node, the resource type must
                // not be the node type since we intend to traverse the sling:resourceType hierarchy.
                // However, the resourceType provided by resource#getResourceType could be the node type since
                // Resource#getResourceType falls back to the node type of no sling:resourceType is specified.
                try {
                    String nodeType = node.getPrimaryNodeType().getName();
                    if (!nodeType.equals(resourceType)) {
                        this.currentResourceType = resourceType;
                    }
                } catch (RepositoryException e) {
                    throw new RuntimeException("Unable to obtain the node type.", e);
                }
            } else {
                this.currentResourceType = resourceType;
            }
        }

        this.nextResourceType = this.currentResourceType;
    }

    public boolean hasNext() {
        return this.nextResourceType != null || this.currentResourceType != null && resolveNext();
    }

    public String next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        String type = this.nextResourceType;
        this.nextResourceType = null;
        this.currentResourceType = type;
        return type;
    }

    private boolean resolveNext() {
        String nextResourceType = this.resolver.getParentResourceType(this.currentResourceType);
        if (nextResourceType == null && isProvideSyntheticResourceRoot()) {
            nextResourceType = SYNTHETIC_RESOURCETYPE_ROOT;
        }
        this.nextResourceType = nextResourceType;
        return nextResourceType != null;
    }

    /**
     * This iterator provides a virtual resource type root
     * common to all synthetic resources to enable mapping to all
     * resources including synthetic ones.
     * 
     * @see io.neba.api.Constants#SYNTHETIC_RESOURCETYPE_ROOT
     */
    public boolean isProvideSyntheticResourceRoot() {
        return this.isSyntheticResource && !SYNTHETIC_RESOURCETYPE_ROOT.equals(this.currentResourceType);
    }

    public void remove() {
        throw new UnsupportedOperationException("The resource hierarchy iterator is read-only.");
    }

    @Override
    public Iterator<String> iterator() {
        return this;
    }
}
