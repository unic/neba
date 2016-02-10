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

package io.neba.core.util;

import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A thread-safe storage for key -&gt; value associations. Unlike
 * {@link ConcurrentHashMap}, this map synchronizes writes since the multi-value
 * associations require two independent read/writes to the map state; one to
 * retrieve an existing collection, a second one to add another value to it. <br />
 * Note that it is <em>not</em> save to modify the contents of the collection
 * returned by this map since it is still based on the contents of this map and
 * not a shallow copy for performance considerations.
 * <br />
 * In addition, the values stored in this map are distinct, i.e.
 * if a value already exists in the values collection it is {@link Collection#remove(Object) removed}
 * prior to the insertion of the new element.
 * 
 * @param <K> The key's type.
 * @param <V> The value's type.
 * @author Olaf Otto
 */
public class ConcurrentDistinctMultiValueMap<K, V> {
    private final Map<K, Collection<V>> store = new ConcurrentHashMap<>(128);

    public Collection<V> get(K key) {
        return this.store.get(key);
    }

    public synchronized void put(K key, V value) {
        Collection<V> vs = getOrCreate(key);
        vs.add(value);
    }

    public void clear() {
        this.store.clear();
    }

    public Collection<V> remove(K key) {
        return this.store.remove(key);
    }

    public Set<Entry<K, Collection<V>>> entrySet() {
        return this.store.entrySet();
    }

    public Collection<Collection<V>> values() {
        return this.store.values();
    }

    public synchronized void put(K key, Collection<V> values) {
        Collection<V> vs = getOrCreate(key);
        vs.addAll(values);
    }

    private Collection<V> getOrCreate(K key) {
        Collection<V> vs = this.store.get(key);
        if (vs == null) {
            vs = new ConcurrentLinkedDistinctQueue<>();
            this.store.put(key, vs);
        }
        return vs;
    }

    /**
     * @return a shallow copy of the current state of this map. Note that the
     *         map values (the collection) are also copied; it is thus save to
     *         modify the state of the returned map and the state of the
     *         collections returned as the map values. Never returns null.
     */
    public Map<K, Collection<V>> getContents() {
        HashMap<K, Collection<V>> contents = new HashMap<>(this.store.size());
        Set<Entry<K, Collection<V>>> entries = this.store.entrySet();
        for (Entry<K, Collection<V>> entry : entries) {
            contents.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        return contents;
    }

    public int size() {
        return this.store.size();
    }

    public boolean isEmpty() {
        return this.store.isEmpty();
    }

    /**
     * Removes the given value from all collections stored under any key.
     *
     * @param value must not be <code>null</code>.
     */
    public void removeValue(V value) {
        if (value == null) {
            throw new IllegalArgumentException("Method argument value must not be null.");
        }
        for (Collection<V> values : values()) {
            values.remove(value);
        }
    }
}
