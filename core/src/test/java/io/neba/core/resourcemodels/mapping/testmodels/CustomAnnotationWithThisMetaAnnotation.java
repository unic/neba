package io.neba.core.resourcemodels.mapping.testmodels;

import io.neba.api.annotations.This;

import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * @author Olaf Otto
 */
@Retention(RUNTIME)
@This
public @interface CustomAnnotationWithThisMetaAnnotation {
}
