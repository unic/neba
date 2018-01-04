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
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * A resource model represents sling {@link org.apache.sling.api.resource.Resource resources} with
 * specific sling resource types, JCR primary node types or mixin types.
 *
 * <h2>Registering resource models</h2>
 * Classes annotated with &#64;{@link ResourceModel} are automatically detected by NEBA when their containing packages
 * are specified in the <code>Neba-packages</code> bundle manifest header, e.g.
 *
 * <p>
 * <pre>
 * &lt;Neba-Packages&gt;
 * com.acme.app.models,
 * com.acme.app.more.models
 * &lt;/Neba-Packages&gt;
 * </pre>
 * </p>
 *
 * When using NEBA with Spring, models can be declared as Spring beans. In this case, NEBA will automatically detect all beans
 * annotated with {@link ResourceModel} in the application context. Spring beans representing resource models <em>must be prototypically scoped</em> as they represent
 * dynamically created, contextual pieces of data, not singletons.
 *
 * <h2>Obtaining model instances</h2>
 * Resources with suitable types can be {@link org.apache.sling.api.resource.Resource#adaptTo(Class) adapted to}
 * the corresponding models. This adaptation can be done automatically using <code>/apps/neba/neba.js</code> with HTL, {@link io.neba.api.tags.DefineObjectsTag}
 * with JSP or the {@link io.neba.api.services.ResourceModelResolver} service.
 *
 * <h2>Mapping resource properties to models</h2>
 * The fields of a &#64;{@link ResourceModel} are automatically mapped from the properties
 * of the {@link org.apache.sling.api.resource.Resource resource} it represents,
 * unless they are static, final, injected (annotated with &#64;Inject, &#64;Autowired or &#64;Resource) or
 * annotated with &#64;{@link Unmapped}. See also &#64;{@link Path}, &#64;{@link This}
 * and &#64;{@link Reference} and refer to the NEBA user guide.
 *
 * <h2>Examples</h2>
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
 * <p>
 * A {@link ResourceModel} applies to the <em>type hierarchy</em> of a resource.
 * Therefore, if a {@link org.apache.sling.api.resource.Resource} has no
 * specific {@link ResourceModel}, i.e. no mapping to its
 * {@link org.apache.sling.api.resource.Resource#getResourceType()} or node
 * type, its resource hierarchy, followed by its node type hierarchy (including mixin types) are
 * searched for a less specific model.
 * </p>
 *
 * @author Olaf Otto
 * @see org.apache.sling.api.resource.Resource#adaptTo(Class)
 */
@Target({TYPE, ANNOTATION_TYPE})
@Retention(RUNTIME)
@Documented
public @interface ResourceModel {
    /**
     * A {@link org.apache.sling.api.resource.Resource#getResourceType()
     * resource type} or JCR node type (including mixin types) are acceptable
     * values for the type mapping. Example: "myapps/components/mycomponent",
     * "nt:base", "mix:versionable".
     */
    String[] types();
}
