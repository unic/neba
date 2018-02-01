package io.neba.core.resourcemodels.mapping;

import io.neba.api.spi.PlaceholderVariableResolver;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import java.util.ArrayList;
import java.util.Collection;

import static java.lang.System.getProperty;
import static java.lang.System.getenv;
import static org.osgi.service.component.annotations.ReferenceCardinality.MULTIPLE;
import static org.osgi.service.component.annotations.ReferencePolicy.DYNAMIC;

/**
 * @author Olaf Otto
 */
@Component(service = PlaceholderVariableResolvers.class)
public class PlaceholderVariableResolvers {
    private final Collection<PlaceholderVariableResolver> resolvers = new ArrayList<>();

    @Reference(
            cardinality = MULTIPLE,
            policy = DYNAMIC,
            unbind = "unbind")
    protected void bind(PlaceholderVariableResolver resolver) {
        this.resolvers.add(resolver);
    }

    @SuppressWarnings("unused")
    protected void unbind(PlaceholderVariableResolver resolver) {
        if (resolver == null) {
            return;
        }
        this.resolvers.remove(resolver);
    }

    /**
     * @param variableName must not be <code>null</code>.
     * @return the resolved value, or <code>null</code> if no value could be resolved.
     */
    String resolve(String variableName) {
        if (variableName == null) {
            throw new IllegalArgumentException("Method argument variableName must not be null");
        }

        for (PlaceholderVariableResolver resolver : resolvers) {
            String resolved = resolver.resolve(variableName);
            if (resolved != null) {
                return resolved;
            }
        }

        String resolved = getenv(variableName);
        if (resolved != null) {
            return resolved;
        }

        return getProperty(variableName, null);
    }
}
