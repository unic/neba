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
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.FIELD;

/**
 * Defines the relative or absolute path of a property or resource mapped to a
 * field. Can be used when the name of the field does not match the name of
 * the mapped child resource or property, e.g. in case namespaces are used, or
 * if an absolute or relative path is to be mapped. <br />
 * Example
 * <p>
 * 
 * <pre>
 * public class Model {
 *     &#064;Path(&quot;jcr:title&quot;)
 *     private String title;
 *     &#064;Path(&quot;/content/homepage&quot;)
 *     private Page homepage;
 *     &#064;Path(&quot;../toolbar&quot;)
 *     private Page toolbar;
 * }
 * </pre>
 * 
 * </p>
 * 
 * @author Olaf Otto
 */
@Documented
@Target({FIELD, ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Path {
    String value();
}
