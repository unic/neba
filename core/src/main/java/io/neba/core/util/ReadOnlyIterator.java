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

import java.util.Iterator;

/**
 * @author Olaf Otto
 *
 * @param <T> the type produced by the iterator.
 */
public class ReadOnlyIterator<T> implements Iterator<T> {
    private final Iterator<T> iterator;

    /**
     * @param iterator must not be <code>null</code>.
     * @return never <code>null</code>.
     */
    public static <T> ReadOnlyIterator<T> readOnly(Iterator<T> iterator) {
        if (iterator == null) {
            throw new IllegalArgumentException("Method argument iterator must not be null.");
        }
        return new ReadOnlyIterator<>(iterator);
    }

    /**
     * @param iterator must not be <code>null</code>.
     */
    public ReadOnlyIterator(Iterator<T> iterator) {
        if (iterator == null) {
            throw new IllegalArgumentException("Constructor parameter iterator must not be null.");
        }
        this.iterator = iterator;
    }

    @Override
    public boolean hasNext() {
        return this.iterator.hasNext();
    }

    @Override
    public T next() {
        return this.iterator.next();
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("This is a read-only iterator");
    }
}
