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

import java.util.Collection;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * This linked queue does not permit duplicates and {@link #remove(Object) removes}
 * already existing elements from the underlying collection prior to value insertion.
 *  
 * @author Olaf Otto
 *
 * @param <E> the collection type.
 */
public class ConcurrentLinkedDistinctQueue<E> extends ConcurrentLinkedQueue<E> {
    private static final long serialVersionUID = 5522473136825366225L;
    
    /**
     * Removes the element prior to insertion.
     * 
     * @param e must not be <code>null</code>.
     */
    public synchronized boolean add(E e) {
        remove(e);
        return super.add(e);
    }

    /**
     * Removes all elements in the collection prior to insertion.
     * 
     * @param c must not be <code>null</code>.
     */
    @Override
    public synchronized boolean addAll(Collection<? extends E> c) {
        removeAll(c);
        return super.addAll(c);
    }
}
