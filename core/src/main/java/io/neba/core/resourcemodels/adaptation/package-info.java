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
 * Contains generic {@link org.apache.sling.api.adapter.AdapterFactory adapter factories}
 * for adapting a resource to concrete {@link io.neba.api.annotations.ResourceModel resource models}
 * or the generic {@link io.neba.api.resourcemodels.Model} type. The generic adaptation is used by the
 * {@link io.neba.api.tags.DefineObjectsTag} to adapt to the most specific resource model
 * of a resource.
 */
package io.neba.core.resourcemodels.adaptation;