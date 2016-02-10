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

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.*;

import static io.neba.core.util.ReadOnlyIterator.readOnly;
import static java.util.Arrays.asList;
import static java.util.Collections.addAll;

/**
 * Supports meta-annotations by looking up annotations in the transitive
 * hull (annotations and their annotations, called meta-annotations) of a given
 * {@link java.lang.reflect.AnnotatedElement}.
 *
 * @author Olaf Otto
 */
public class Annotations implements Iterable<Annotation> {
    private final AnnotatedElement annotatedElement;
    private Map<Class<? extends Annotation>, Annotation> annotations = null;

    /**
     * @param annotatedElement must not be <code>null</code>
     * @return never null.
     */
    public static Annotations annotations(AnnotatedElement annotatedElement) {
        if (annotatedElement == null) {
            throw new IllegalArgumentException("Method argument annotatedElement must not be null.");
        }
        return new Annotations(annotatedElement);
    }

    /**
     * @param annotatedElement must not be <code>null</code>.
     */
    public Annotations(AnnotatedElement annotatedElement) {
        if (annotatedElement == null) {
            throw new IllegalArgumentException("Constructor parameter annotatedElement must not be null.");
        }
        this.annotatedElement = annotatedElement;
    }

    /**
     * @param type must not be <code>null</code>.
     * @return whether the given element or any of its meta-annotations is annotated with the given annotation type.
     */
    public boolean contains(Class<? extends Annotation> type) {
        if (type == null) {
            throw new IllegalArgumentException("Method argument type must not be null.");
        }
        return getAnnotationMap().get(type) != null;
    }

    /**
     * @param name must not be <code>null</code>.
     * @return whether the given type name matches one of the present annotations
     */
    public boolean containsName(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Method argument name must not be null.");
        }
        for (Class<? extends  Annotation> annotationType : getAnnotationMap().keySet()) {
            if (annotationType.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param type must not be <code>null</code>.
     * @return the annotation if present on the given element or any meta-annotation thereof, or <code>null</code>.
     */
    @SuppressWarnings("unchecked")
    public <T extends Annotation> T get(Class<T> type) {
        if (type == null) {
            throw new IllegalArgumentException("Method argument type must not be null.");
        }

        return (T) getAnnotationMap().get(type);
    }

    /**
     * @return all annotations and meta-annotations present on the element. Never <code>null</code> but rather an empty map.
     */
    private Map<Class<? extends Annotation>, Annotation> getAnnotationMap() {
        if (this.annotations == null) {
            // We do not care about calculating the same thing twice in case of concurrent access.
            HashMap<Class<? extends Annotation>, Annotation> annotations = new HashMap<>();
            Queue<Annotation> queue = new LinkedList<>(asList(this.annotatedElement.getAnnotations()));
            while (!queue.isEmpty()) {
                Annotation annotation = queue.remove();
                // Prevent lookup loops (@A annotated with @B annotated with @A ...)
                if (!annotations.containsKey(annotation.annotationType())) {
                    annotations.put(annotation.annotationType(), annotation);
                    addAll(queue, annotation.annotationType().getAnnotations());
                }
            }
            this.annotations = annotations;
        }

        return this.annotations;
    }

    @Override
    public Iterator<Annotation> iterator() {
        return readOnly(getAnnotationMap().values().iterator());
    }

    public Map<Class<? extends Annotation>, Annotation> getAnnotations() {
        return new HashMap<>(getAnnotationMap());
    }
}
