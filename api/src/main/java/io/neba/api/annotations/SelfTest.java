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

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Methods annotated with {@link SelfTest} are considered selftests of a service
 * component and are automatically detected.<br />
 * Selftest methods <em>must</em> adhere to the following conventions: <br />
 * 
 * <ol>
 * <li>They must be applied to a public void method without arguments</li>
 * <li>The annotated bean must have application scope (i.e. be a singleton)</li>
 * <li>If the test fails, the method throws an exception. The exception should
 * include a message explaining the type of test failure.</li>
 * <li>If the test succeeds, the method does not throw an exception.</li>
 * </ol>
 * <br />
 * Example: <br />
 * 
 * <pre>
 * &#064;Service
 * public class MyService {
 *     &#064;{@link SelfTest}(&quot;Test something very important&quot;)
 *     public void testSomething() {
 *         if (somethingFails()) {
 *             throw new RuntimeException(&quot;Something failed, check...&quot;);
 *         }
 *     }
 * }
 * 
 * </pre>
 * 
 * @author Olaf Otto
 */
@Documented
@Target(METHOD)
@Retention(RUNTIME)
public @interface SelfTest {
    /**
     * Short description of the test, e.g. &quot;Mailserver connectivity
     * test&quot;.
     */
    String value();

    /**
     * Text displayed when the test has failed. Should contain an advice on how
     * to proceed with failure analysis.
     */
    String failure() default "The test has failed.";

    /**
     * Text displayed when the test has succeeded.
     */
    String success() default "The test was successful.";
}
