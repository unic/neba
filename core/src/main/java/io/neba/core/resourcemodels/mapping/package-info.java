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
 * Contains the {@link org.apache.sling.api.resource.Resource} to {@link io.neba.api.annotations.ResourceModel}
 * mapper implementation. Most importantly, the {@link io.neba.core.resourcemodels.mapping.FieldValueMappingCallback}
 * is used to traverse and map the fields of a resource model. Further aspects are the support
 * of {@link io.neba.core.resourcemodels.mapping.NestedMappingSupport nested (cyclic) mappings}
 * as well as the collection of statistical data regarding these mappings.
 */
package io.neba.core.resourcemodels.mapping;