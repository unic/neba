/**
 * Copyright 2013 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 the "License";
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
**/

package io.neba.core.resourcemodels.adaptation;

import io.neba.core.resourcemodels.registration.ModelRegistry;
import io.neba.core.util.OsgiBeanSource;
import org.apache.sling.api.adapter.AdapterFactory;
import org.apache.sling.api.resource.Resource;
import org.eclipse.gemini.blueprint.context.BundleContextAware;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;

import static org.apache.commons.lang.ClassUtils.getAllInterfaces;
import static org.apache.commons.lang.ClassUtils.getAllSuperclasses;
import static org.apache.sling.api.adapter.AdapterFactory.ADAPTABLE_CLASSES;
import static org.apache.sling.api.adapter.AdapterFactory.ADAPTER_CLASSES;
import static org.osgi.framework.Bundle.ACTIVE;
import static org.osgi.framework.Bundle.STARTING;

/**
 * An {@link AdapterFactory} provides the {@link AdapterFactory#ADAPTABLE_CLASSES type(s) it adapts from}
 * and the {@link AdapterFactory#ADAPTER_CLASSES types it can adapt to} as OSGi service 
 * properties. This information is used by {@link org.apache.sling.api.adapter.Adaptable} types to
 * {@link org.apache.sling.api.adapter.Adaptable#adaptTo(Class) adapt to}
 * other types, i.e. is essentially a factory pattern.
 * <br /> 
 * This service registers the {@link ResourceToModelAdapter} as
 * an {@link AdapterFactory} OSGi service and dynamically updates the before mentioned
 * service properties with regard to the resource models detected by the
 * {@link io.neba.core.resourcemodels.registration.ModelRegistrar}.
 * This enables direct {@link Resource#adaptTo(Class) adaptation} to the resource
 * models without having to provide all available models as service metadata at build time.
 * 
 * @author Olaf Otto
 */
@Service
public class ResourceToModelAdapterUpdater implements BundleContextAware {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private ModelRegistry registry;
    @Autowired
    private ResourceToModelAdapter adapter;
    
    private BundleContext context = null;
    private ServiceRegistration resourceToModelAdapterRegistration = null;

    @Async("singlethreaded")
    public void refresh() {
        if (isModelAdapterUpdatable()) {
            updateModeAdapter();
        }
    }

    /**
     * Depending on the bundle lifecycle, an OSGi service may not always be 
     * updatable.
     * 
     * @return true if the {@link ResourceToModelAdapter} OSGi service may be altered.
     */
    private boolean isModelAdapterUpdatable() {
        int bundleState = this.context.getBundle().getState();
        return bundleState == ACTIVE || bundleState == STARTING;
    }

    /**
     * Sling does not detect changes to the state of an {@link AdapterFactory} service. Their 
     * properties are only read when the service is registered. Thus
     * the service is unregistered and re-registered when changing its properties
     * (e.g. adding new adaptable types).
     */
    private void updateModeAdapter() {
        unregisterModelAdapter();
        registerModelAdapter();
    }

    /**
     * {@link BundleContext#registerService(String, Object, Dictionary) Registers} 
     * the {@link ResourceToModelAdapter}, i.e. publishes it as an OSGi service.  
     */
    @PostConstruct
    public void registerModelAdapter() {
        Dictionary<String, Object> properties = createResourceToModelAdapterProperties();
        String serviceClassName = AdapterFactory.class.getName();
        this.resourceToModelAdapterRegistration = this.context.registerService(serviceClassName, this.adapter, properties);
    }

    private void unregisterModelAdapter() {
        try {
            this.resourceToModelAdapterRegistration.unregister();    
        } catch (IllegalStateException e) {
            this.logger.info("The resource to model adapter was already unregistered, ignoring.", e);
        }
    }

    private Dictionary<String, Object> createResourceToModelAdapterProperties() {
        Dictionary<String, Object> properties = new Hashtable<>();
        Set<String> fullyQualifiedNamesOfRegisteredModels = getAdapterTypeNames();
        properties.put(ADAPTER_CLASSES, fullyQualifiedNamesOfRegisteredModels.toArray());
        properties.put(ADAPTABLE_CLASSES, new String[] { Resource.class.getName() });
        properties.put("service.vendor", "neba.io");
        properties.put("service.description", "Adapts Resources to @ResourceModels.");
        return properties;
    }

    /**
     * Obtains all {@link OsgiBeanSource bean sources} from the
     * {@link io.neba.core.resourcemodels.registration.ModelRegistrar} and adds the {@link OsgiBeanSource#getBeanType()
     * model type name} as well as the type name of all of its superclasses and
     * interfaces to the set.
     * 
     * @return never null but rather an empty set.
     * 
     * @see org.apache.commons.lang.ClassUtils#getAllInterfaces(Class)
     * @see org.apache.commons.lang.ClassUtils#getAllSuperclasses(Class)
     */
    @SuppressWarnings("unchecked")
    private Set<String> getAdapterTypeNames() {
        List<OsgiBeanSource<?>> beanSources = this.registry.getBeanSources();
        Set<String> modelNames = new HashSet<>();
        for (OsgiBeanSource<?> source : beanSources) {
            Class<?> c = source.getBeanType();
            modelNames.add(c.getName());
            modelNames.addAll(toClassnameList(getAllInterfaces(c)));
            List<Class<?>> allSuperclasses = getAllSuperclasses(c);
            // Remove Object.class - it is always the topmost element.
            allSuperclasses.remove(allSuperclasses.size() - 1);
            modelNames.addAll(toClassnameList(allSuperclasses));
        }
        return modelNames;
    }

    private Collection<String> toClassnameList(List<Class<?>> l) {
        List<String> classNames = new ArrayList<>(l.size());
        classNames.addAll(l.stream().map(Class::getName).collect(Collectors.toList()));
        return classNames;
    }

    @Override
    public void setBundleContext(BundleContext bundleContext) {
        this.context = bundleContext;
    }
}