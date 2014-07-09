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
 * Resource models are detected during the construction phase of a bundle's application context
 * by the {@link io.neba.core.resourcemodels.registration.ModelRegistrar}. References to
 * Registered resource models are stored in the {@link io.neba.core.resourcemodels.registration.ModelRegistry}.
 * The detected models can be viewed using the {@link io.neba.core.resourcemodels.registration.ModelRegistryConsolePlugin
 * model registry console}.
 */
package io.neba.core.resourcemodels.registration;