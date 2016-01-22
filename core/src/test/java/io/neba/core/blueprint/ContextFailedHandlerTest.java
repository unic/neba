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
import org.eclipse.gemini.blueprint.context.event.OsgiBundleApplicationContextEvent;
import org.eclipse.gemini.blueprint.context.event.OsgiBundleContextFailedEvent;
import org.eclipse.gemini.blueprint.context.event.OsgiBundleContextRefreshedEvent;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static org.osgi.framework.Bundle.*;

/**
 * @author Olaf Otto
 */
@RunWith(MockitoJUnitRunner.class)
public class ContextFailedHandlerTest {
    @Mock
    private ModelRegistrar modelRegistrar;
    @Mock
    private MvcServlet dispatcherServlet;
    @Mock
    private Bundle bundle;

    private OsgiBundleApplicationContextEvent event;

    @InjectMocks
    private ContextFailedHandler testee;

    @Test
    public void testHandlerOnlyHandlesContextFailedEvents() throws Exception {
        withContextRefreshedEvent();
        handleEvent();

        verifyBundleIsNotObtainedFromEvent();

        withContextFailedEvent();
        handleEvent();
        verifyBundleIsObtainedFromEvent();
    }

    @Test
    public void testHandlerUnregistersInfrastructureInRightOrder() throws Exception {
        withContextFailedEvent();
        handleEvent();
        verifyInfrastructureIsUnregisteredInCorrectOrder();
    }

    @Test
    public void testBundleIsStoppedWhenStarting() throws Exception {
        withContextFailedEvent();
        withStartingBundle();
        handleEvent();
        verifyBundleIsStopped();
    }

    @Test
    public void testBundleIsStoppedWhenActive() throws Exception {
        withContextFailedEvent();
        withActiveBundle();
        handleEvent();
        verifyBundleIsStopped();
    }

    @Test
    public void testBundleIsNotStoppedWhenStopping() throws Exception {
        withContextFailedEvent();
        withStoppingBundle();
        handleEvent();
        verifyBundleIsNotStopped();
    }

    @Test
    public void testBundleIsNotStoppedWhenResolved() throws Exception {
        withContextFailedEvent();
        withResolvedBundle();
        handleEvent();
        verifyBundleIsNotStopped();
    }

    private void withResolvedBundle() {
        doReturn(RESOLVED).when(this.bundle).getState();
    }

    private void withStoppingBundle() {
        doReturn(STOPPING).when(this.bundle).getState();
    }

    private void withActiveBundle() {
        doReturn(ACTIVE).when(this.bundle).getState();
    }

    private void withStartingBundle() {
        doReturn(STARTING).when(this.bundle).getState();
    }

    private void verifyBundleIsStopped() throws BundleException {
        verify(this.bundle).stop();
    }

    private void verifyBundleIsNotStopped() throws BundleException {
        verify(this.bundle, never()).stop();
    }

    private void verifyInfrastructureIsUnregisteredInCorrectOrder() {
        InOrder inOrder = inOrder(this.modelRegistrar, this.dispatcherServlet);
        inOrder.verify(this.modelRegistrar).unregister(eq(this.bundle));
        inOrder.verify(this.dispatcherServlet).disableMvc(eq(this.bundle));
    }

    private void verifyBundleIsObtainedFromEvent() {
        verify(this.event, atLeastOnce()).getBundle();
    }

    private void verifyBundleIsNotObtainedFromEvent() {
        verify(this.event, never()).getBundle();
    }

    private void handleEvent() {
        this.testee.onOsgiApplicationEvent(this.event);
    }

    private void withContextRefreshedEvent() {
        this.event = mock(OsgiBundleContextRefreshedEvent.class);
        when(this.event.getBundle()).thenReturn(this.bundle);
    }

    private void withContextFailedEvent() {
        this.event = mock(OsgiBundleContextFailedEvent.class);
        when(this.event.getBundle()).thenReturn(this.bundle);
    }
}
