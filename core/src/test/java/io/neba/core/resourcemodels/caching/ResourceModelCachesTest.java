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

package io.neba.core.resourcemodels.caching;

import io.neba.api.spi.ResourceModelCache;
import io.neba.core.resourcemodels.metadata.ResourceModelMetaData;
import io.neba.core.resourcemodels.metadata.ResourceModelMetaDataRegistrar;
import io.neba.core.resourcemodels.metadata.ResourceModelStatistics;
import io.neba.core.util.Key;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static io.neba.core.resourcemodels.caching.ResourceModelCaches.key;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Olaf Otto
 */
@RunWith(MockitoJUnitRunner.class)
public class ResourceModelCachesTest {
	@Mock
	private Resource resource;
	@Mock
	private ResourceResolver resourceResolver;
	@Mock
	private ResourceModelMetaDataRegistrar resourceModelMetaDataRegistrar;
	@Mock
	private ResourceModelMetaData resourceModelMetaData;
	@Mock
	private ResourceModelStatistics resourceModelStatistics;

	private List<ResourceModelCache> mockedCaches = new LinkedList<>();
	private Class<Object> targetType = Object.class;
    private Object model = new Object();
	private Object lookupResult;

    @InjectMocks
	private ResourceModelCaches testee;

	@Before
	public void prepareTest() {
		this.mockedCaches.clear();

		doReturn(this.resourceModelMetaData)
				.when(this.resourceModelMetaDataRegistrar)
				.get(any());

		doReturn(this.resourceModelStatistics)
				.when(this.resourceModelMetaData)
				.getStatistics();

		doReturn(this.resourceResolver)
				.when(this.resource)
				.getResourceResolver();
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNullResourceIsNotAllowedForCacheLookup() {
		this.testee.lookup(null, key(this.targetType));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNullClassIsNotAllowedForCacheLookup() {
		this.testee.lookup(mock(Resource.class), null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNullResourceIsNotAllowedForCacheWrite() {
		this.testee.store(null, key(this.targetType), new Object());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNullClassIsNotAllowedForCacheWrite() {
		this.testee.store(mock(Resource.class), null, new Object());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNullModelIsNotAllowedForCacheWrite() {
		this.testee.store(mock(Resource.class), key(this.targetType), null);
	}

	@Test
	public void testCacheUsesResourceResolverIdentityForCacheKey() {
		bindCache();
    	storeModel();
    	lookup();

    	assertModelWasFoundInCache();

		withDifferentResourceResolverForSameResource();
		lookup();

		assertModelWasNotFoundInCache();
	}

	@Test
	public void testUnsuccessfulLookup() {
		bindCache();
		bindCache();
		lookup();
		verifyEachCacheIsUsedForLookup();
	}
	
	@Test
	public void testSuccessfulLookup() {
		bindCache();
		bindCache();
		withCachedObjectIn(0);
		lookup();
		verifyCacheReturnsOnFirstHit();
	}

	@Test
	public void testSuccessfulLookupIsCountedAsCacheHitInResourceModelMetaData() {
		bindCache();
		withCachedObjectIn(0);
		lookup();
		verifyCacheHitIsCounted();
	}

	@Test
	public void testUnsuccessfulLookupIsNotCountedAsCacheHit() {
		bindCache();
		lookup();
		verifyResourceModelStatisticsAreNotUsed();
	}

	@Test
	public void testRemovalOfCaches() {
		bindCache();
		bindCache();
		unbindAllCaches();
		lookup();
		verifyNoCacheIsUsed();
	}

    @Test
    public void testStorage() {
        bindCache();
        bindCache();
        storeModel();
        verifyModelIsStoredInAllCaches();
    }

	@Test
	public void testRemovalOfNullCacheDoesNotCauseException() {
		this.testee.unbind(null);
	}

    private void verifyModelIsStoredInAllCaches() {
        for (ResourceModelCache cache : this.mockedCaches) {
            verify(cache).put(eq(this.resource), eq(this.model), isA(Key.class));
        }
    }

    private void storeModel() {
        this.testee.store(this.resource, key(this.targetType), this.model);
    }

    private void verifyNoCacheIsUsed() {
		for (ResourceModelCache cache : this.mockedCaches) {
			verify(cache, never()).get(isA(Key.class));
		}
	}

	private void unbindAllCaches() {
		for (ResourceModelCache cache : this.mockedCaches) {
			this.testee.unbind(cache);
		}
	}

	private void verifyCacheReturnsOnFirstHit() {
		ResourceModelCache cache = this.mockedCaches.get(0);
		verify(cache, times(1)).get(isA(Key.class));
		for (int i = 1; i < this.mockedCaches.size(); ++i) {
			cache = this.mockedCaches.get(i);
			verify(cache, never()).get(isA(Key.class));
		}
	}

	private void withCachedObjectIn(int index) {
		ResourceModelCache cache = this.mockedCaches.get(index);
		when(cache.get(isA(Key.class))).thenReturn(this.model);
	}

    private void verifyEachCacheIsUsedForLookup() {
		for (ResourceModelCache cache : this.mockedCaches) {
			verify(cache, times(1)).get(isA(Key.class));
		}
	}

	private void verifyCacheHitIsCounted() {
		verify(this.resourceModelStatistics).countCacheHit();
	}

	private void verifyResourceModelStatisticsAreNotUsed() {
		verify(this.resourceModelMetaDataRegistrar, never()).get(any());
	}

	private void lookup() {
		this.lookupResult = this.testee.lookup(this.resource, key(this.targetType));
	}

	private void assertModelWasFoundInCache() {
		assertThat(this.lookupResult).isSameAs(this.model);
	}

	private void assertModelWasNotFoundInCache() {
		assertThat(this.lookupResult).isNull();
	}

	private void withDifferentResourceResolverForSameResource() {
		doReturn(mock(ResourceResolver.class)).when(this.resource).getResourceResolver();
	}

	private void bindCache() {
		ResourceModelCache cache = mock(ResourceModelCache.class);
		Map<Object, Object> cacheData = new HashMap<>();
		doAnswer(i -> cacheData.get(i.getArguments()[0])).when(cache).get(isA(Key.class));
		doAnswer(i -> cacheData.put(i.getArguments()[2], i.getArguments()[1])).when(cache).put(isA(Resource.class), any(), isA(Key.class));

		this.mockedCaches.add(cache);
		this.testee.bind(cache);
	}
}
