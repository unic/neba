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

package io.neba.core.resourcemodels.mapping;

import org.springframework.stereotype.Service;

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
public class CyclicMappingSupport {
    // Recursive mappings always occurs within the same thread.
    private final ThreadLocal<Map<Mapping, Mapping>> ongoingMappings = new ThreadLocal<Map<Mapping, Mapping>>();

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
        Map<Mapping, Mapping> ongoingMappings = getOrCreateMappings();
        @SuppressWarnings("unchecked")
        Mapping<T> alreadyExistingMapping = ongoingMappings.get(mapping);
        if (alreadyExistingMapping == null) {
            trackDepths(ongoingMappings);
            ongoingMappings.put(mapping, mapping);
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
        Map<Mapping, Mapping> ongoingMappings = this.ongoingMappings.get();
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
    private Map<Mapping, Mapping> getOrCreateMappings() {
        Map<Mapping, Mapping> ongoingMappings = this.ongoingMappings.get();
        if (ongoingMappings == null) {
            ongoingMappings = new LinkedHashMap<Mapping, Mapping>();
            this.ongoingMappings.set(ongoingMappings);
        }
        return ongoingMappings;
    }

    /**
     * The ordered map of mappings also represents the mapping stack, i.e.
     * the depth of the current branch of the mapping tree.
     * The topmost mapping (root) thus contains a mapping with
     * a depth equal to the length of the ongoing mappings. This information is provided to the
     * {@link io.neba.core.resourcemodels.metadata.ResourceModelStatistics}.
     */
    private void trackDepths(Map<Mapping, Mapping> ongoingMappings) {
        int depth = ongoingMappings.size();
        for (Mapping ongoing : ongoingMappings.values()) {
            ongoing.getMetadata().getStatistics().countMappings(depth--);
        }
    }

    public Set<Mapping> getOngoingMappings() {
        return getOrCreateMappings().keySet();
    }
}
