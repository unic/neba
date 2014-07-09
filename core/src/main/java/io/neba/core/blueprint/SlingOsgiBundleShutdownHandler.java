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

package io.neba.core.blueprint;

import org.eclipse.gemini.blueprint.context.BundleContextAware;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.SynchronousBundleListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import static org.osgi.framework.BundleEvent.STOPPING;

/**
 * Dispatches a {@link org.osgi.framework.Bundle} shutdown event to the injected
 * services in a defined order.<br />
 * This is a {@link SynchronousBundleListener} since only these listeners are
 * notified when a bundle <em>is about to be stopped</em> in a synchronous
 * manner. This is required in order to prevent the shutdown event from being handled in the wrong order, e.g.
 * <em>after</em> the {@link SlingBeanFactoryPostProcessor} was invoked.
 * 
 * @author Olaf Otto
 */
@Service
public class SlingOsgiBundleShutdownHandler extends ContextShutdownHandler implements SynchronousBundleListener, BundleContextAware {
    private BundleContext context;

    @Override
    @Async
    public void bundleChanged(BundleEvent event) {
        if (isRemoveEvent(event)) {
            event.getBundle().getBundleContext().removeBundleListener(this);
            handleStop(event.getBundle());
        }
    }

    private boolean isRemoveEvent(BundleEvent event) {
        return event.getType() == STOPPING;
    }

    @PostConstruct
    public void startListening() {
        this.context.addBundleListener(this);
    }

    @PreDestroy
    public void stopListening() {
        this.context.removeBundleListener(this);
    }

    @Override
    public void setBundleContext(BundleContext bundleContext) {
        this.context = bundleContext;
    }
}
