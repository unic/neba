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

package io.neba.core.util;

import org.apache.commons.lang3.reflect.TypeUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import static java.lang.reflect.Modifier.isPublic;
import static java.util.Arrays.asList;
import static java.util.Collections.addAll;

/**
 * @author Olaf Otto
 */
public final class ReflectionUtil {
    private static final int DEFAULT_COLLECTION_SIZE = 32;

    /**
     * Resolves the {@link Type} of the lower, or, if no lower bound is present, the upper bound of a single type argument of a
     * {@link ParameterizedType}.
     * <p/>
     * <pre>
     *   private List&lt;MyModel&gt; myModel -&gt; MyModel.
     *   private List&lt;? extends MyModel&gt; myModel -&gt; MyModel.
     *   private List&lt;? super MyModel&gt; myModel -&gt; MyModel.
     *   private List&lt;T&gt; myModel | T extends MyModel -&gt; MyModel.
     *   private Optional&lt;MyModel&gt; myModel -&gt; MyModel.
     * </pre>
     *
     * @param type must not be <code>null</code>.
     * @return never null.
     */
    public static Type getBoundaryOfParametrizedType(final Type type, final Type assigningType) {
        if (type == null) {
            throw new IllegalArgumentException("Method parameter type must not be null.");
        }

        // Only the ParametrizedType contains reflection information about the actual type.
        if (!(type instanceof ParameterizedType)) {
            throw new IllegalArgumentException("Cannot obtain the component type of " + type +
                    ", it does not declare generic type parameters.");
        }
        ParameterizedType parameterizedType = (ParameterizedType) type;
        Type[] typeArguments = parameterizedType.getActualTypeArguments();

        // We expect exactly one argument representing the model type.
        if (typeArguments.length != 1) {
            signalUnsupportedNumberOfTypeDeclarations(type);
        }

        Type resolvedType = typeArguments[0];

        // Wildcard type <X ... Y>
        if (resolvedType instanceof WildcardType) {
            resolvedType = resolveWildCard((WildcardType) resolvedType, assigningType);
        }

        if (resolvedType instanceof TypeVariable) {
            resolvedType = resolveTypeVariable((TypeVariable<?>) resolvedType, assigningType);
        }

        if (resolvedType == null) {
            throw new IllegalArgumentException("Cannot obtain the generic type of " + type +
                    ", it has a generic signature with no resolvable lower or upper boundary. " +
                    "Either a specific type, e.g. List<MyType>, a lower bound, e.g. List<? super MyType> " +
                    "or an upper bound, e.g. List<? extends MyType> must be resolvable for the generic signature.");
        }

        return resolvedType;
    }

    private static Type resolveTypeVariable(final TypeVariable<?> variable, final Type assigningType) {
        // Can the type variable be resolved?
        if (TypeUtils.getRawType(variable, assigningType) == null) {
            // It cannot be resolved - we must look at the boundaries.
            Type[] bounds = variable.getBounds();

            // Type variables only have upper bounds. There are now five options:

            // 1: There are multiple boundaries, e.g. <? extends A & B>, which means the type cannot be derived. Bail.
            if (bounds.length != 1) {
                return null;
            }

            // 2: There is one boundary, and it is java.lang.Object. This means there is no boundary. Bail.
            if (bounds[0] == Object.class) {
                return null;
            }

            // 3: There is one boundary, and it's a Type Variable. Repeat.
            if (bounds[0] instanceof TypeVariable) {
                return resolveTypeVariable((TypeVariable<?>) bounds[0], assigningType);
            }

            // 4: There is one boundary, and it's a Parametrized or wildcard type. Return the respective type, e.g. List<? extends SomeType<V>> -> SomeType
            if (bounds[0] instanceof WildcardType) {
                return resolveWildCard((WildcardType) bounds[0], assigningType);
            }

            // 5: At this point, we know there is exactly one boundary, which is not a generic symbol and not java.lang.Object.
            //    Thus, it must be the effective type, e.g. List<? extends SpecificType> -> SpecificType
            return bounds[0];
        }

        return variable;
    }

    private static Type resolveWildCard(final WildcardType typeArgument, Type assigningType) {
        Type resolvedType = null;
        // Start with the lower boundary as it is the most specific
        Type[] boundaries = typeArgument.getLowerBounds();
        if (boundaries.length != 0) {
            resolvedType = boundaries[0];
        } else {
            // Fall back to the upper bounds
            boundaries = typeArgument.getUpperBounds();
            if (boundaries.length != 0) {
                resolvedType = boundaries[0];
            }
        }

        if (resolvedType == null || resolvedType == Object.class) {
            return null;
        }

        if (resolvedType instanceof WildcardType) {
            resolvedType = resolveWildCard((WildcardType) resolvedType, assigningType);
        }

        if (resolvedType instanceof TypeVariable) {
            resolvedType = resolveTypeVariable((TypeVariable<?>) resolvedType, assigningType);
        }

        return resolvedType;
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

    /**
     * All methods of the given type, its super types and interfaces, starting with the methods of
     * the given type.
     *
     * @param type must not be <code>null</code>.
     * @return never <code>null</code> but rather an empty list.
     */
    public static List<Method> methodsOf(Class<?> type) {
        if (type == null) {
            throw new IllegalArgumentException("Method argument type must not be null");
        }
        List<Method> methods = new LinkedList<>();
        Queue<Class<?>> classes = new LinkedList<>();
        classes.add(type);
        while (!classes.isEmpty()) {
            Class<?> c = classes.poll();
            methods.addAll(asList(c.getDeclaredMethods()));
            if (c.getSuperclass() != null) {
                classes.add(c.getSuperclass());
            }
            addAll(classes, c.getInterfaces());
        }
        return methods;
    }

    /**
     * @param type must not be <code>null</code>.
     * @param name must not be <code>null</code>.
     * @return the {@link Class#getDeclaredFields() declared field} with the given name, or <code>null</code>
     */
    public static Field findField(Class<?> type, String name) {
        if (type == null) {
            throw new IllegalArgumentException("Method argument type must not be null");
        }
        if (name == null) {
            throw new IllegalArgumentException("Method argument name must not be null");
        }

        Class<?> c = type;

        do {
            for (Field f : c.getDeclaredFields()) {
                if (name.equals(f.getName())) {
                    return f;
                }
            }
            c = c.getSuperclass();
        } while (c != null);

        return null;
    }

    /**
     * Makes the {@link Field#setAccessible(boolean) accessible} only if it is not.
     *
     * @param field must not be <code>null</code>.
     * @return the given field.
     */
    public static Field makeAccessible(Field field) {
        if (field == null) {
            throw new IllegalArgumentException("Method argument field must not be null.");
        }
        if (isInaccessible(field) && !field.isAccessible()) {
            field.setAccessible(true);
        }

        return field;
    }

    /**
     * Makes the {@link Method#setAccessible(boolean) accessible} only if it is not.
     *
     * @param method must not be <code>null</code>.
     * @return the given method.
     */
    public static Method makeAccessible(Method method) {
        if (method == null) {
            throw new IllegalArgumentException("Method argument method must not be null.");
        }

        if (isInaccessible(method) && !method.isAccessible()) {
            method.setAccessible(true);
        }

        return method;
    }

    private static boolean isInaccessible(Member member) {
        return !isPublic(member.getModifiers()) ||
                !isPublic(member.getDeclaringClass().getModifiers());
    }

    private ReflectionUtil() {
    }
}
