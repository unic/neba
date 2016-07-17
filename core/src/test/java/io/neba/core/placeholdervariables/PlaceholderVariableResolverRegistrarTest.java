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

package io.neba.core.placeholdervariables;

import io.neba.api.configuration.PlaceholderVariableResolver;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.util.StringValueResolver;

import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.*;

/**
 * @author Olaf Otto
 */
@RunWith(MockitoJUnitRunner.class)
public class PlaceholderVariableResolverRegistrarTest {
	@Mock
    private BundleContext context;
	@Mock
    private ConfigurableListableBeanFactory factory;
    @Mock
    private Bundle bundle;

    private String[] resolverNames;

	@InjectMocks
    private PlaceholderVariableResolverRegistrar testee;

    @Before
    public void prepareOsgiContext() {
        when(this.context.getBundle()).thenReturn(this.bundle);
    }

    @Test
    public void testPlaceholderVariableResolverRegistration() throws Exception {
        withResolvers("resover1", "resolver2");
        registerResolvers();
        verifyRegistrarAddsEmbeddedResolverForEachResolver();
    }

    private void verifyRegistrarAddsEmbeddedResolverForEachResolver() {
        verify(this.factory, times(this.resolverNames.length)).addEmbeddedValueResolver(isA(StringValueResolver.class));
    }

    private void registerResolvers() {
        this.testee.registerResolvers(this.context, this.factory);
    }

    private void withResolvers(String... strings) {
        this.resolverNames = strings;
        when(this.factory.getBeanNamesForType(eq(PlaceholderVariableResolver.class))).thenReturn(this.resolverNames);
    }
}
