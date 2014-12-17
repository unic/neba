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
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Marks a field as representing the {@link org.apache.sling.api.resource.Resource resource}
 * represented by the current {@link ResourceModel}.
 * <br />
 * If the field's type is not {@link org.apache.sling.api.resource.Resource}, the
 * resource is {@link org.apache.sling.api.resource.Resource#adaptTo(Class) adapted}
 * to the type of the field.<br />
 * Example:
 * <p>
 * <pre>
 * &#64;{@link ResourceModel}(types = "my/resource/type")
 * public class MyModel {
 *     &#64;{@link This}   
 *     private {@link org.apache.sling.api.resource.Resource} resource;
 *     
 *     &#64;{@link This}
 *     private OtherModelForThisResourceType otherModel;
 * }
 * </pre>
 * </p>
 * @author Olaf Otto
 */
@Documented
@Retention(RUNTIME)
@Target({FIELD, ANNOTATION_TYPE})
public @interface This {
}