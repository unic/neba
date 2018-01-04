package io.neba.core.resourcemodels.mapping;

import io.neba.api.spi.PlaceholderVariableResolver;
import java.util.ArrayList;
import java.util.Collection;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.References;
import org.apache.felix.scr.annotations.Service;


import static java.lang.System.getProperty;
import static java.lang.System.getenv;
import static org.apache.felix.scr.annotations.ReferenceCardinality.OPTIONAL_MULTIPLE;
import static org.apache.felix.scr.annotations.ReferencePolicy.DYNAMIC;

/**
 * @author Olaf Otto
 */
@Service(PlaceholderVariableResolvers.class)
@Component
@References({
        @Reference(referenceInterface = PlaceholderVariableResolver.class,
                cardinality = OPTIONAL_MULTIPLE,
                policy = DYNAMIC,
                name = "resolvers",
                bind = "bind",
                unbind = "unbind")
})
public class PlaceholderVariableResolvers {
    private final Collection<PlaceholderVariableResolver> resolvers = new ArrayList<>();

    protected void bind(PlaceholderVariableResolver resolver) {
        this.resolvers.add(resolver);
    }

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
