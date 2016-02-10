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

package io.neba.core.resourcemodels.caching;

import io.neba.api.resourcemodels.ResourceModelCache;
import io.neba.core.util.Key;
import org.apache.sling.api.resource.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents all currently registered {@link ResourceModelCache resource model cache services}.
 * <br />
 * 
 * Thread-safety must be provided by the underlying {@link ResourceModelCache caches}.
 * 
 * @author Olaf Otto
 */
@Service
public class ResourceModelCaches {
	private final List<ResourceModelCache> caches = new ArrayList<>();

	/**
	 * Looks up the {@link #store(Resource, Object, Key) cached model}
	 * of the given type for the given resource. 
	 * Returns the first model found in the caches.
	 * 
	 * @param key must not be <code>null</code>.
	 * @return can be <code>null</code>.
	 */
    public <T> T lookup(Key key) {
    	T model = null;

    	for (ResourceModelCache cache : this.caches) {
    		model = cache.get(key);
    		if (model != null) {
    			break;
    		}
    	}
    	return model;
    }
    
    /**
     * Stores the model representing the result of the
     * {@link Resource#adaptTo(Class) adaptation} of the given resource
     * to the given target type.
     * 
     * @param resource must not be <code>null</code>.
     * @param model can be <code>null</code>.
     * @param key must not be <code>null</code>.
     */
    public <T> void store(Resource resource, T model, Key key) {
    	for (ResourceModelCache cache : this.caches) {
    		cache.put(resource, model, key);
    	}
    }

	public void bind(ResourceModelCache cache) {
		this.caches.add(cache);
	}
	
	public void unbind(ResourceModelCache cache) {
		if (cache == null) {
			return;
		}
		this.caches.remove(cache);
	}
}
