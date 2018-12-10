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
package io.neba.core.resourcemodels.factory;

import io.neba.api.annotations.ResourceModel;
import io.neba.api.spi.ResourceModelFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.Version;
import org.osgi.service.component.ComponentContext;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Vector;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.osgi.framework.Bundle.ACTIVE;
import static org.osgi.framework.BundleEvent.STARTING;

/**
 * @author Olaf Otto
 */
@RunWith(MockitoJUnitRunner.class)
public class NebaPackagesResourceModelFactoryInjectorTest {
    @Mock
    private ComponentContext componentContext;
    @Mock
    private Bundle bundle;
    @Mock
    private BundleContext bundleContext;

    private BundleListener registeredListener;

    private NebaPackagesResourceModelFactoryInjector testee;

    @Before
    public void setUp() throws ClassNotFoundException, MalformedURLException {
        Dictionary<String, String> headers = new Hashtable<>();
        headers.put("Neba-Packages", "io.neba.core.resourcemodels.factory");

        doReturn(headers).when(bundle).getHeaders();
        doReturn(bundleContext).when(componentContext).getBundleContext();
        doReturn(bundleContext).when(bundle).getBundleContext();
        doReturn(ACTIVE).when(bundle).getState();
        doReturn("test-bundle").when(bundle).getSymbolicName();
        doReturn(new Version(1, 0, 0)).when(bundle).getVersion();

        doAnswer(inv -> {
            registeredListener = (BundleListener) inv.getArguments()[0];
            return null;
        }).when(bundleContext).addBundleListener(any());

        Vector<URL> urls = new Vector<>();
        urls.add(new URL("file://some/class/file.class"));
        doReturn(urls.elements()).when(bundle).findEntries(any(), any(), eq(true));
        doReturn(TestModel.class).when(bundle).loadClass(any());

        this.testee = new NebaPackagesResourceModelFactoryInjector();
        this.testee.activate(this.componentContext);
    }

    @Test
    public void testModelFactoryIsRegisteredWhenModelsArePresent() {
        startBundle();
        verifyResourceModelFactoryServiceIsAddedForBundle();
    }

    @Test
    public void testModelFactoryIsNotRegisteredWhenNoModelsArePresent() {
        withoutNebaPackagesHeader();
        startBundle();
        verifyNoResourceModelFactoryServiceIsAddedForBundle();
    }

    @Test
    public void testModelFactoryIsNotRegisteredWhenBundleContextIsMissing() {
        withoutBundleContext();
        startBundle();
        verifyNoResourceModelFactoryServiceIsAddedForBundle();
    }

    private void withoutBundleContext() {
        doReturn(null).when(bundle).getBundleContext();
    }

    private void withoutNebaPackagesHeader() {
        doReturn(new Hashtable<>()).when(bundle).getHeaders();
    }

    @SuppressWarnings("unchecked")
    private void verifyResourceModelFactoryServiceIsAddedForBundle() {
        verify(bundleContext).registerService(eq(ResourceModelFactory.class), isA(ResourceModelFactory.class), isA(Dictionary.class));
    }

    private void verifyNoResourceModelFactoryServiceIsAddedForBundle() {
        verify(bundleContext, never()).registerService(eq(ResourceModelFactory.class), isA(ResourceModelFactory.class), any());
    }

    private void startBundle() {
        this.registeredListener.bundleChanged(new BundleEvent(STARTING, bundle));
    }

    @ResourceModel(types = "some/model/type")
    public static class TestModel {
    }
}