package io.neba.core.resourcemodels.mapping.testmodels;

import io.neba.api.annotations.Path;

import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * @author Olaf Otto
 */
@Retention(RUNTIME)
@Path("/absolute/path")
public @interface CustomAnnotationWithPathMetaAnnotation {
}
