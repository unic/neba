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

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Can be used to specify an OSGi service filter to narrow the service(s) that shall be
 * injected, for example:
 *
 * <pre>
 * &#064;{@link javax.inject.Inject}
 * &#064;{@link Filter}("(&amp;(property=value)(otherProperty=otherValue))")
 * private SomeService service;
 * </pre>
 */
@Retention(RUNTIME)
@Target({PARAMETER, ANNOTATION_TYPE})
public @interface Filter {
    String value();
}
