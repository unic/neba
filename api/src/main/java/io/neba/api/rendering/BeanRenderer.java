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

import java.util.Map;

/**
 * Renders any object to a string using an internal template mechanism,
 * such as velocity.<br />
 * The view used to render the object is derived from the
 * object's type and the provided view hint.
 * 
 * @author Olaf Otto
 */
public interface BeanRenderer {
    /**
     * @param bean must not be <code>null</code>.
     * @return the rendered bean, or <code>null</code> if no view was found for the bean.
     */
    String render(Object bean);

    /**
     * @param bean must not be <code>null</code>.
     * @param viewHint can be <code>null</code>.
     * @return the rendered bean, or <code>null</code> if no view was found for the bean.
     */
    String render(Object bean, String viewHint);

    /**
     * @param bean must not be <code>null</code>.
     * @param viewHint can be <code>null</code>.
     * @param context additional objects to be exposed in the rendering context. can be <code>null</code>.
     *        It is not possible to override variables provided e.g. by bindings values providers
     *        in this context. If a key in the given context already exists in the scripting bindings
     *        an {@link IllegalArgumentException} will be thrown.
     * @return the rendered bean, or <code>null</code> if no view was found for the bean.
     */
    String render(Object bean, String viewHint, Map<String, Object> context);

    /**
     * @param bean must not be <code>null</code>.
     * @param context additional objects to be exposed in the rendering context. can be <code>null</code>.
     *        It is not possible to override variables provided e.g. by bindings values providers
     *        in this context. If a key in the given context already exists in the scripting bindings
     *        an {@link IllegalArgumentException} will be thrown.
     * @return the rendered bean, or <code>null</code> if no view was found for the bean.
     */
    String render(Object bean, Map<String, Object> context);
}
