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

package io.neba.core.resourcemodels.adaptation;

/**
 * Thrown when a {@link org.apache.sling.api.resource.Resource} is 
 * {@link org.apache.sling.api.resource.Resource#adaptTo(Class) adapted to}
 * a target type via the {@link ResourceToModelAdapter}, but there is more than
 * one model with the target type (e.g. sharing the same super class or interface).
 * 
 * @see ResourceToModelAdapter#getAdapter(Object, Class)
 * @author Olaf Otto
 */
public class AmbiguousModelAssociationException extends RuntimeException {
    private static final long serialVersionUID = -8109827534618440349L;

    public AmbiguousModelAssociationException(String message) {
        super(message);
    }
}
