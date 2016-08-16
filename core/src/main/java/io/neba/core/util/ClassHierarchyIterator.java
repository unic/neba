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
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Queue;

import static java.util.Arrays.asList;

/**
 * Used to iterate over a class hierarchy in the order:
 * 
 * <ol>
 * 	<li>type</li>
 *      <li>implemented interfaces</li>
 *      <li>supertype</li>
 * </ol>.
 * 
 * @author Olaf Otto
 */
public class ClassHierarchyIterator implements Iterable<Class<?>>, Iterator<Class<?>> {

    /**
     * @param type must not be <code>null</code>
     * @return never <code>null</code>
     */
    public static ClassHierarchyIterator hierarchyOf(Class<?> type) {
        return new ClassHierarchyIterator(type);
    }

    private Queue<Class<?>> queue = new LinkedList<>();
    private Class<?> current;
    private Class<?> next = null;

    /**
     * @param type must not be <code>null</code>.
     */
    public ClassHierarchyIterator(Class<?> type) {
        if (type == null) {
            throw new IllegalArgumentException("Constructor parameter type must not be null.");
        }
        this.current = type;
        this.next = current;
    }

    public Iterator<Class<?>> iterator() {
        return this;
    }

    public boolean hasNext() {
        return this.next != null || this.current != null && resolveNext();
    }

    private boolean resolveNext() {
        Class<?>[] interfaces = this.current.getInterfaces();
        if (interfaces != null) {
            queue.addAll(asList(interfaces));
        }
        Class<?> superType = this.current.getSuperclass();
        if (superType != null) {
            queue.add(superType);
        }
        if (!queue.isEmpty()) {
            this.next = this.queue.poll();
        }
        return this.next != null;
    }

    public Class<?> next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        this.current = this.next;
        this.next = null;
        return this.current;
    }

    public void remove() {
        throw new UnsupportedOperationException("Remove is unsupported - this is a read-only iterator.");
    }
}