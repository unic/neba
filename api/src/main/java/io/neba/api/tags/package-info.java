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
 * Contains the NEBA tag libraries.
 * All tag libraries contained in this package are not extensible to prevent direct dependencies
 * of project-specific implementations to the NEBA core, i.e. to prevent
 * project code to inherit from these tag implementations.
 */
@TagLibrary(
        value = "http://neba.io/1.0",
        descriptorFile = "neba.tld",
        shortName = "neba",
        description = "NEBA tag library",
        libraryVersion = "1.0")
package io.neba.api.tags;

import tldgen.TagLibrary;