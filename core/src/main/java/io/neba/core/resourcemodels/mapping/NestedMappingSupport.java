/*
  Copyright 2013 the original author or authors.
  <p/>
  Licensed under the Apache License, Version 2.0 the "License";
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at
  <p/>
  http://www.apache.org/licenses/LICENSE-2.0
  <p/>
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
 */

package io.neba.core.resourcemodels.mapping;

import io.neba.core.resourcemodels.metadata.ResourceModelMetaData;
import org.osgi.service.component.annotations.Component;

import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static java.lang.ThreadLocal.withInitial;


/**
 * Provides thread-local tracking of mapping invocations in order to support cycles in mappings
 * and gather statistical data regarding mapping depths of
 * {@link io.neba.api.annotations.ResourceModel resource models}.
 *
 * @author Olaf Otto
 */
@Component(service = NestedMappingSupport.class)
@SuppressWarnings("rawtypes")
public class NestedMappingSupport {

    /**
     * Represents the stack of the currently ongoing mappings.
     *
     * @author Olaf Otto
     */
    private static class MappingStack {
        private final Map<Mapping, Mapping> mappings = new LinkedHashMap<>(8);
        private final Map<ResourceModelMetaData, Count> metaData = new HashMap<>(8);
        private RecordedMapping<?> tail = null;

        private <T> void push(Mapping<T> mapping) {
            this.tail = new RecordedMapping<>(mapping, this.tail);
            // We are keeping an occurrence count in order to only remove resource model metadata
            // if no mapping for the corresponding resource model is left on the stack.
            Count occurrenceCount = new Count();
            Count previousCount = this.metaData.put(mapping.getMetadata(), occurrenceCount);
            if (previousCount != null) {
                occurrenceCount.add(previousCount);
            }
            this.mappings.put(mapping, mapping);
        }

        /**
         * Removes the last {@link #push(Mapping) pused mapping} form the stack.
         *
         * @return the stack depth after removing the mapping.
         */
        private int pop() {
            if (tail == null) {
                throw new EmptyStackException();
            }
            this.mappings.remove(this.tail.mapping);

            final ResourceModelMetaData metadata = this.tail.mapping.getMetadata();
            Count count = this.metaData.remove(metadata);
            if (count.decrement() != 0) {
                this.metaData.put(metadata, count);
            }

            tail = tail.previous;
            return mappings.size();
        }


        @SuppressWarnings("unchecked")
        public <T> Mapping<T> get(Mapping<?> mapping) {
            return this.mappings.get(mapping);
        }

        public Mapping<?> peek() {
            return tail == null ? null : tail.mapping;
        }

        public Iterable<Mapping> getMappings() {
            return this.mappings.keySet();
        }

        public boolean contains(ResourceModelMetaData metadata) {
            return this.metaData.containsKey(metadata);
        }

        /**
         * A backwards-linked map entry representing a mapping and its predecessor in the stack.
         * This is mostly because there is no decent stack datastructure in standard java
         * that combines Stack-Semantics with Set Semantics (Linear iterations, get by position and constant-time contains operations).
         *
         * @param <T>
         */
        private static class RecordedMapping<T> {
            private final Mapping<T> mapping;
            private RecordedMapping<?> previous;

            private RecordedMapping(Mapping<T> mapping, RecordedMapping<?> previous) {
                this.mapping = mapping;
                this.previous = previous;
            }
        }

        /**
         * A mutable key occurrence count in a map.
         *
         * @author Olaf Otto
         */
        private static class Count {
            private int i = 1;

            public void add(Count other) {
                i += other.i;
            }

            int decrement() {
                --i;
                return i;
            }
        }
    }

    // Recursive mappings always occurs within the same thread.
    private final ThreadLocal<MappingStack> ongoingMappings = withInitial(MappingStack::new);

    /**
     * Contract: When invoked and <code>true</code> is returned,
     * one <em>must</em> invoke {@link #pop()} after the corresponding mapping was executed.<br />
     * Otherwise, a leak in the form of persisting thread-local attributes is introduced.
     *
     * @param mapping must not be <code>null</code>.
     * @return The already ongoing mapping, or <code>null</code> if the given mapping has not occurred
     * yet. The provided mapping <em>must</em> only be executed if this method returns <code>null</code>.
     * Otherwise, the execution results in an infinite loop.
     */
    <T> Mapping<T> push(Mapping<T> mapping) {
        if (mapping == null) {
            throw new IllegalArgumentException("Method argument mapping must not be null");
        }
        MappingStack mappingStack = this.ongoingMappings.get();
        @SuppressWarnings("unchecked")
        Mapping<T> alreadyExistingMapping = mappingStack.get(mapping);
        if (alreadyExistingMapping == null) {
            mappingStack.push(mapping);
        }
        return alreadyExistingMapping;
    }

    /**
     * Ends a mapping that was {@link #push(Mapping) begun}.
     */
    public void pop() {
        MappingStack mappingStack = this.ongoingMappings.get();
        if (mappingStack.pop() == 0) {
            this.ongoingMappings.remove();
        }
    }

    /**
     * @return An unsafe view of the ongoing mappings. Modifications to the returned set will modify
     * the state of this instance. Never null.
     */
    Iterable<Mapping> getMappingStack() {
        return this.ongoingMappings.get().getMappings();
    }

    /**
     * @return The mapping that was last {@link #push(Mapping) pushed} and not yet {@link #pop() popped}.
     */
    Mapping<?> peek() {
        return this.ongoingMappings.get().peek();
    }

    /**
     * @param metadata must not be <code>null</code>.
     * @return whether there is an ongoing (parent) mapping for the resource model represented by the provided
     * meta data.
     */
    boolean hasOngoingMapping(ResourceModelMetaData metadata) {
        if (metadata == null) {
            throw new IllegalArgumentException("Method argument metadata must not be null");
        }
        return this.ongoingMappings.get().contains(metadata);
    }
}
