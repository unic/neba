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
 * @deprecated Using this script to automatically to adapt to the most specific model is deprecated
 *             as it introduces implicit and intransparent coupling between a view and a model. This coupling is
 *             easily broken by introducing a new model for the same resource type. This is almost impossible to foresee and
 *             hard to fix. Thus, instead of using this script, explicitly adapting the resource to the desired model is advised.
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
