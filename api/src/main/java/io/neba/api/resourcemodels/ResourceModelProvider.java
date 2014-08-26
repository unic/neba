package io.neba.api.resourcemodels;

import org.apache.sling.api.resource.Resource;

/**
 * @author Olaf Otto
 */
public interface ResourceModelProvider {
    /**
     * @param resource must not be <code>null</code>
     * @param beanName must not be <code>null</code>
     * @return the most specific model bean instance compatible with the
     *         given resource's resource type, or <code>null</code>. The
     *         model stems from a bean who's name matches the given bean name.
     */
    Object resolveMostSpecificModelWithBeanName(Resource resource, String beanName);

    /**
     * @param resource must not be <code>null</code>.
     * @return the most specific model for the given resource, or <code>null</code> if
     *         there is no unique most specific model. Models for base types such as nt:usntructured
     *         or nt:base are not considered.
     */
    Object resolveMostSpecificModel(Resource resource);

    /**
     * @param resource must not be <code>null</code>.
     * @return the most specific model for the given resource, or <code>null</code> if
     *         there is no unique most specific model. Models for base types such as nt:unstructured
     *         or nt:base are considered.
     */
    Object resolveMostSpecificModelIncludingModelsForBaseTypes(Resource resource);
}
