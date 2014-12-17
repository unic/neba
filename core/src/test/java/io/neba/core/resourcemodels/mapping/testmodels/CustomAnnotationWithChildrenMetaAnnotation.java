package io.neba.core.resourcemodels.mapping.testmodels;

import io.neba.api.annotations.Children;

import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * @author Olaf Otto
 */
@Retention(RUNTIME)
@Children
public @interface CustomAnnotationWithChildrenMetaAnnotation {
}
