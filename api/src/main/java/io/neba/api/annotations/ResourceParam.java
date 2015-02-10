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

package io.neba.api.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Marks a parameter of a Spring &#64;RequestMapping method as containing a resource path.
 * This path is automatically resolved by NEBA using the
 * {@link org.apache.sling.api.SlingHttpServletRequest#getResourceResolver() request's resource resolver}. If the annotated
 * parameter's type is not {@link Class#isAssignableFrom(Class) assignable from} {@link org.apache.sling.api.resource.Resource},
 * the resolved resource is {@link org.apache.sling.api.resource.Resource#adaptTo(Class) adapted} to the target type. Otherwise,
 * the resolved resource is provided.
 *
 * <p>
 *  Example:
 *  <pre>
 *  &#64;Controller
 *  &#64;RequestMapping(produces = "text/plain")
 *  public class MyController {
 *      &#64;RequestMapping("/echoTitle")
 *      &#64;ResponseBody
 *      public String echoTitle(&#64;{@link ResourceParam} Page page) {
 *          return page.getTitle();
 *      }
 *  }
 *  </pre>
 *
 *  In the above example, a request parameter "page" is expected. The parameter value is regarded as a
 *  resource path and the corresponding resource is
 *  {@link org.apache.sling.api.resource.ResourceResolver#resolve(javax.servlet.http.HttpServletRequest, String) resolved}.
 *  The resolved resource is subsequently adapted to "Page":
 *
 *  <pre>GET /bin/mvc.do/echoTitle?path=/content/site/en: "English page title".</pre>

 * </p>
 * <p>
 *     One may also append a segment to the path prior to resolution, for instance to target
 *     a sub resource of the provided path, like so:
 *  <pre>
 *      public String echoTitle(&#64;ResourceParam(append = "jcr:content") PageContent content) {
 *          return page.getTitle();
 *      }
 *  </pre>
 *
 *  The append path is also applied to the default value, if provided.
 * </p>
 *
 * By default, &#64;{@link ResourceParam} parameters are {@link ResourceParam#required() required}. If the parameter is missing,
 * cannot be resolved to an existing resource or cannot be adapted to the required target type, an exception is thrown.
 * Accordingly, the parameter provided to the controller method is guaranteed not to be <code>null</code> if it is required.
 * Consequently, the parameter may be null if either the parameter value is missing, cannot
 * be resolved to an existing resource or if the resolved resource cannot be adapted to the required target type
 * when the parameter is not {@link ResourceParam#required() required}. If a {@link #defaultValue() default value} is provided,
 * the parameter is always optional, regardless whether it is {@link #required() required}.
 * If the default value cannot be resolved, the parameter provided to the method is <code>null</code>.
 *
 * @author Olaf Otto
 */
@Retention(RUNTIME)
@Target({PARAMETER, ANNOTATION_TYPE})
@Documented
public @interface ResourceParam {
    /**
     * The name of the request parameter. Defaults to the
     * name of the annotated parameter.
     */
    String value() default "";

    /**
     * @return when <code>true</code>, the resolved parameter is guaranteed not to be <code>null</code>.
     */
    boolean required() default true;

    /**
     * @return if not <code>null</code> or empty, this default value is used in case no value is provided for this request parameter.
     *         Providing a default value implies {@link #required()} = false.
     */
    String defaultValue() default "";

    /**
     * Append this path segment to the provided path prior to resource resolution.
     */
    String append() default "";
}
