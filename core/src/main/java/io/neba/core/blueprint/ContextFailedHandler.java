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

package io.neba.core.blueprint;

import org.eclipse.gemini.blueprint.context.event.OsgiBundleApplicationContextEvent;
import org.eclipse.gemini.blueprint.context.event.OsgiBundleApplicationContextListener;
import org.eclipse.gemini.blueprint.context.event.OsgiBundleContextFailedEvent;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import static io.neba.core.util.BundleUtil.displayNameOf;
import static org.osgi.framework.Bundle.ACTIVE;
import static org.osgi.framework.Bundle.STARTING;

/**
 * When an application context activation fails, NEBA removes any previously
 * registered resource models, self tests, MVC infrastructure and the like, since these
 * are registered before the application context is
 * {@link org.springframework.context.ConfigurableApplicationContext#refresh() activated}.
 *
 * @author Olaf Otto
 */
@Service("osgiApplicationContextListener")
public class ContextFailedHandler extends ContextShutdownHandler
                                  implements OsgiBundleApplicationContextListener<OsgiBundleApplicationContextEvent> {
    /**
     * This method is executed asynchronously since the original extender thread may try to obtain a lock to the OSGi
     * framework's registry state during the stop while holding the event handling lock, which may result in a transitive
     * deadlock.
     *
     * @param event can be <code>null</code>, in which case it is ignored.
     */
    @Override
    @Async
    public void onOsgiApplicationEvent(OsgiBundleApplicationContextEvent event) {
        // We need to use the generic OsgiBundleApplicationContextEvent here and test
        // for instanceof since gemini-blueprint does not correctly determine
        // the event type we are listening for.
        if (event instanceof OsgiBundleContextFailedEvent) {
            final Bundle bundle = event.getBundle();

            handleStop(bundle);
            stop(bundle);
        }
    }

    private void stop(Bundle bundle) {
        try {
            if (canStop(bundle)) {
                bundle.stop();
            }
        } catch (BundleException e) {
            throw new RuntimeException("Unable to stop bundle " + displayNameOf(bundle) + ".", e);
        }
    }

    private boolean canStop(Bundle bundle) {
        return bundle.getState() == ACTIVE || bundle.getState() ==  STARTING;
    }
}
