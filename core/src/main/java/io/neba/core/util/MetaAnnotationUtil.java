package io.neba.core.util;

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
 * {@link java.lang.reflect.AnnotatedElement}. Note that this utility does not cache
 * lookup results on should thus be used with care to avoid performance issues.
 *
 * @author Olaf Otto
 */
public class MetaAnnotationUtil {
    /**
     * @param element must not be <code>null</code>.
     * @param type must not be <code>null</code>.
     *
     * @return whether the given element or any of its meta-annotations is annotated with the given annotation type.
     */
    public static boolean isAnnotationPresent(AnnotatedElement element, Class<? extends Annotation> type) {
        if (element == null) {
            throw new IllegalArgumentException("Method argument element must not be null.");
        }
        if (type == null) {
            throw new IllegalArgumentException("Method argument type must not be null.");
        }
        return getAnnotations(element).get(type) != null;
    }

    /**
     * @param element must not be <code>null</code>.
     * @param type must not be <code>null</code>.
     *
     * @return the annotation if present on the given element or any meta-annotation thereof, or <code>null</code>.
     */
    @SuppressWarnings("unchecked")
    public static <T extends Annotation> T getAnnotation(AnnotatedElement element, Class<T> type) {
        if (element == null) {
            throw new IllegalArgumentException("Method argument element must not be null.");
        }
        if (type == null) {
            throw new IllegalArgumentException("Method argument type must not be null.");
        }

        return (T) getAnnotations(element).get(type);
    }


    /**
     * @param element must not be <code>null</code>.
     *
     * @return all annotations an meta-annotations present on the given element. Never <code>null</code> but rather an empty map.
     */
    public static Map<Class<? extends Annotation>, Annotation> getAnnotations(AnnotatedElement element) {
        if (element == null) {
            throw new IllegalArgumentException("Method argument element must not be null.");
        }

        HashMap<Class<? extends Annotation>, Annotation> annotations = new HashMap<Class<? extends Annotation>, Annotation>();
        Queue<Annotation> queue = new LinkedList<Annotation>(asList(element.getAnnotations()));
        while (!queue.isEmpty()) {
            Annotation annotation = queue.remove();
            // Prevent lookup loops (@A annotated with @B annotated with @A ...)
            if (!annotations.containsKey(annotation.annotationType())) {
                annotations.put(annotation.annotationType(), annotation);
                addAll(queue, annotation.annotationType().getAnnotations());
            }
        }
        return annotations;
    }

    private MetaAnnotationUtil() {}
}
