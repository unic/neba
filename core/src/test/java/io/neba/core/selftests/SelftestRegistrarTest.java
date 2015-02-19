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

package io.neba.core.selftests;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.osgi.framework.Bundle;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests {@link SelftestRegistrar} using a bundle mock.
 * @author Olaf Otto
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("SelftestRegistrarTest.xml")
public class SelftestRegistrarTest implements BeanFactoryAware {
    private ConfigurableListableBeanFactory factory;
    private Bundle bundle;

    private SelftestRegistrar testee;

    @Before
    public void prepareRegistrar() {
        this.testee = new SelftestRegistrar();
    }
    
    @Before
    public void prepareOsgiContext() {
        this.bundle = mock(Bundle.class);
        when(this.bundle.getBundleId()).thenReturn(12345L);
    }
    
    @Test
    public void testRegistrarDiscoversAllSelftestingBeans() throws Exception {
        discoverSelftests();
        assertNumberOfRegisteredSelfTestsIs(3);
    }

    @Test
    public void testRemovalOfReferencesOnSourceBundleShutdown() throws Exception {
        discoverSelftests();
        assertNumberOfRegisteredSelfTestsIs(3);
        
        signalSourceBundleRemoval();
        assertNumberOfRegisteredSelfTestsIs(0);
    }

    @Test
    public void testRemovalOfInvalidReferences() throws Exception {
        discoverSelftests();
        withInvalidReferences();
        removeInvalidReferences();
        assertNumberOfRegisteredSelfTestsIs(0);
    }

    private void removeInvalidReferences() {
        this.testee.removeInvalidReferences();
    }

    private void withInvalidReferences() {
        doReturn(Bundle.UNINSTALLED).when(this.bundle).getState();
    }

    private void signalSourceBundleRemoval() {
        this.testee.unregister(this.bundle);
    }

    private void assertNumberOfRegisteredSelfTestsIs(int i) {
        assertThat(this.testee.getSelftestReferences()).hasSize(i);
    }

    private void discoverSelftests() {
        this.testee.registerSelftests(this.factory, this.bundle);
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.factory = (ConfigurableListableBeanFactory) beanFactory;
    }
}
