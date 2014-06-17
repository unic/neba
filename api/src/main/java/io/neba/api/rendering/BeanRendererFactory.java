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

package io.neba.api.rendering;

/**
 * Obtains {@link BeanRenderer} instances with a specific name configured in the
 * sling console. Renderers load templates from a specific repository path, i.e.
 * a path in a JCR repository such as <code>/apps/views/</code>.
 * 
 * @see BeanRenderer
 * @author Olaf Otto
 */
public interface BeanRendererFactory {
    /**
     * @param rendererName the name of the desired renderer. Must not be <code>null</code>.
     * @return the configured renderer, or <code>null</code> if no renderer with the given name exists.
     */
    BeanRenderer get(String rendererName);

    /**
     * @return the default renderer, or <code>null</code> if the default renderer is not configured.
     */
    BeanRenderer getDefault();
}
