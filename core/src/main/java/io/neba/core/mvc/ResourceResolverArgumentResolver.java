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

package io.neba.core.mvc;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.ResourceResolver;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebArgumentResolver;
import org.springframework.web.context.request.NativeWebRequest;

/**
 * Supports {@link ResourceResolver} arguments of a {@link org.springframework.web.bind.annotation.RequestMapping}.
 * <br />
 *
 * Example:<br />
 * <p>
 * <pre>
 *  &#64;{@link org.springframework.web.bind.annotation.RequestMapping}(...)
 *  public void myHandlerMethod({@link ResourceResolver} resolver, ...) {
 *      ...
 *  }
 * </pre>
 * </p>
 *
 * @author Olaf Otto
 */
public class ResourceResolverArgumentResolver implements WebArgumentResolver {
    private boolean supportsParameter(MethodParameter parameter) {
        return ResourceResolver.class.isAssignableFrom(parameter.getParameterType());
    }

    private Object resolveArgumentInternal(MethodParameter parameter, NativeWebRequest webRequest) throws Exception {
        Object request = webRequest.getNativeRequest();
        if (request instanceof SlingHttpServletRequest) {
            return ((SlingHttpServletRequest) request).getResourceResolver();
        }
        return null;
    }

    @Override
    public Object resolveArgument(MethodParameter methodParameter, NativeWebRequest webRequest) throws Exception {
        return resolveArgumentInternal(methodParameter, webRequest);
    }
}
