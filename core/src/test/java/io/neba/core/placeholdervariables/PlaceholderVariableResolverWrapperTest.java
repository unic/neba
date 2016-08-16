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
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 * @author Olaf Otto
 */
@RunWith(MockitoJUnitRunner.class)
public class PlaceholderVariableResolverWrapperTest {
    private static final int TWICE = 2;
    
    @Mock
    private ConfigurableListableBeanFactory factory;
    @Mock
    private PlaceholderVariableResolver resolver;

    private String resolverName = "testResolver";
    
    private PlaceholderVariableResolverWrapper testee;

    @Before
    public void prepareWrapper() {
        when(this.factory.getBean(eq(this.resolverName), eq(PlaceholderVariableResolver.class)))
        .thenReturn(this.resolver);
        
        this.testee = new PlaceholderVariableResolverWrapper(this.factory, this.resolverName);
    }

    @Test
    public void testWrapperObtainsResolverFromBeanFactoryEveryTime() throws Exception {
        resolve("${test}");
        resolve("${test}");
        verifyResolverIsObtainedFromBeanFactory(TWICE);
    }
    
    @Test
    public void testWrapperUsesVariableNameAsKeyToResolveValue() throws Exception {
        resolve("${test}");
        verifyResolverFromFactoryIsAskedToResolve("test");
    }

    private void verifyResolverFromFactoryIsAskedToResolve(String key) {
        verify(this.resolver).resolve(eq(key));
    }

    private void verifyResolverIsObtainedFromBeanFactory(int wantedNumberOfInvocations) {
        verify(this.factory, times(wantedNumberOfInvocations)).getBean(eq(this.resolverName), eq(PlaceholderVariableResolver.class));
    }

    private void resolve(String strVal) {
        this.testee.resolveStringValue(strVal);
    }
}
