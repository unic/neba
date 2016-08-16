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

package io.neba.core.util;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.*;

/**
 * @author Olaf Otto
 */
public final class ReflectionUtil {
    private static final int DEFAULT_COLLECTION_SIZE = 32;

    /**
     * Resolves the {@link Type} of the lower bound of a single type argument of a
     * {@link ParameterizedType}.
     * <p/>
     * <pre>
     *   private List&lt;MyModel&gt; myModel -&gt; MyModel.
     *   private Optional&lt;MyModel&gt; myModel -&gt; MyModel.
     * </pre>
     *
     * @param type        must not be <code>null</code>.
     * @return never null.
     */
    public static Type getLowerBoundOfSingleTypeParameter(Type type) {
        if (type == null) {
            throw new IllegalArgumentException("Method parameter type must not be null.");
        }

        // The generic type may contain the generic type declarations, e.g. List<String>.
        if (!(type instanceof ParameterizedType)) {
            throw new IllegalArgumentException("Cannot obtain the component type of " + type +
                    ", it does not declare generic type parameters.");
        }

        // Only the ParametrizedType contains reflection information about the actual type.
        ParameterizedType parameterizedType = (ParameterizedType) type;


        Type[] typeArguments = parameterizedType.getActualTypeArguments();

        // We expect exactly one argument representing the model type.
        if (typeArguments.length != 1) {
            signalUnsupportedNumberOfTypeDeclarations(type);
        }

        Type typeArgument = typeArguments[0];

        // Wildcard type <X ... Y>
        if (typeArgument instanceof WildcardType) {
            WildcardType wildcardType = (WildcardType) typeArgument;
            Type[] lowerBounds = wildcardType.getLowerBounds();
            if (lowerBounds.length == 0) {
                throw new IllegalArgumentException("Cannot obtain the generic type of " + type +
                        ", it has a wildcard declaration with an upper" +
                        " bound (<? extends Y>) and is thus read-only." +
                        " Only simple type parameters (e.g. <MyType>)" +
                        " or lower bound wildcards (e.g. <? super MyModel>)" +
                        " are supported.");
            }
            typeArgument = lowerBounds[0];
        }
        return typeArgument;
    }

    private static void signalUnsupportedNumberOfTypeDeclarations(Type type) {
        throw new IllegalArgumentException("Cannot obtain the component type of " + type +
                ", it must have exactly one parameter type, e.g. <MyModel>.");
    }

    /**
     * Provides an implementation instance for the desired collection type.
     * Example: If the collection type is {@link List}, a suitable list implementation is instantiated.
     *
     * @param collectionType must not be <code>null</code>.
     * @return never <code>null</code>. Throws an {@link IllegalStateException}
     * if the collection type is not supported.
     */
    @SuppressWarnings("unchecked")
    public static <K, T extends Collection<K>> Collection<K> instantiateCollectionType(Class<T> collectionType, int length) {
        if (collectionType == null) {
            throw new IllegalArgumentException("Method parameter collectionType must not be null.");
        }

        T collection;
        // This includes java.util.Collection
        if (collectionType.isAssignableFrom(List.class)) {
            collection = (T) new ArrayList<K>(length);
        } else if (collectionType.isAssignableFrom(Set.class)) {
            collection = (T) new LinkedHashSet<K>(length);
        } else {
            throw new IllegalArgumentException("Unable to instantiate a collection of type " + collectionType +
                    ". Only collections assignable from " + List.class + " or " + Set.class + " are supported.");
        }
        return collection;
    }

    /**
     * @param type must not be <code>null</code>.
     * @return whether the given collection type can be instantiated using {@link #instantiateCollectionType(Class)}.
     */
    public static boolean isInstantiableCollectionType(Class<?> type) {
        if (type == null) {
            throw new IllegalArgumentException("Method argument type must not be null.");
        }

        for (Class<?> supportedType : getInstantiableCollectionTypes()) {
            if (type.isAssignableFrom(supportedType)) {
                return true;
            }
        }

        return false;
    }

    /**
     * @return an array of the types a collection type
     * must be {@link Class#isAssignableFrom(Class) assignable from} in order to be
     * {@link #isInstantiableCollectionType(Class) instantiable}.
     */
    public static Class<?>[] getInstantiableCollectionTypes() {
        return new Class[]{List.class, Set.class};
    }

    /**
     * Creates an instance with a default capacity.
     *
     * @see #instantiateCollectionType(Class, int)
     */
    public static <K, T extends Collection<K>> Collection<K> instantiateCollectionType(Class<T> collectionType) {
        return instantiateCollectionType(collectionType, DEFAULT_COLLECTION_SIZE);
    }

    private ReflectionUtil() {
    }

}
