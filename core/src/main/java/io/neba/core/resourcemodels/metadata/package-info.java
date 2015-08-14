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
 * Contains implementations representing the
 * {@link io.neba.core.resourcemodels.metadata.ResourceModelMetaData resource model metadata}
 * created during both resource model registration and usage. This metadata is used during the lookup and adaptation of the resource models
 * to avoid costly superfluous reflection on the resource model types at runtime. Furthermore,
 * the {@link io.neba.core.resourcemodels.metadata.ModelStatisticsConsolePlugin metadata console plugin}
 * leverages the static resource model metadata in combination with
 * {@link io.neba.core.resourcemodels.metadata.ResourceModelStatistics statistical resource model data collected at runtime}. to
 * display and visualize runtime behavior.
 */
package io.neba.core.resourcemodels.metadata;