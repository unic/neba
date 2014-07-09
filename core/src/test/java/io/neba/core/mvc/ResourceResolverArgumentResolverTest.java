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

package io.neba.core.mvc;

import org.apache.sling.api.resource.ResourceResolver;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.doReturn;

/**
 * @author Olaf Otto
 */
@RunWith(MockitoJUnitRunner.class)
public class ResourceResolverArgumentResolverTest extends AbstractArgumentResolverTest<ResourceResolverArgumentResolver> {
    @Mock
    private ResourceResolver resourceResolver;

    @InjectMocks
    private ResourceResolverArgumentResolver testee;

    @Before
    public void mockResourceResolver() throws Exception {
        doReturn(this.resourceResolver).when(getRequest()).getResourceResolver();
    }

    @Test
    public void testResourceResolverSupport() throws Exception {
        resolveArguments();
        assertResolvedArgumentIsResourceResolver();
    }

    @Test
    public void testResourceResolverIsSupported() throws Exception {
        assertResolverSupports(ResourceResolver.class);
    }

    @Override
    public ResourceResolverArgumentResolver getTestee() {
        return this.testee;
    }

    private void assertResolvedArgumentIsResourceResolver() {
        assertResolvedArgumentIs(this.resourceResolver);
    }
}
