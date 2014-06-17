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
import org.apache.sling.api.resource.ResourceUtil;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.util.Iterator;
import java.util.NoSuchElementException;

import static io.neba.api.Constants.SYNTHETIC_RESOURCETYPE_ROOT;
import static org.apache.sling.api.resource.ResourceUtil.getResourceSuperType;

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
 * @see ResourceUtil#findResourceSuperType(Resource)
 * @author Olaf Otto
 */
public class ResourceTypeHierarchyIterator implements Iterator<String>, Iterable<String> {
    private final ResourceResolver resolver;

    /**
     * @param resource must not be <code>null</code>.
     * @return never <code>null</code>.
     */
    public static ResourceTypeHierarchyIterator typeHierarchyOf(final Resource resource, final ResourceResolver resourceResolver) {
        return new ResourceTypeHierarchyIterator(resource, resourceResolver);
    }

    private Resource currentResource;
    private String resourceType;
    private boolean isSyntheticResource;

    /**
     * @param resource must not be <code>null</code>.
     */
    public ResourceTypeHierarchyIterator(final Resource resource, final ResourceResolver resourceResolver) {
        if (resource == null) {
            throw new IllegalArgumentException("Constructor parameter resource must not be null.");
        }
        if (resourceResolver == null) {
            throw new IllegalArgumentException("Method argument resourceResolver must not be null.");
        }

        this.resolver = resourceResolver;
        this.currentResource = resource;
        this.isSyntheticResource = ResourceUtil.isSyntheticResource(resource);

        if (this.isSyntheticResource) {
            // Synthetic resources do not represent nodes, thus their type is
            // intentionally provided by the resource implementation
            // and does not fall back to the primary type of a node.
            this.resourceType = resource.getResourceType();
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
                        this.resourceType = resourceType;
                    }
                } catch (RepositoryException e) {
                    throw new RuntimeException("Unable to obtain the node type.", e);
                }
            } else {
                this.resourceType = resourceType;
            }
        }
    }

    public boolean hasNext() {
        return this.resourceType != null || this.currentResource != null && resolveNext();
    }

    public String next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        String type = this.resourceType;
        this.resourceType = null;
        return type;
    }

    private boolean resolveNext() {
        Resource nextResource = null;

        String nextResourceType = this.currentResource.getResourceSuperType();
        if (nextResourceType == null) {
            nextResourceType = getResourceSuperType(this.resolver, this.currentResource.getResourceType());
        }

        if (nextResourceType != null) {
            nextResource = findResource(nextResourceType);
        } else if (isProvideSyntheticResourceRoot()) {
            nextResourceType = SYNTHETIC_RESOURCETYPE_ROOT;
        }
        this.currentResource = nextResource;
        this.resourceType = nextResourceType;
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
        return this.isSyntheticResource && !SYNTHETIC_RESOURCETYPE_ROOT.equals(this.resourceType);
    }

    private Resource findResource(String resourceSuperType) {
        Resource resource = null;
        if (isAbsolutePath(resourceSuperType)) {
            resource = this.resolver.getResource(resourceSuperType);
        } else {
            for (String prefix : this.resolver.getSearchPath()) {
                String absoluteResourcePath = prefix + resourceSuperType;
                resource = this.resolver.getResource(absoluteResourcePath);
                if (resource != null) {
                    break;
                }
            }
        }
        return resource;
    }

    private boolean isAbsolutePath(String resourceSuperType) {
        return resourceSuperType.charAt(0) == '/';
    }

    public void remove() {
        throw new UnsupportedOperationException("The resource hierarchy iterator is read-only.");
    }

    @Override
    public Iterator<String> iterator() {
        return this;
    }
}
