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

import io.neba.core.mvc.MvcServlet;
import io.neba.core.resourcemodels.registration.ModelRegistrar;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Olaf Otto
 */
@RunWith(MockitoJUnitRunner.class)
public class SlingOsgiBundleShutdownHandlerTest {
	@Mock
	private MvcServlet dispatcherServlet;
    @Mock
    private ModelRegistrar modelRegistrar;
    @Mock
    private BundleEvent event;
    @Mock
    private Bundle bundle;
    @Mock
    private BundleContext context;
    
    @InjectMocks
    private SlingOsgiBundleShutdownHandler testee;

    @Before
    public void prepareMocks() {
        when(this.bundle.getBundleContext()).thenReturn(this.context);
        when(this.event.getBundle()).thenReturn(this.bundle);
    }
    
    @Test
    public void testDispatching() throws Exception {
        sendBundleStoppedEvent();
        verifyDispatcherServletReceivesEvent();
        verifyModelRegistrarReceivesEvent();
    }

    @Test
    public void testRegistrationAtBundleContext() throws Exception {
        startListening();
        verifyShutdownHandlerIsRegisteredAsBundleListener();
    }

    @Test
    public void testRemovalFromBundleContext() throws Exception {
        stopListening();
        verifyShutdownHandlerIsRemovedAsBundleListener();
    }

    private void verifyShutdownHandlerIsRemovedAsBundleListener() {
        verify(this.context).removeBundleListener(eq(this.testee));
    }

    private void stopListening() {
        this.testee.stopListening();
    }

    private void verifyShutdownHandlerIsRegisteredAsBundleListener() {
        verify(this.context).addBundleListener(eq(this.testee));
    }

    private void startListening() {
        this.testee.startListening();
    }

    private void sendBundleStoppedEvent() {
        when(this.event.getType()).thenReturn(BundleEvent.STOPPING);
        this.testee.bundleChanged(this.event);
    }

    private void verifyModelRegistrarReceivesEvent() {
        verify(this.modelRegistrar).unregister(eq(this.bundle));
    }


    private void verifyDispatcherServletReceivesEvent() {
        verify(this.dispatcherServlet).disableMvc(eq(this.bundle));
    }
}
