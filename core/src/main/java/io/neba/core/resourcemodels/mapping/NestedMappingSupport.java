/**
 * Copyright 2013 the original author or authors.
 * <p/>
 * Licensed under the Apache License, Version 2.0 the "License";
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/

package io.neba.core.resourcemodels.mapping;

import io.neba.core.resourcemodels.metadata.ResourceModelMetaData;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.springframework.util.Assert.notNull;

/**
 * Provides thread-local tracking of mapping invocations in order to support cycles in mappings
 * and gather statistical data regarding mapping depths of
 * {@link io.neba.api.annotations.ResourceModel resource models}.
 *
 * @author Olaf Otto
 */
@Service
@SuppressWarnings("rawtypes")
public class NestedMappingSupport {
    /**
     * Represents the stack of the currently ongoing mappings.
     *
     * @author Olaf Otto
     */
    private static class OngoingMappings {
        private final Map<Mapping, Mapping> mappings = new LinkedHashMap<>();
        private final Map<ResourceModelMetaData, Count> metaData = new HashMap<>();

        public void add(Mapping<?> mapping) {
            this.mappings.put(mapping, mapping);
            Count value = new Count();
            Count previous = this.metaData.put(mapping.getMetadata(), value);
            if (previous != null) {
                value.add(previous);
            }
        }

        public void remove(Mapping<?> mapping) {
            this.mappings.remove(mapping);
            Count count = this.metaData.get(mapping.getMetadata());
            if (count != null && count.decrement() == 0) {
                this.metaData.remove(mapping.getMetadata());
            }
        }

        @SuppressWarnings("unchecked")
        public <T> Mapping<T> get(Mapping<T> mapping) {
            return this.mappings.get(mapping);
        }

        public boolean contains(ResourceModelMetaData metaData) {
            return this.metaData.containsKey(metaData);
        }

        public boolean isEmpty() {
            return this.mappings.isEmpty();
        }

        public Set<Mapping> getMappings() {
            return this.mappings.keySet();
        }

        public Set<ResourceModelMetaData> getMetaData() {
            return this.metaData.keySet();
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

            public int decrement() {
                --i;
                return i;
            }
        }
    }

    // Recursive mappings always occurs within the same thread.
    private final ThreadLocal<OngoingMappings> ongoingMappings = new ThreadLocal<>();

    /**
     * Contract: When invoked and <code>null</code> is returned,
     * one <em>must</em> invoke {@link #end(Mapping)} after the corresponding mapping was executed.<br />
     * Otherwise, a leak in the form of persisting thread-local attributes is introduced.
     *
     * @param mapping must not be <code>null</code>.
     * @return The already ongoing mapping, or <code>null</code> if the given mapping has not occurred
     *         yet. The provided mapping <em>must</em> only be executed if this emthod returns <code>null</code>.
     *         Otherwise, the execution results in an infinite loop.
     */
    public <T> Mapping<T> begin(Mapping<T> mapping) {
        notNull(mapping, "Method argument mapping must not be null.");
        OngoingMappings ongoingMappings = getOrCreateMappings();
        @SuppressWarnings("unchecked")
        Mapping<T> alreadyExistingMapping = ongoingMappings.get(mapping);
        if (alreadyExistingMapping == null) {
            trackNestedMapping(ongoingMappings);
            ongoingMappings.add(mapping);
        }
        return alreadyExistingMapping;
    }

    /**
     * Ends a mapping that was {@link #begin(Mapping) begun}.
     *
     * @param mapping must not be <code>null</code>.
     */
    public void end(Mapping mapping) {
        notNull(mapping, "Method argument mapping must not be null.");
        OngoingMappings ongoingMappings = this.ongoingMappings.get();
        if (ongoingMappings != null) {
            ongoingMappings.remove(mapping);
            if (ongoingMappings.isEmpty()) {
                this.ongoingMappings.remove();
            }
        }
    }

    /**
     * @return a thread-local, ordered map representing the stack of currently ongoing mappings.
     *         Never <code>null</code> but rather an empty map.
     */
    private OngoingMappings getOrCreateMappings() {
        OngoingMappings ongoingMappings = this.ongoingMappings.get();
        if (ongoingMappings == null) {
            ongoingMappings = new OngoingMappings();
            this.ongoingMappings.set(ongoingMappings);
        }
        return ongoingMappings;
    }

    /**
     * Record a subsequent mapping in the {@link io.neba.core.resourcemodels.metadata.ResourceModelStatistics statistics}
     * of every {@link ResourceModelMetaData resource model} in the current mapping stack.
     */
    private void trackNestedMapping(OngoingMappings ongoingMappings) {
        for (ResourceModelMetaData metaData : ongoingMappings.getMetaData()) {
            metaData.getStatistics().countSubsequentMapping();
        }
    }

    /**
     * @return An unsafe view of the ongoing mappings. Modifications to the returned set will modify
     * the state of this instance. Never null.
     */
    public Set<Mapping> getOngoingMappings() {
        return getOrCreateMappings().getMappings();
    }

    /**
     * @param metadata must not be <code>null</code>.
     * @return whether there is an ongoing (parent) mapping for the resource model represented by the provided
     * meta data.
     */
    public boolean hasOngoingMapping(ResourceModelMetaData metadata) {
        notNull(metadata, "Method argument metadata must not be null.");
        return getOrCreateMappings().contains(metadata);
    }
}
