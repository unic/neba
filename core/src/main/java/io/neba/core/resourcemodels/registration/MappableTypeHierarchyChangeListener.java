package io.neba.core.resourcemodels.registration;

import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.sling.api.SlingConstants.PROPERTY_PATH;

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
 * Only valid cases handled herein. For instance, if a resource points to a sling:resourceType or sling:resourceSuperType, and
 * that type or super type resource is removed at runtime, this handler does not invalidate the cache as this represents an invalid
 * content state and is thus considered a programming error.
 * </p>
 *
 * @author Olaf Otto
 */
@Service
public class MappableTypeHierarchyChangeListener implements EventHandler {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final ExecutorService executorService = newSingleThreadExecutor();
    private final BlockingQueue<Object> invalidationRequests = new ArrayBlockingQueue<>(1);
    private boolean isShutDown = false;

    @Autowired
    private ModelRegistry modelRegistry;

    @PostConstruct
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

    @PreDestroy
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
