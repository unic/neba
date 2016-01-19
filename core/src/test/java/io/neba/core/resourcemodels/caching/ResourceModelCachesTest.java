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

package io.neba.core.resourcemodels.caching;

import io.neba.api.resourcemodels.ResourceModelCache;
import io.neba.core.util.Key;
import org.apache.sling.api.resource.Resource;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.LinkedList;
import java.util.List;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 * @author Olaf Otto
 */
@RunWith(MockitoJUnitRunner.class)
public class ResourceModelCachesTest {
	@Mock
	private Resource resource;
	
	private List<ResourceModelCache> mockedCaches = new LinkedList<ResourceModelCache>();
	private Class<Object> targetType = Object.class;
    private Object model = new Object();

    @InjectMocks
	private ResourceModelCaches testee;

    @Before
	public void prepareTest() {
		this.mockedCaches.clear();
	}

	@Test
	public void testUnsuccessfulLookup() throws Exception {
		bindCache();
		bindCache();
		lookup();
		verifyEachCacheIsUsedForLookup();
	}
	
	@Test
	public void testSuccessfulLookup() throws Exception {
		bindCache();
		bindCache();
		withCachedObjectIn(0);
		lookup();
		verifyCacheReturnsOnFirstHit();
	}
	
	@Test
	public void testRemovalOfCaches() throws Exception {
		bindCache();
		bindCache();
		unbindAllCaches();
		lookup();
		verifyNoCacheIsUsed();
	}

    @Test
    public void testStorage() throws Exception {
        bindCache();
        bindCache();
        storeModel();
        verifyModelIsStoredInAllCaches();
    }

	@Test
	public void testRemovalOfNullCacheDoesNotCauseException() throws Exception {
		this.testee.unbind(null);
	}

    private void verifyModelIsStoredInAllCaches() {
        for (ResourceModelCache cache : this.mockedCaches) {
            verify(cache).put(eq(this.resource), eq(this.model), eq(lookupKey()));
        }
    }

    private void storeModel() {
        this.testee.store(this.resource, this.model, lookupKey());
    }

    private void verifyNoCacheIsUsed() {
		for (ResourceModelCache cache : this.mockedCaches) {
			verify(cache, never()).get(eq(lookupKey()));
		}
	}

	private void unbindAllCaches() {
		for (ResourceModelCache cache : this.mockedCaches) {
			this.testee.unbind(cache);
		}
	}

	private void verifyCacheReturnsOnFirstHit() {
		ResourceModelCache cache = this.mockedCaches.get(0);
		verify(cache, times(1)).get(eq(lookupKey()));
		for (int i = 1; i < this.mockedCaches.size(); ++i) {
			cache = this.mockedCaches.get(i);
			verify(cache, never()).get(eq(lookupKey()));
		}
	}

	private void withCachedObjectIn(int index) {
		ResourceModelCache cache = this.mockedCaches.get(index);
		when(cache.get(eq(lookupKey()))).thenReturn(this.model);
	}

    private Key lookupKey() {
        return new Key("/junit/test/resource/path", this.targetType);
    }

    private void verifyEachCacheIsUsedForLookup() {
		for (ResourceModelCache cache : this.mockedCaches) {
			verify(cache, times(1)).get(eq(lookupKey()));
		}
	}

	private void lookup() {
		this.testee.lookup(lookupKey());
	}
	
	private void bindCache() {
		ResourceModelCache cache = mock(ResourceModelCache.class);
		this.mockedCaches.add(cache);
		this.testee.bind(cache);
	}
}
