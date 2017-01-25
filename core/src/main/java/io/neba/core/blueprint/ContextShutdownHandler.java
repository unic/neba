/**
 * Copyright 2013 the original author or authors.
 * <p/>
 * Licensed under the Apache License, Version 2.0 the "License";
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/

package io.neba.core.blueprint;

import io.neba.core.mvc.MvcServlet;
import io.neba.core.resourcemodels.registration.ModelRegistrar;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Base class for services reacting to context shutdowns.
 *
 * @author Olaf Otto
 */
public abstract class ContextShutdownHandler {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    @Autowired
    private ModelRegistrar modelRegistrar;
    @Autowired
    private MvcServlet dispatcherServlet;

    /**
     * We must guarantee the order in which the event is consumed by the
     * collaborators since subsequent operations may depend on the resulting
     * state.<br />
     * Furthermore, the operations performed by the event handling parties may
     * encounter further locks which may be blocked by threads waiting for the
     * {@link io.neba.core.blueprint.EventhandlingBarrier}, thus resulting in a deadlock. <br />
     * To prevent this, the handling is performed {@link org.springframework.scheduling.annotation.Async asynchronously}.
     *
     * @param bundle must not be <code>null</code>.
     */
    public void handleStop(Bundle bundle) {
        if (bundle == null) {
            throw new IllegalArgumentException("Method argument bundle must not be null.");
        }

        this.logger.info("Removing infrastructure for bundle: " + bundle + "...");
        EventhandlingBarrier.begin();
        try {
            this.modelRegistrar.unregister(bundle);
            this.dispatcherServlet.disableMvc(bundle);
        } finally {
            EventhandlingBarrier.end();
        }
        this.logger.info("Infrastructure for " + bundle + " removed.");
    }
}
