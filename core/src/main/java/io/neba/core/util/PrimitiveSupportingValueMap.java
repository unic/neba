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

import org.apache.sling.api.resource.ValueMap;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import static org.apache.commons.lang.ClassUtils.primitiveToWrapper;

/**
 * This {@link ValueMap} converts primitive types
 * to their boxed equivalents prior to conversion.
 * 
 * @author Olaf Otto
 */
public class PrimitiveSupportingValueMap implements ValueMap {
    private final ValueMap map;

    /**
     * @param valueMap must not be <code>null</code>.
     */
    public PrimitiveSupportingValueMap(ValueMap valueMap) {
        if (valueMap == null) {
            throw new IllegalArgumentException("Method argument valueMap must not be null.");
        }
        this.map = valueMap;
	}

	public <T> T get(String name, Class<T> type) {
        if (name == null) {
            throw new IllegalArgumentException("Method argument name must not be null.");
        }
        if (type == null) {
            throw new IllegalArgumentException("Method argument type must not be null.");
        }
        @SuppressWarnings("unchecked")
		final Class<T> boxedType = primitiveToWrapper(type);
		return this.map.get(name, boxedType);
	}

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(String name, T defaultValue) {
        if (name == null) {
            throw new IllegalArgumentException("Method argument name must not be null.");
        }
        if (defaultValue == null) {
            throw new IllegalArgumentException("Method argument defaultValue must not be null.");
        }

        T t = (T) get(name, defaultValue.getClass());

        return t == null? defaultValue : t;
    }

    @Override
    public Object get(Object key) {
        if (key == null) {
            throw new IllegalArgumentException("Method argument key must not be null.");
        }

        return this.map.get(key);
    }

    @Override
    public int size() {
        return this.map.size();
    }

    @Override
    public boolean isEmpty() {
        return this.map.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return this.map.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return this.map.containsValue(value);
    }

    @Override
    public Object put(String key, Object value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object remove(Object key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void putAll(Map<? extends String, ?> m) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<String> keySet() {
        return this.map.keySet();
    }

    @Override
    public Collection<Object> values() {
        return this.map.values();
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        return this.map.entrySet();
    }
}
