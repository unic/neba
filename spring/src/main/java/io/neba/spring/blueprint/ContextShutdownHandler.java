/*
  Copyright 2013 the original author or authors.
  <p/>
  Licensed under the Apache License, Version 2.0 the "License";
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at
  <p/>
  http://www.apache.org/licenses/LICENSE-2.0
  <p/>
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
 */

package io.neba.spring.blueprint;

import io.neba.spring.mvc.MvcServlet;
import io.neba.spring.resourcemodels.registration.SpringModelRegistrar;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Base class for services reacting to context shutdowns.
 * Operations performed by the event handling parties may
 * encounter further locks, thus resulting in a (temporary) deadlock. <br />
 * To prevent this, it is recommended that any event handling is
 * performed {@link org.springframework.scheduling.annotation.Async asynchronously}.
 *
 * @author Olaf Otto
 */
abstract class ContextShutdownHandler {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    @Autowired
    private SpringModelRegistrar modelRegistrar;
    @Autowired
    private MvcServlet dispatcherServlet;

    /**
     * We must guarantee the order in which the event is consumed by the
     * collaborators since subsequent operations may depend on the resulting
     * state.
     *
     * @param bundle must not be <code>null</code>.
     */
    void handleStop(Bundle bundle) {
        if (bundle == null) {
            throw new IllegalArgumentException("Method argument bundle must not be null.");
        }

        this.logger.info("Removing infrastructure for bundle: " + bundle + "...");
            this.modelRegistrar.unregister(bundle);
            this.dispatcherServlet.disableMvc(bundle);

        this.logger.info("Infrastructure for {} removed.", bundle);
    }
}
