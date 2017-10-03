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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.Bundle;


import static org.mockito.Mockito.inOrder;

@RunWith(MockitoJUnitRunner.class)
public class ContextShutdownHandlerTest {
    @Mock
    private SpringModelRegistrar modelRegistrar;
    @Mock
    private MvcServlet dispatcherServlet;
    @Mock
    private Bundle bundle;

    @InjectMocks
    private ContextShutdownHandler testee = new ContextShutdownHandler() {
    };

    @Test(expected = IllegalArgumentException.class)
    public void testBundleMustNotBeNull() throws Exception {
        withNullBundle();
        handleStop();
    }

    @Test
    public void testOrderOfShutdownEvents() throws Exception {
        handleStop();
    }

    @Test
    public void testBundleIsRemovedFromModelRegistrarAndDispatcherServletUsingEventHandlingBarrier() throws Exception {
        handleStop();

        InOrder inOrder = inOrder(this.modelRegistrar, this.dispatcherServlet);
        inOrder.verify(this.modelRegistrar).unregister(this.bundle);
        inOrder.verify(this.dispatcherServlet).disableMvc(this.bundle);
    }

    private void handleStop() {
        this.testee.handleStop(this.bundle);
    }

    private void withNullBundle() {
        this.bundle = null;
    }
}