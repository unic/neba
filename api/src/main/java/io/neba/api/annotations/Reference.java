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
 * Marks a field as containing a reference, i.e. a path to another resource.
 * Example: The property "searchPage" contains the path
 * "/content/mysite/de/search" (a String value). without the &#64;{@link Reference} annotation,
 * the property could be mapped like so:
 * 
 * <p>
 * 
 * <pre>
 * &#64;{@link ResourceModel}("my/resource/type")
 * public class MyModel {
 *     private String searchPage;
 *     ...
 * }
 * </pre>
 * 
 * </p>
 * 
 * However, if one wants to obtain a model representing the referenced search
 * page, e.g. "SearchPage", one would have to obtain a
 * {@link org.apache.sling.api.resource.ResourceResolver}, get the
 * {@link org.apache.sling.api.resource.Resource} using the "searchPage"
 * property value, check for null and finally
 * {@link org.apache.sling.api.resource.Resource#adaptTo(Class) adapt} the
 * {@link org.apache.sling.api.resource.Resource} to the "SearchPage" model:
 * 
 * <p>
 * 
 * <pre>
 * &#64;{@link ResourceModel}("my/resource/type")
 * public class MyModel {
 *     private String searchPage;
 *     &#64;{@link This}
 *     private Resource resource;
 *     
 *     public SearchPage getSearchPage() {
 *          SearchPage page = null;
 *          if (!isBlank(this.searchPage)) {
 *             Resource pageResource = this.resource.getResourceResolver().get(this.searchPage);
 *             if (pageResource != null) {
 *                page = pageResource.adaptTo(SearchPage.class);
 *             }
 *          }
 *          return page;
 *     }
 * }
 * </pre>
 * 
 * </p>
 * 
 * This boilerplate code is no longer necessary when using the @{@link Reference} annotation:
 * 
 * <p>
 * 
 * <pre>
 * &#64;{@link ResourceModel}("my/resource/type")
 * public class MyModel {
 *    &#64;{@link Reference}
 *    private SearchPage searchPage;
 * }
 * </pre>
 * 
 * </p>
 * 
 * This will automatically
 * {@link org.apache.sling.api.resource.ResourceResolver#getResource(String)
 * get} the {@link org.apache.sling.api.resource.Resource} denoted by the string
 * value "searchPage" and
 * {@link org.apache.sling.api.resource.Resource#adaptTo(Class) adapt it} to the
 * "SearchPage" model. One can also use this annotation in conjunction with the &#64;
 * {@link Path} annotation:
 * <p>
 * 
 * <pre>
 * ...
 * &#64;{@link Path}("some:searchPagePath")
 * &#64;{@link Reference}
 * private SearchPage searchPage;
 * </pre>
 * 
 * </p>
 * However, {@link Reference} is unnecessary if {@link Path} is absolute, since
 * an absolute path is always considered a reference to a
 * {@link org.apache.sling.api.resource.Resource}, thus the following works:
 * <p>
 * 
 * <pre>
 * ...
 * &#64;{@link Path}("/content/mysite/${language}/search")
 * private SearchPage searchPage;
 * </pre>
 * 
 * </p>
 * 
 * {@link java.util.Collection}, {@link java.util.List}, {@link java.util.Set}
 * and arrays of references are also supported. In this case, the corresponding
 * property ("pages" in the example below) must have the type String[].
 * 
 * <p>
 * 
 * <pre>
 * ...
 * &#64;{@link Reference}
 * private List&lt;Page&gt; pages;
 * </pre>
 * 
 * </p>
 * 
 * Note that upper bound generic types are not supported since the corresponding
 * collection would be read-only. Thus, the following does not work:
 * 
 * <p>
 * 
 * <pre>
 * ...
 * &#64;{@link Reference}
 * private List&lt;? extends Page&gt; pages;
 * </pre>
 * 
 * </p>
 * 
 * However, lower bound generic types are supported. Thus, the following does
 * work:
 * 
 * <p>
 * 
 * <pre>
 * ...
 * &#64;{@link Reference}
 * private List&lt;? super Page&gt; pages;
 * </pre>
 * 
 * </p>
 *
 * In case you want to alter the reference prior to resolution, e.g. to obtain specific children of the referenced resource,
 * you can modify the reference path(s) by providing a path segment that is appended to all reference paths, like so:
 *
 * <p>
 *
 * <pre>
 * ...
 * &#64;{@link Reference}(append = "jcr:content")
 * private List&lt;PageContent&gt; pageContents;
 * </pre>
 *
 * </p>
 *
 * Thus, if the reference(s) point to pages, e.g. /content/page/a, /jcr:content would be appended to the reference path, resulting in
 * /content/page/a/jcr:content to be resolved. The appended path is relative, thus appending a path of the form "../../xyz" is supported as well.
 *
 * @author Olaf Otto
 */
@Documented
@Target({FIELD, ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Reference {
    /**
     * Append this path segment to the reference path prior to resource resolution.
     */
    String append() default "";
}
