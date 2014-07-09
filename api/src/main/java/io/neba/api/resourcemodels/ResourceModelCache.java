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

package io.neba.api.resourcemodels;

import org.apache.sling.api.resource.Resource;

/**
 * <h2>Function of the resource model cache</h2>
 * Implementations cache the result of {@link Resource#adaptTo(Class) resource adaptations} to a specific target type.
 * They are invoked by the generic NEBA adapter factory prior to the lookup and mapping of
 * a {@link io.neba.api.annotations.ResourceModel}. If the model is not contained
 * in any cache, it is obtained from its spring bean factory, mapped and afterwards 
 * {@link #put(Resource, Object, Object) added} to all known {@link ResourceModelCache resource model caches}.
 * 
 * <h2>Providing a cache implementation</h2>
 * To provide a cache implementation, publish an OSGi service with this interface.
 * It will automatically be detected by NEBA. You may provide any number of caches. NEBA
 * will always attempt to retrieve a cached model from all of them. The first cache 
 * {@link #get(Object) providing} a non-<code>null</code> model wins.
 * There is no guarantee concerning the order in which caches are invoked.<br />
 * Likewise, a model will be {@link #put(Resource, Object, Object) stored} in all available caches.
 * 
 * <h2>Default implementations shipped with NEBA</h2>
 * NEBA comes with a sensible default implementation of this cache, the <em>request-scoped resource model cache</em>,
 * which is configurable via the Apache Felix console.<br />
 * 
 * <h2>Architecture considerations when providing a custom cache implementation</h2>
 * Caching is hard. You must be aware that correctly scoping, i.e. expiring objects
 * from this cache is crucial for both system stability and semantic correctness. Specifically,
 * items in the cache <em>must never ever</em>:
 * 
 * <ul>
 *   <li>Live longer than the resources they represent, including any subsequent resources mapped to
 *       the resource model</li>
 *   <li>Live longer than the {@link io.neba.api.annotations.ResourceModel resource model}
 *       represented by the model type and any subsequent resource models mapped to it</li>
 * </ul> 
 * 
 * @author Olaf Otto
 */
public interface ResourceModelCache {
	/**
	 * Retrieve a cached model.
	 * 
	 * @param key The key used to identify the stored model. Never <code>null</code>.
	 * @return The cached model, or <code>null</code>.
	 */
	<T> T get(Object key);
	
	/**
	 * @param resource The resource {@link Resource#adaptTo(Class) adapted} to the target type. Never <code>null</code>.
	 * @param model the model representing the mapped result of the adaptation. Can be <code>null</code>.
     * @param key the key by which the model is identified and {@link #get(Object) retrieved}. Never <code>null</code>.
	 */
	<T> void put(Resource resource, T model, Object key);
}