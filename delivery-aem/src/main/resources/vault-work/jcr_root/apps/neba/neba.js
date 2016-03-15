/**
 * Provides the most specific resource model for the current resource. Can be used like so:
 *
 * <... data-sly-use.m="/apps/neba/neba.js" ...>
 * ${m.someProperty}
 *
 * To explicitly specify a bean name, use
 *
 * <... data-sly-use.m='${"/apps/neba/neba.js" @ beanName="name"}'...>
 *
 * @param beanName the optional specific name of the model bean to adapt the current resource to
 */
use(function () {
    // Java packages are accessed from JS using Packages.<FQCN>
    var service = sling.getService(Packages.io.neba.api.resourcemodels.ResourceModelProvider);
    return this.beanName == null ?
        service.resolveMostSpecificModel(resource) :
        service.resolveMostSpecificModelWithBeanName(resource, this.beanName);
});