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

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static org.springframework.beans.factory.config.BeanDefinition.SCOPE_PROTOTYPE;

/**
 * A resource model is a {@link Component} representing sling
 * {@link org.apache.sling.api.resource.Resource resources} with
 * specific sling resource types, JCR primary node types or mixin types.
 * <p>
 * Classes annotated with &#64;{@link ResourceModel} are subject to auto
 * detection by spring classpath scanning. The resulting bean definitions are
 * detected by NEBA. Resources with suitable types can then be
 * {@link org.apache.sling.api.resource.Resource#adaptTo(Class) adapted to}
 * the corresponding models and can automatically be published into the scripting
 * context as the model representing the current resource
 * using NEBA's defineObjects tag.
 * </p>
 * <p>
 * The fields of a &#64;{@link ResourceModel} are automatically mapped from the properties
 * of the {@link org.apache.sling.api.resource.Resource resource} it represents,
 * unless they are static, final, injected (annotated with &#64;Inject, &#64;Autowired or &#64;Resource) or
 * annotated with &#64;{@link Unmapped}. See also &#64;{@link Path}, &#64;{@link This}
 * and &#64;{@link Reference} and refer to the NEBA user guide.
 * </p>
 *
 * <h2>Examples</h2>
 * <p>
 * 
 * <pre>
 *    // A resource model for a sling resource type.
 *    &#64;{@link ResourceModel}(types = "shared/components/teaser")
 *    public class MyTeaser {
 *       ...
 *    }
 *    
 *    // A resource model for a JCR node type.
 *    &#64;{@link ResourceModel}(types = "nt:base")
 *    public class MyBaseModel {
 *       ...
 *    }
 *    
 *    // A resource model for a JCR mixin node type.
 *    &#64;{@link ResourceModel}(types = "mix:versionable")
 *    public class MyVersionableModel {
 *       ...
 *    }
 * </pre>
 * 
 * </p>
 * 
 * A {@link ResourceModel} applies to the <em>type hierarchy</em> of a resource.
 * Therefore, if a {@link org.apache.sling.api.resource.Resource} has no
 * specific {@link ResourceModel}, i.e. no mapping to its
 * {@link org.apache.sling.api.resource.Resource#getResourceType()} or node
 * type, its resource hierarchy, followed by its node type hierarchy (including mixin types) are
 * searched for a less specific model.<br />
 * Resource models have <em>prototype</em> scope since their values may be
 * mapped from a resource via content-to-object mapping (ocm) during
 * {@link org.apache.sling.api.resource.Resource#adaptTo(Class) adaptation}.
 * 
 * @author Olaf Otto
 * @see Component
 * @see org.apache.sling.api.resource.Resource#adaptTo(Class)
 */
@Scope(SCOPE_PROTOTYPE)
@Component
@Target({TYPE, ANNOTATION_TYPE})
@Retention(RUNTIME)
@Documented
public @interface ResourceModel {
    /**
     * @see Component#value()
     */
    String value() default "";

    /**
     * A {@link org.apache.sling.api.resource.Resource#getResourceType()
     * resource type} or JCR node type (including mixin types) are acceptable
     * values for the type mapping. Example: "myapps/components/mycomponent",
     * "nt:base", "mix:versionable".
     */
    String[] types();
}
