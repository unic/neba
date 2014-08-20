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

package io.neba.core.resourcemodels.tagsupport;

import io.neba.core.resourcemodels.caching.ResourceModelCaches;
import io.neba.core.resourcemodels.mapping.ResourceToModelMapper;
import io.neba.core.resourcemodels.registration.LookupResult;
import io.neba.core.resourcemodels.registration.ModelRegistry;
import io.neba.core.util.OsgiBeanSource;
import org.apache.sling.api.resource.Resource;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import java.util.LinkedList;

import static io.neba.api.Constants.SYNTHETIC_RESOURCETYPE_ROOT;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Olaf Otto
 */
@RunWith(MockitoJUnitRunner.class)
public class ResourceModelProviderImplTest {
	@Mock
	private ModelRegistry registry;
	@Mock
	private Resource resource;
	@Mock
	private ResourceToModelMapper mapper;
    @Mock
    private LookupResult lookupResult;
    @Mock
	private ResourceModelCaches caches;

    private OsgiBeanSource<?> osgiBeanSource;
    private Object resolutionResult;
    private final Object model = new Object();

	@InjectMocks
	private ResourceModelProviderImpl testee;

    @Before
	@SuppressWarnings("unchecked")
	public void prepareContainerAdapter() {
		when(this.mapper.map(isA(Resource.class), isA(OsgiBeanSource.class))).thenAnswer(new Answer<Object>() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				OsgiBeanSource<Object> source = (OsgiBeanSource<Object>) invocation.getArguments()[1];
				return source.getBean();
			}
		});
	}

    @Before
    public void provideMockResourceModel() {
        LinkedList<LookupResult> lookupResults = new LinkedList<LookupResult>();
        this.osgiBeanSource = mock(OsgiBeanSource.class);
        lookupResults.add(this.lookupResult);
        doReturn(this.osgiBeanSource).when(this.lookupResult).getSource();
        when(this.osgiBeanSource.getBean()).thenReturn(this.model);
        when(this.registry.lookupMostSpecificModels(eq(this.resource))).thenReturn(lookupResults);
        when(this.registry.lookupMostSpecificModels(eq(this.resource), anyString())).thenReturn(lookupResults);
    }

	@Test
	public void testResolutionOfSpecificModelIncludingBaseTypes() throws Exception {
		provideMostSpecificModelIncludingBaseTypes();
		verifyResourceIsMappedToModel();
        assertResolvedModelIsReturned();
	}

    @Test
    public void testResolutionOfSpecificModelWithModelForNtUnstructured() throws Exception {
        withModelFoundForResourceType("nt:unstructured");
        provideMostSpecificModel();
        assertResolvedModelIsNull();
    }

    @Test
    public void testResolutionOfSpecificModelWithModelForNtBase() throws Exception {
        withModelFoundForResourceType("nt:base");
        provideMostSpecificModel();
        assertResolvedModelIsNull();
    }

    @Test
    public void testResolutionOfSpecificModelWithModelForSyntheticResourceRoot() throws Exception {
        withModelFoundForResourceType(SYNTHETIC_RESOURCETYPE_ROOT);
        provideMostSpecificModel();
        assertResolvedModelIsNull();
    }

    @Test
    public void testResolutionOfAnyModelWithModelForNtUnstructured() throws Exception {
        withModelFoundForResourceType("nt:unstructured");
        provideMostSpecificModelIncludingBaseTypes();
        verifyResourceIsMappedToModel();
        assertResolvedModelIsReturned();
    }

    @Test
    public void testResolutionOfAnyModelWithModelForNtBase() throws Exception {
        withModelFoundForResourceType("nt:base");
        provideMostSpecificModelIncludingBaseTypes();
        verifyResourceIsMappedToModel();
        assertResolvedModelIsReturned();
    }

    @Test
    public void testResolutionOfAnyModelWithModelForSyntheticResourceRoot() throws Exception {
        withModelFoundForResourceType(SYNTHETIC_RESOURCETYPE_ROOT);
        provideMostSpecificModelIncludingBaseTypes();
        verifyResourceIsMappedToModel();
        assertResolvedModelIsReturned();
    }

    @Test
    public void testResolutionOfAnyModelWithSpecificBeanName() throws Exception {
        provideMostSpecificModelWithBeanName("unitTestBean");
        verifyRegistryIsQueriedWithBeanName();
        verifyResourceIsMappedToModel();
        assertResolvedModelIsReturned();
    }

    @Test
    public void testResolutionOfAnyModelWithSpecificBeanNameDisregardsBasicTypes() throws Exception {
        withModelFoundForResourceType("nt:base");
        provideMostSpecificModelWithBeanName("unitTestBean");
        verifyRegistryIsQueriedWithBeanName();
        verifyResourceIsMappedToModel();
        assertResolvedModelIsReturned();
    }

    private void verifyRegistryIsQueriedWithBeanName() {
        verify(this.registry).lookupMostSpecificModels(eq(this.resource), anyString());
    }

    private void provideMostSpecificModelWithBeanName(String name) {
        this.resolutionResult = this.testee.resolveMostSpecificModelWithBeanName(this.resource, name);
    }

    private void provideMostSpecificModelIncludingBaseTypes() {
        this.resolutionResult = this.testee.resolveMostSpecificModelIncludingModelsForBaseTypes(this.resource);
    }

    private void provideMostSpecificModel() {
        this.resolutionResult = this.testee.resolveMostSpecificModel(this.resource);
    }

    private void withModelFoundForResourceType(String type) {
        when(this.lookupResult.getResourceType()).thenReturn(type);
    }

    private void verifyResourceIsMappedToModel() {
		verify(this.mapper).map(eq(this.resource), eq(this.osgiBeanSource));
	}

	private void assertResolvedModelIsNull() {
		assertThat(this.resolutionResult).isNull();
	}

    private void assertResolvedModelIsReturned() {
        assertThat(this.resolutionResult).isSameAs(this.model);
    }
}
