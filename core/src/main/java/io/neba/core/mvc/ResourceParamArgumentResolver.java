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

import io.neba.api.annotations.ResourceParam;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import static org.apache.commons.lang.StringUtils.isEmpty;
import static org.apache.sling.api.resource.ResourceUtil.isNonExistingResource;

/**
 * Supports {@link io.neba.api.annotations.ResourceModel} arguments of a
 * {@link org.springframework.web.bind.annotation.RequestMapping}.
 * <br />
 *
 * Example:<br />
 * <p>
 * <pre>
 *  &#64;{@link org.springframework.web.bind.annotation.RequestMapping}(...)
 *  public void myHandlerMethod(&#64;{@link io.neba.api.annotations.ResourceParam} MyModel model, ...) {
 *      ...
 *  }
 * </pre>
 * </p>
 *
 * This will expect a String parameter "model", which is a path to a JCR resource adapting to "MyModel". This argument resolver
 * will resolve the path and adapt to the desired model. Unless the resource param is not
 * {@link io.neba.api.annotations.ResourceParam#required() required}, an exception will be thrown
 * if the parameter is missing, the path is unresolvable or the resource cannot be adapted to the desired model,
 * i.e. the resulting model instance is guaranteed not to be <code>null</code>.
 *
 * @author Olaf Otto
 */
public class ResourceParamArgumentResolver implements HandlerMethodArgumentResolver {
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return getParameterAnnotation(parameter) != null;
    }

    private ResourceParam getParameterAnnotation(MethodParameter parameter) {
        return parameter.getParameterAnnotation(ResourceParam.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                 ModelAndViewContainer container,
                                 NativeWebRequest webRequest,
                                 WebDataBinderFactory binderFactory) throws Exception {

        final Object nativeRequest = webRequest.getNativeRequest();

        if (!(nativeRequest instanceof SlingHttpServletRequest)) {
            throw new IllegalStateException("Expected a " + SlingHttpServletRequest.class.getName() +
                                            " request, but got: " + nativeRequest + ".");
        }

        final SlingHttpServletRequest request = (SlingHttpServletRequest) nativeRequest;
        final ResourceParam resourceParam = getParameterAnnotation(parameter);
        final String parameterName = resolveParameterName(parameter, resourceParam);
        final boolean required = resourceParam.required() && isEmpty(resourceParam.defaultValue());
        final String resourcePath = resolveResourcePath(request, resourceParam, parameterName, required);

        if (resourcePath == null) {
            return null;
        }

        // We must resolve (and not use getResource()) as the resource path may be mapped.
        ResourceResolver resolver = request.getResourceResolver();
        Resource resource = resolver.resolve(request, resourcePath);

        if (resource == null || isNonExistingResource(resource)) {
            if (required) {
                throw new UnresolvableResourceException("Unable to resolve resource " + resourcePath +
                                                        " for the required parameter '" + parameterName + "'.");
            }
            return null;
        }

        if (parameter.getParameterType().isAssignableFrom(Resource.class)) {
            return resource;
        }

        Object adapted = resource.adaptTo(parameter.getParameterType());
        if (adapted == null && required) {
                throw new MissingAdapterException("Unable to adapt " + resource + " to " + parameter.getParameterType() +
                                                  " for required parameter '" + parameterName + "'.");
        }

        return adapted;
    }

    private String resolveResourcePath(SlingHttpServletRequest request,
                                       ResourceParam resourceParam,
                                       String parameterName,
                                       boolean required) throws MissingServletRequestParameterException {

        String resourcePath = request.getParameter(parameterName);
        if (isEmpty(resourcePath)) {
            resourcePath = resourceParam.defaultValue();
        }

        if (isEmpty(resourcePath)) {
            if (required) {
                throw new MissingServletRequestParameterException(parameterName, String.class.getSimpleName());
            }
            return null;
        }

        if (!isEmpty(resourceParam.append())) {
            resourcePath += resourceParam.append();
        }
        return resourcePath;
    }

    private String resolveParameterName(MethodParameter parameter, ResourceParam param) {
        String parameterName = param.value();
        if (isEmpty(parameterName)) {
            parameterName = parameter.getParameterName();
        }
        return parameterName;
    }
}
