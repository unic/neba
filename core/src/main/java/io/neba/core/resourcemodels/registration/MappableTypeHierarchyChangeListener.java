/*
  Copyright 2013 the original author or authors.

  Licensed under the Apache License, Version 2.0 the "License";
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
*/
package io.neba.core.resourcemodels.registration;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;


import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.sling.api.SlingConstants.PROPERTY_PATH;
import static org.apache.sling.api.SlingConstants.TOPIC_RESOURCE_CHANGED;
import static org.osgi.service.event.EventConstants.EVENT_FILTER;
import static org.osgi.service.event.EventConstants.EVENT_TOPIC;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Listens for <em>resource property</em> changes potentially altering the resource type -&gt; model relationships. Invalidates the
 * {@link ModelRegistry} accordingly.
 * <p>
 * <p>
 * In Sling, the sling type hierarchy of a resource is defined by multiple attributes and can
 * be either explicit or implicit. For instance, a resource <em>may</em> explicitly specify a sling:resourceSuperType <em>or</em> the super type
 * could be derived implicitly from the sling:resourceSuperType of the type the resource's sling:resourceType property is
 * referring to, the former taking precedence. In addition, resources may specify <em>mixin types</em>, which can be added and removed dynamically.
 * </p>
 * <p>
 * Consequently, cached type hierarchy state must be cleared when these attributes change. This is what this event handler is responsible for.
 * <p>
 * <p>
 * Only valid cases are handled here. For instance, if a resource points to a sling:resourceType or sling:resourceSuperType, and
 * that type or super type resource is removed at runtime, this handler does not invalidate the cache as this represents an invalid
 * content state and is thus considered a programming error.
 * </p>
 *
 * @author Olaf Otto
 */
@Service(EventHandler.class)
@Component
@Properties({
        @Property(name = EVENT_TOPIC, value = TOPIC_RESOURCE_CHANGED),
        /*
         * React to changes potentially altering the cacheable resource type hierarchy, unless they are occurring
         * in a location known not to contain data relevant to the type hierarchy, such as /var or /content
         */
        @Property(name = EVENT_FILTER, value =
                "(&" +
                " (!(path=/content/*))" +
                " (!(path=/var/*))" +
                " (!(path=/jcr:*))" +
                " (!(path=/oak:*))" +
                " (|" +
                "  (resourceAddedAttributes=jcr:mixinTypes)" +
                "  (resourceAddedAttributes=sling:resourceSuperType)" +
                "  (resourceChangedAttributes=jcr:mixinTypes)" +
                "  (resourceChangedAttributes=sling:resourceType)" +
                "  (resourceChangedAttributes=sling:resourceSuperType)" +
                "  (resourceRemovedAttributes=sling:resourceSuperType)" +
                "  (resourceRemovedAttributes=jcr:mixinTypes)" +
                "))"),
        @Property(name = "service.description", value="An event handler invalidating cache resource type hierarchy information."),
        @Property(name = "service.vendor", value="neba.io")
})
public class MappableTypeHierarchyChangeListener implements EventHandler {
    private final Logger logger = getLogger(getClass());
    private final ExecutorService executorService = newSingleThreadExecutor();
    private final BlockingQueue<Object> invalidationRequests = new ArrayBlockingQueue<>(1);
    private boolean isShutDown = false;

    @Reference
    private ModelRegistry modelRegistry;

    @Activate
    protected void activate() {
        executorService.execute(() -> {
            while (!isShutDown) {
                try {
                    Object path = invalidationRequests.poll(5, SECONDS);
                    if (path != null) {
                        if (logger.isTraceEnabled()) {
                            logger.trace("Invalidating the resource model registry lookup cache due to changes to {}.", path);
                        }
                        modelRegistry.clearLookupCaches();
                    }
                } catch (InterruptedException e) {
                    if (!isShutDown) {
                        logger.debug("The type hierarchy change listener got interrupted, but was not shut down.", e);
                    }
                }
            }
        });
    }

    @Deactivate
    protected void deactivate() {
        this.isShutDown = true;
        this.executorService.shutdownNow();
    }

    /**
     * A substantial number of events may reach this handler.
     * However, we only need to clear the cache once.
     * Thus, if there already is a pending invalidation,
     * further invalidating events are simply discarded.
     */
    @Override
    public void handleEvent(Event event) {
        invalidationRequests.offer(event.getProperty(PROPERTY_PATH));
    }
}
