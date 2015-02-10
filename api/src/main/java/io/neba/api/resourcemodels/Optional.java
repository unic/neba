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
package io.neba.api.resourcemodels;

import java.util.NoSuchElementException;

/**
 * <p>
 * This value-holder interface is used to declare lazy-loading 1:1 relationships in resource models. It is designed
 * to be API-compatible to the <a href="http://docs.oracle.com/javase/8/docs/api/java/util/Optional.html">JAVA 8 "Optional" interface</a>
 * to allow leveraging to the latter interface in the future.
 * </p>
 * <p>
 * NEBA will automatically detect <em>{@link Optional}</em> members and provide a suitable lazy-loading implementation.
 * Note that this interface is <em>not</em> required to lazy-load collections, as NEBA automatically
 * provides collection-typed members, such as {@link io.neba.api.annotations.Reference}
 * or {@link io.neba.api.annotations.Children} collections, as lazy-loading proxies.
 * </p>
 * <p>
 * To declare a lazy reference from resource model "A" to resource model "B", write:
 * </p>
 * <pre>
 * &#64;{@link io.neba.api.annotations.ResourceModel}(types = "...")
 * public class A {
 *   &#64;{@link io.neba.api.annotations.Reference}
 *   private Optional&lt;B&gt; b;
 * }
 * </pre>
 * <p>
 *     This interface <em>may</em> also be used to explicitly lazy-load collection-typed resource model relationships, such as
 *     {@link io.neba.api.annotations.Children} or {@link io.neba.api.annotations.Reference} collections:
 * </p>
 * <pre>
 * &#64;{@link io.neba.api.annotations.ResourceModel}(types = "...")
 * public class A {
 *   &#64;{@link io.neba.api.annotations.Children}
 *   private Optional&lt;List&lt;B&gt;&gt; children;
 * }
 * </pre>
 * <p>
 *     However, collection-typed relationships are automatically provided as lazy-loading proxies, thus there usually is no
 *     reason to make them {@link io.neba.api.resourcemodels.Optional}.
 * </p>
 *
 * @param <T> the type of the lazy-loaded object.
 * @author Olaf Otto
 */
public interface Optional<T> {
    /**
     * @return the non-<code>null</code> value, or throws a {@link java.util.NoSuchElementException} if no value exists.
     * @throws NoSuchElementException if no value exists.
     */
    T get() throws NoSuchElementException;

    /**
     * @param defaultValue can be <code>null</code>.
     * @return the value if non-<code>null</code>, otherwise the default value, which can be <code>null</code>.
     */
    T orElse(T defaultValue);

    /**
     * @return <code>true</code> if the value is non-<code>null</code>. This method attempts to load the value. It
     * is equivalent to invoking {@link #orElse(Object)} with a <code>null</code> argument and checking
     * whether the returned value is non-<code>null</code>.
     */
    boolean isPresent();
}
