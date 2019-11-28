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
package io.neba.spring.resourcemodels.registration;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import static java.util.Arrays.asList;
import static java.util.Collections.addAll;

/**
 * Supports meta-annotations by looking up annotations in the transitive
 * hull (annotations and their annotations, called meta-annotations) of a given
 * {@link AnnotatedElement}.
 *
 * @author Olaf Otto
 */
public class Annotations {
    private final AnnotatedElement annotatedElement;
    private Map<Class<? extends Annotation>, Annotation> annotations = null;

    /**
     * @param annotatedElement must not be <code>null</code>
     * @return never null.
     */
    static Annotations annotations(AnnotatedElement annotatedElement) {
        if (annotatedElement == null) {
            throw new IllegalArgumentException("Method argument annotatedElement must not be null.");
        }
        return new Annotations(annotatedElement);
    }

    /**
     * @param annotatedElement must not be <code>null</code>.
     */
    private Annotations(AnnotatedElement annotatedElement) {
        if (annotatedElement == null) {
            throw new IllegalArgumentException("Constructor parameter annotatedElement must not be null.");
        }
        this.annotatedElement = annotatedElement;
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
}
