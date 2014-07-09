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

/**
 * The sub packages contain the implementations of the {@link io.neba.api.annotations.ResourceModel}
 * lifecycle: Registration (detection and lookup), metadata (created at registration time and runtime),
 * adaptation ({@link org.apache.sling.api.adapter.AdapterFactory mainly adapter factory support}) for adapting
 * from resources to the resource models, mapping (for injecting properties of resources into the resource models) and
 * caching of adapted resource models.
 */
package io.neba.core.resourcemodels;