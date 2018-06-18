/**
 * Provides the most specific resource model for the current resource. Can be used like so:
 *
 * <... data-sly-use.m="/apps/neba/neba.js" ...>
 * ${m.someProperty}
 *
 * To explicitly specify a model name, use
 *
 * <... data-sly-use.m='${"/apps/neba/neba.js" @ modelName="name"}'...>
 *
 * @param modelName the optional specific name of the model bean to adapt the current resource to
 */
use(function () {
    // Java packages are accessed from JS using Packages.<FQCN>
    var service = sling.getService(Packages.io.neba.api.services.ResourceModelResolver);

    if (service == null) {
        return null;
    }
    return this.modelName == null ?
        service.resolveMostSpecificModel(resource) :
        service.resolveMostSpecificModelWithName(resource, this.beanName);
});