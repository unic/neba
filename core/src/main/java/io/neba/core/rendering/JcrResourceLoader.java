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

package io.neba.core.rendering;

import io.neba.core.sling.AdministrativeResourceResolver;
import org.apache.commons.collections.ExtendedProperties;
import org.apache.sling.api.resource.Resource;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.resource.loader.ResourceLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.InputStream;

/**
 * Loads velocity resources from a JCR repository.
 * 
 * @author Olaf Otto
 */
@Service
public class JcrResourceLoader extends ResourceLoader {
    @Autowired
    private AdministrativeResourceResolver resourceResolver;

    @Override
    public void init(ExtendedProperties configuration) {
    }

    @Override
    public InputStream getResourceStream(String source) throws ResourceNotFoundException {
        InputStream in = null;
        Resource resource = getResource(source);
        if (resource != null) {
            in = resource.adaptTo(InputStream.class);
        }
        return in;
    }

    @Override
    public boolean isSourceModified(org.apache.velocity.runtime.resource.Resource velocityResource) {
        Resource resource = getResource(velocityResource.getName());
        boolean resourceWasRemoved = resource == null;
        boolean resourceWasModified = !resourceWasRemoved && 
                resource.getResourceMetadata().getModificationTime() > velocityResource.getLastModified();
        return resourceWasRemoved || resourceWasModified;
    }

    @Override
    public long getLastModified(org.apache.velocity.runtime.resource.Resource velocityResource) {
        Resource resource = getResource(velocityResource.getName());
        return resource == null ? -1 : resource.getResourceMetadata().getModificationTime();
    }

    private Resource getResource(String source) {
        return this.resourceResolver.get(source);
    }
}
