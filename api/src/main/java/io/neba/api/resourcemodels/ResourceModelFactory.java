package io.neba.api.resourcemodels;

import io.neba.api.annotations.ResourceModel;
import java.util.Collection;
import javax.annotation.Nonnull;

/**
 * Represents a source for resource models. NEBA will register all services of this type to obtain the available resource models. The models
 * will be tied to the scope of the providing service.
 *
 * @author Olaf Otto
 */
public interface ResourceModelFactory {
    /**
     * @return A list of all {@link ModelDefinition model definitions} suitable for {@link #getModel(ModelDefinition) model} resolution.
     * Never <code>null</code> but rather an empty collections.
     */
    @Nonnull
    Collection<ModelDefinition> getModelDefinitions();

    /**
     * @param modelDefinition must not be <code>null</code>.
     * @return an instance of the model with the given name. Never <code>null</code>.
     */
    @Nonnull
    Object getModel(@Nonnull ModelDefinition modelDefinition);

    /**
     * @author Olaf Otto
     */
    interface ModelDefinition {
        @Nonnull
        ResourceModel getResourceModel();

        @Nonnull
        String getName();

        @Nonnull
        Class<?> getType();
    }
}
