/*
  Copyright 2013 the original author or authors.

  Licensed under the Apache License, Version 2.0 the "License";
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
*/

package io.neba.api.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * <p>
 * Methods of a {@link ResourceModel} with this annotation are invoked after
 * all properties have been mapped from the model's resource.
 * If multiple methods are annotated, all of them are executed in no particular order. Inherited annotated
 * methods are executed as well, though after the execution of the callbacks on the child class.
 * </p>
 * <p>
 * Example:
 * <pre>
 *     &#064;{@link ResourceModel}
 *     public class MyModel {
 *          &#064;{@link This}
 *          private Resource resource;
 *
 *          &#064;{@link AfterMapping}
 *          public void initializeSomethingElse() {
 *              // Use the mapped member
 *              this.resource.getResourceResolver().resolve(...);
 *              //...
 *          }
 *          &#064;{@link AfterMapping}
 *          private void anotherMethodThatRequiresMappedProperties() throws Exception {
 *              ...
 *          }
 *     }
 * </pre>
 * <p>
 * When an exception arises during the invocation of an after-mapping method, a {@link ExceptionInAfterMappingMethod} will
 * be thrown. If the method itself is inaccessible - e.g. due to a security manager issue - an {@link IllegalStateException} is thrown instead.
 * </p>
 * @author Olaf Otto
 * @since 5.0.0
 */
@Documented
@Retention(RUNTIME)
@Target({METHOD, ANNOTATION_TYPE})
public @interface AfterMapping {

    /**
     * Represents an issue when invoking a model method annotated with {@link AfterMapping}.
     */
    class ExceptionInAfterMappingMethod extends RuntimeException {
        private static final long serialVersionUID = -4551759236956505577L;

        public ExceptionInAfterMappingMethod(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
