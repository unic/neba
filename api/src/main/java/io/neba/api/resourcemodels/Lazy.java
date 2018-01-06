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
package io.neba.api.resourcemodels;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

/**
 * <p>
 * Declares lazy-loading 1:1 relationships in resource models. In addition, as lazy-loading always has to deal with nonexistent values, this
 * interface both features a {@link java.util.Optional} representation via {@link #asOptional()} and provides the same API semantics
 * as {@link java.util.Optional} for convenience.
 * </p>
 * <p>
 * NEBA automatically detects <em>{@link Lazy}</em> fields and provides a suitable lazy-loading implementation.
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
 *   private Lazy&lt;B&gt; b;
 * }
 * </pre>
 * <p>
 * This interface <em>may</em> also be used to explicitly lazy-load collection-typed resource model relationships, such as
 * {@link io.neba.api.annotations.Children} or {@link io.neba.api.annotations.Reference} collections:
 * </p>
 * <pre>
 * &#64;{@link io.neba.api.annotations.ResourceModel}(types = "...")
 * public class A {
 *   &#64;{@link io.neba.api.annotations.Children}
 *   private Lazy&lt;List&lt;B&gt;&gt; children;
 * }
 * </pre>
 * <p>
 * However, collection-typed relationships are automatically provided as lazy-loading proxies, thus there usually is no
 * reason to make them {@link Lazy}.
 * </p>
 *
 * @param <T> the type of the lazy-loaded object.
 * @author Olaf Otto
 */
public interface Lazy<T> {
    /**
     * @return a {@link java.util.Optional} representation of the lazy-loaded value, never <code>null</code>.
     */
    @Nonnull
    java.util.Optional<T> asOptional();

    /**
     * {@see java.util.Optional#get}
     */
    @Nonnull
    default T get() {
        return asOptional().get();
    }

    /**
     * {@see java.util.Optional#isPresent}
     */
    default boolean isPresent() {
        return asOptional().isPresent();
    }

    /**
     * {@see java.util.Optional#ifPresent}
     */
    default void ifPresent(Consumer<? super T> c) {
        asOptional().ifPresent(c);
    }

    /**
     * {@see java.util.Optional#filter}
     */
    @Nonnull
    default Lazy<T> filter(Predicate<? super T> predicate) {
        return () -> asOptional().filter(predicate);
    }

    /**
     * {@see java.util.Optional#map}
     */
    @Nonnull
    default <U> Lazy<U> map(Function<? super T, ? extends U> f) {
        return () -> asOptional().map(f);
    }

    /**
     * {@see java.util.Optional#flatMap}
     */
    @Nonnull
    default <U> Lazy<U> flatMap(Function<? super T, Lazy<U>> f) {
        requireNonNull(f);
        if (!isPresent()) {
            return java.util.Optional::empty;
        } else {
            return requireNonNull(f.apply(get()));
        }
    }

    /**
     * {@see java.util.Optional#orElse}
     */
    @CheckForNull
    default T orElse(T other) {
        return asOptional().orElse(other);
    }

    /**
     * {@see java.util.Optional#orElseGet}
     */
    @CheckForNull
    default T orElseGet(Supplier<? extends T> other) {
        return asOptional().orElseGet(other);
    }

    /**
     * {@see java.util.Optional#orElseThrow}
     */
    @Nonnull
    default <X extends Throwable> T orElseThrow(Supplier<? extends X> exceptionSupplier) throws X {
        return asOptional().<X>orElseThrow(exceptionSupplier);
    }
}