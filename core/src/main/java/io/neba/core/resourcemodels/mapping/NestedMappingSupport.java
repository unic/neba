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

import javax.annotation.CheckForNull;
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
    // Recording meta information of occurred mappings requires tracking resource to object mappings that may
    // span multiple successive independent mappings, e.g. in case of lazy loading. the recorded meta data
    // thus requires its own scope.
    private final ThreadLocal<Map<Object, Mapping<?>>> recordedMappings = new ThreadLocal<>();

    // Recursive mappings always occurs within the same thread. This thread locale tracks the respective
    // mapping stack.
    private final ThreadLocal<MappingStack> mappingStack = withInitial(() -> new MappingStack(recordedMappings.get()));

    /**
     * Contract: When invoked and <code>null</code> is returned,
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
        MappingStack mappingStack = this.mappingStack.get();
        Mapping<T> alreadyExistingMapping = mappingStack.get(mapping);
        if (alreadyExistingMapping == null) {
            mappingStack.push(mapping);
        }
        return alreadyExistingMapping;
    }

    /**
     * Starts recording all mappings in the current thread until {@link #endRecordingMappings()} is invoked.
     * Recorded mappings are available via {@link #getRecordedMappings()}.
     */
    public void beginRecordingMappings() {
        this.recordedMappings.set(new HashMap<>(128));
    }

    /**
     * @return the currently {@link #beginRecordingMappings() recorded mappings}, or <code>null</code>.
     */
    @CheckForNull
    public Map<Object, Mapping<?>> getRecordedMappings(){
        return this.recordedMappings.get();
    }

    /**
     * Ends recording mappings for this thread and removes all recorded mappings.
     */
    public void endRecordingMappings() {
        this.recordedMappings.remove();
    }

    /**
     * Ends a mapping that was {@link #push(Mapping) begun}. Removes thread-local tracking once the mapping stack is empty.
     */
    void pop() {
        MappingStack mappingStack = this.mappingStack.get();
        if (mappingStack.pop() == 0) {
            this.mappingStack.remove();
        }
    }

    /**
     * @return An unsafe view of the ongoing mappings. Modifications to the returned set will modify
     * the state of this instance. Never null.
     */
    Iterable<Mapping> getMappingStack() {
        return this.mappingStack.get().getMappings();
    }

    /**
     * @return The mapping that was last {@link #push(Mapping) pushed} and not yet {@link #pop() popped}.
     */
    Mapping<?> peek() {
        return this.mappingStack.get().peek();
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
        return this.mappingStack.get().contains(metadata);
    }

    /**
     * Represents the stack of the currently ongoing mappings.
     *
     * @author Olaf Otto
     */
    private static class MappingStack {
        // Contains the stack view of the nested mappings
        private final Map<Mapping, Mapping> stack = new LinkedHashMap<>(8);
        // Contains the occurrence count of each resource model metadata (model type) currently in the stack
        private final Map<ResourceModelMetaData, Count> metaData = new HashMap<>(8);
        // If not null, contains all completed mappings.
        private final Map<Object, Mapping<?>> recordedMappings;

        private Entry<?> tail = null;

        MappingStack(Map<Object, Mapping<?>> recordedMappings) {
            this.recordedMappings = recordedMappings;
        }

        <T> void push(Mapping<T> mapping) {
            this.tail = new Entry<>(mapping, this.tail);
            // We are keeping an occurrence count in order to only remove resource model metadata
            // if no mapping for the corresponding resource model is left on the stack.
            Count occurrenceCount = new Count();
            Count previousCount = this.metaData.put(mapping.getMetadata(), occurrenceCount);
            if (previousCount != null) {
                occurrenceCount.add(previousCount);
            }
            this.stack.put(mapping, mapping);
        }

        /**
         * Removes the last {@link #push(Mapping) pused mapping} form the stack.
         *
         * @return the stack depth after removing the mapping.
         */
        int pop() {
            if (tail == null) {
                throw new EmptyStackException();
            }
            this.stack.remove(this.tail.mapping);

            final ResourceModelMetaData metadata = this.tail.mapping.getMetadata();
            Count count = this.metaData.remove(metadata);
            if (count.decrement() != 0) {
                this.metaData.put(metadata, count);
            }

            if (recordedMappings != null) {
                this.recordedMappings.put(this.tail.mapping.getMappedModel(), this.tail.mapping);
            }

            tail = tail.previous;

            return stack.size();
        }

        Mapping<?> peek() {
            return tail == null ? null : tail.mapping;
        }

        @SuppressWarnings("unchecked")
        <T> Mapping<T> get(Mapping<?> mapping) {
            return this.stack.get(mapping);
        }

        Iterable<Mapping> getMappings() {
            return this.stack.keySet();
        }

        boolean contains(ResourceModelMetaData metadata) {
            return this.metaData.containsKey(metadata);
        }

        /**
         * A backwards-linked map entry representing a mapping and its predecessor in the stack.
         * This is mostly because there is no decent stack datastructure in standard java
         * that combines Stack-Semantics with Set Semantics (Linear iterations, get by position and constant-time contains operations).
         */
        private static class Entry<T> {
            private final Mapping<T> mapping;
            private Entry<?> previous;

            private Entry(Mapping<T> mapping, Entry<?> previous) {
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
}
