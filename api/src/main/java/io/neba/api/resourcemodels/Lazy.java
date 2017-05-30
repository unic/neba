package io.neba.api.resourcemodels;

/**
 * <p>
 * Declares lazy-loading 1:1 relationships in resource models.
 * </p>
 * <p>
 * NEBA will automatically detect <em>{@link Lazy}</em> fields and provide a suitable lazy-loading implementation.
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
 *     This interface <em>may</em> also be used to explicitly lazy-load collection-typed resource model relationships, such as
 *     {@link io.neba.api.annotations.Children} or {@link io.neba.api.annotations.Reference} collections:
 * </p>
 * <pre>
 * &#64;{@link io.neba.api.annotations.ResourceModel}(types = "...")
 * public class A {
 *   &#64;{@link io.neba.api.annotations.Children}
 *   private Lazy&lt;List&lt;B&gt;&gt; children;
 * }
 * </pre>
 * <p>
 *     However, collection-typed relationships are automatically provided as lazy-loading proxies, thus there usually is no
 *     reason to make them {@link Lazy}.
 * </p>
 *
 * @param <T> the type of the lazy-loaded object.
 * @author Olaf Otto
 */
public interface Lazy<T> {
    /**
     * @return an {@link java.util.Optional} representation of the lazy-loaded value, never <code>null</code>.
     */
    java.util.Optional<T> getValue();
}