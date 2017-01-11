/*
  Copyright 2013 the original author or authors.

  Licensed under the Apache License, Version 2.0 the "License";
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
*/

package io.neba.core.resourcemodels.registration;

import io.neba.core.blueprint.EventhandlingBarrier;
import io.neba.core.util.ConcurrentDistinctMultiValueMap;
import io.neba.core.util.Key;
import io.neba.core.util.MatchedBundlesPredicate;
import io.neba.core.util.OsgiBeanSource;
import org.apache.commons.collections.CollectionUtils;
import org.apache.sling.api.resource.Resource;
import org.osgi.framework.Bundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static io.neba.core.resourcemodels.registration.MappableTypeHierarchy.mappableTypeHierarchyOf;
import static io.neba.core.util.BundleUtil.displayNameOf;
import static java.util.Collections.unmodifiableCollection;

/**
 * Contains {@link OsgiBeanSource model sources} associated to
 * {@link MappableTypeHierarchy mappable types} and the corresponding logic to
 * lookup these relationships.
 *
 * @author Olaf Otto
 */
@Service
public class ModelRegistry {
    private static final Object NULL_VALUE = new Object();
    private static final long EVERY_30_SECONDS = 30 * 1000;

    /**
     * Generate a {@link Key} representing both the
     * {@link org.apache.sling.api.resource.Resource#getResourceType() sling resource type},
     * {@link javax.jcr.Node#getPrimaryNodeType() primary node type} and the {@link Node#getMixinNodeTypes() mixin types}
     * of the resource, if any. Rationale: Resources may have the same <code>sling:resourceType</code>, but different primary or mixin types,
     * thus potentially producing different results when mapped. The cache must thus use these
     * types as a key for cached adaptation results.<br />
     *
     * @param resource           must not be <code>null</code>.
     * @param furtherKeyElements can be <code>null</code>
     * @return never <code>null</code>.
     */
    private static Key key(Resource resource, Object... furtherKeyElements) {
        Key key;
        final Key furtherElementsKey = furtherKeyElements == null ? null : new Key(furtherKeyElements);
        Node node = resource.adaptTo(Node.class);
        if (node != null) {
            try {
                key = new Key(
                        resource.getResourceType(),
                        resource.getResourceSuperType(),
                        node.getPrimaryNodeType().getName(),
                        Arrays.toString(node.getMixinNodeTypes()),
                        furtherElementsKey);
            } catch (RepositoryException e) {
                throw new RuntimeException("Unable to retrieve the primary type of " + resource + ".", e);
            }
        } else {
            key = new Key(resource.getResourceType(), furtherElementsKey);
        }
        return key;
    }

    /**
     * @param source can be <code>null</code>.
     * @param <T>    the collection type.
     * @return the collection, or <code>null</code> if the collection
     *         id <code>null</code> or empty.
     */
    private static <T> Collection<T> nullIfEmpty(Collection<T> source) {
        return source == null || source.isEmpty() ? null : source;
    }

    /**
     * @param sources        can be <code>null</code>.
     * @param compatibleType can be <code>null</code>.
     * @return the original collection if sources or compatibleType are
     *         <code>null</code>, or a collection representing the models
     *         compatible to the given type.
     */
    private static Collection<OsgiBeanSource<?>> filter(Collection<OsgiBeanSource<?>> sources, Class<?> compatibleType) {
        Collection<OsgiBeanSource<?>> compatibleSources = sources;
        if (sources != null && compatibleType != null) {
            compatibleSources = new ArrayList<>(sources.size());
            for (OsgiBeanSource<?> source : sources) {
                if (compatibleType.isAssignableFrom(source.getBeanType())) {
                    compatibleSources.add(source);
                }
            }
        }
        return compatibleSources;
    }

    /**
     * @param sources        can be <code>null</code>.
     * @param beanName can be <code>null</code>.
     * @return the original collection if sources or beanName are
     *         <code>null</code>, or a collection representing the models
     *         who's {@link io.neba.core.util.OsgiBeanSource#getBeanName()} bean name}
     *         is equal to the given bean name.
     */
    private static Collection<OsgiBeanSource<?>> filter(Collection<OsgiBeanSource<?>> sources, String beanName) {
        Collection<OsgiBeanSource<?>> sourcesWithBeanName = sources;
        if (sources != null && beanName != null) {
            sourcesWithBeanName = new ArrayList<>(sources.size());
            for (OsgiBeanSource<?> source : sources) {
                if (beanName.equals(source.getBeanName())) {
                    sourcesWithBeanName.add(source);
                }
            }
        }
        return sourcesWithBeanName;
    }

    private final ConcurrentDistinctMultiValueMap<String, OsgiBeanSource<?>>
            typeNameToBeanSourcesMap = new ConcurrentDistinctMultiValueMap<>();
    private final ConcurrentDistinctMultiValueMap<Key, LookupResult>
            lookupCache = new ConcurrentDistinctMultiValueMap<>();

    private final Map<Key, Object> unmappedTypesCache = new ConcurrentHashMap<>();
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final AtomicInteger state = new AtomicInteger(0);

    /**
     * Finds the most specific models for the given {@link Resource}. The model's bean
     * name must match the provided bean name.
     * @param resource must not be <code>null</code>.
     * @param beanName must not be <code>null</code>.
     * @return the resolved models, or <code>null</code> if no such models exist.
     */
    public Collection<LookupResult> lookupMostSpecificModels(Resource resource, String beanName) {
        if (resource == null) {
            throw new IllegalArgumentException("Method argument resource must not be null.");
        }
        if (beanName == null) {
            throw new IllegalArgumentException("Method argument beanName must not be null.");
        }

        Key key = key(resource, beanName);

        if (isUnmapped(key)) {
            return null;
        }

        Collection<LookupResult> matchingModels = lookupFromCache(key);
        if (matchingModels == null) {
            final int currentStateId = this.state.get();

            matchingModels = resolveMostSpecificBeanSources(resource, beanName);

            if (matchingModels.isEmpty()) {
                markAsUnmapped(key, currentStateId);
            } else {
                cache(key, matchingModels, currentStateId);
            }
        }

        return nullIfEmpty(matchingModels);
    }

    /**
     * Finds the most specific models for the given {@link Resource}, i.e. the
     * first model(s) found when traversing the resource's
     * {@link MappableTypeHierarchy mappable hierarchy}.
     *
     * @param resource must not be <code>null</code>.
     * @return the model sources, or <code>null</code> if no models exist for the resource.
     */
    public Collection<LookupResult> lookupMostSpecificModels(Resource resource) {
        if (resource == null) {
            throw new IllegalArgumentException("Method argument resource must not be null.");
        }

        final Key key = key(resource);

        if (isUnmapped(key)) {
            return null;
        }

        Collection<LookupResult> sources = lookupFromCache(key);

        if (sources == null) {
            final int currentStateId = this.state.get();
            sources = resolveMostSpecificBeanSources(resource);

            if (sources.isEmpty()) {
                markAsUnmapped(key, currentStateId);
            } else {
                cache(key, sources, currentStateId);
            }
        }
        return nullIfEmpty(sources);
    }

    /**
     * Finds the all models for the given {@link Resource}, i.e. the
     * all model(s) found when traversing the resource's
     * {@link MappableTypeHierarchy mappable hierarchy}.
     *
     * @param resource must not be <code>null</code>.
     * @return the model sources, or <code>null</code> if no models exist for the resource.
     */
    public Collection<LookupResult> lookupAllModels(Resource resource) {
        if (resource == null) {
            throw new IllegalArgumentException("Method argument resource must not be null.");
        }

        final Key key = key(resource, "allModels");

        if (isUnmapped(key)) {
            return null;
        }

        Collection<LookupResult> sources = lookupFromCache(key);

        if (sources == null) {
            final int currentStateId = this.state.get();
            sources = resolveBeanSources(resource, null, false);

            if (sources.isEmpty()) {
                markAsUnmapped(key, currentStateId);
            } else {
                cache(key, sources, currentStateId);
            }
        }

        return nullIfEmpty(sources);
    }

    /**
     * Finds the most specific models for the given resource which are
     * {@link Class#isAssignableFrom(Class) assignable to} the target type.
     *
     * @param resource   must not be <code>null</code>.
     * @param targetType must not be <code>null</code>.
     * @return the sources of the models, or <code>null</code> if no such model exists.
     */
    public Collection<LookupResult> lookupMostSpecificModels(Resource resource, Class<?> targetType) {
        if (resource == null) {
            throw new IllegalArgumentException("Method argument resource must not be null.");
        }
        if (targetType == null) {
            throw new IllegalArgumentException("Method argument targetType must not be null.");
        }

        final Key key = key(resource, targetType);

        if (isUnmapped(key)) {
            return null;
        }

        Collection<LookupResult> matchingModels = lookupFromCache(key);
        if (matchingModels == null) {
            final int currentStateId = this.state.get();
            matchingModels = resolveMostSpecificBeanSources(resource, targetType);

            if (matchingModels.isEmpty()) {
                markAsUnmapped(key, currentStateId);
            } else {
                cache(key, matchingModels, currentStateId);
            }
        }

        return nullIfEmpty(matchingModels);
    }

    /**
     * Clears the registry upon shutdown.
     */
    @PreDestroy
    public void shutdown() {
        this.logger.info("The model registry is shutting down.");
        clearRegisteredModels();
        clearLookupCaches();
    }

    /**
     * Removes all resource models originating from the given bundle from this registry.
     *
     * @param bundle must not be <code>null</code>.
     */
    public void removeResourceModels(final Bundle bundle) {
        this.logger.info("Removing resource models of bundle " + displayNameOf(bundle) + "...");
        MatchedBundlesPredicate sourcesWithBundles = new MatchedBundlesPredicate(bundle);
        for (Collection<OsgiBeanSource<?>> values : this.typeNameToBeanSourcesMap.values()) {
            CollectionUtils.filter(values, sourcesWithBundles);
        }
        clearLookupCaches();
        this.logger.info("Removed " + sourcesWithBundles.getFilteredElements()
                + " resource models of bundle " + displayNameOf(bundle) + "...");
    }

    /**
     * @return a shallow copy of all registered sources for models, never
     *         <code>null</code> but rather an empty list.
     */
    public List<OsgiBeanSource<?>> getBeanSources() {
        Collection<Collection<OsgiBeanSource<?>>> sources = this.typeNameToBeanSourcesMap.values();
        List<OsgiBeanSource<?>> linearizedSources = new LinkedList<>();
        sources.forEach(linearizedSources::addAll);
        return linearizedSources;
    }

    /**
     * Adds the type[] -&t; model relationship to the registry.
     *
     * @param types  must not be <code>null</code>.
     * @param source must not be <code>null</code>.
     */
    public void add(String[] types, OsgiBeanSource<?> source) {
        for (String resourceType : types) {
            this.typeNameToBeanSourcesMap.put(resourceType, source);
        }
        clearLookupCaches();
    }

    /**
     * @return all type -&gt; model mappings.
     */
    public Map<String, Collection<OsgiBeanSource<?>>> getTypeMappings() {
        return this.typeNameToBeanSourcesMap.getContents();
    }

    /**
     * Checks whether the {@link OsgiBeanSource sources} of all resource models
     * are still {@link io.neba.core.util.OsgiBeanSource#isValid() valid}. If not,
     * the corresponding model(s) are removed from the registry.
     */
    @Scheduled(fixedRate = EVERY_30_SECONDS)
    public void removeInvalidReferences() {
        if (EventhandlingBarrier.tryBegin()) {
            this.logger.debug("Checking for references to beans from inactive bundles...");
            try {
                for (Collection<OsgiBeanSource<?>> values : this.typeNameToBeanSourcesMap.values()) {
                    for (Iterator<OsgiBeanSource<?>> it = values.iterator(); it.hasNext(); ) {
                        final OsgiBeanSource<?> source = it.next();
                        if (!source.isValid()) {
                            this.logger.info("Reference to " + source + " is invalid, removing.");
                            it.remove();
                            clearLookupCaches();
                        }
                    }
                }
            } finally {
                EventhandlingBarrier.end();
            }
            this.logger.debug("Completed checking for references to beans from inactive bundles.");
        }
    }

    /**
     * Clears all quick lookup caches for resource models, but
     * not the registry itself.
     */
    public synchronized void clearLookupCaches() {
        this.state.incrementAndGet();
        this.lookupCache.clear();
        this.unmappedTypesCache.clear();
        this.logger.debug("Cache cleared.");
    }

    private boolean isUnmapped(Key key) {
        return this.unmappedTypesCache.containsKey(key);
    }

    private Collection<LookupResult> lookupFromCache(Key key) {
        return this.lookupCache.get(key);
    }

    private void clearRegisteredModels() {
        this.typeNameToBeanSourcesMap.clear();
        this.logger.debug("Registry cleared.");
    }

    /**
     * @see #resolveMostSpecificBeanSources(org.apache.sling.api.resource.Resource, Class)
     */
    private Collection<LookupResult> resolveMostSpecificBeanSources(Resource resource) {
        return resolveMostSpecificBeanSources(resource, (Class<?>) null);
    }

    /**
     * @see #resolveBeanSources(org.apache.sling.api.resource.Resource, Class, boolean)
     */
    private Collection<LookupResult> resolveMostSpecificBeanSources(
            Resource resource,
            Class<?> compatibleType) {

        return resolveBeanSources(resource, compatibleType, true);
    }

    /**
     * Finds all {@link OsgiBeanSource bean sources} representing models for the given
     * {@link Resource}.
     *
     * @param resource       must not be <code>null</code>.
     * @param compatibleType can be <code>null</code>. If provided, only models
     *                       compatible to the given type are returned.
     * @param resolveMostSpecific whether to resolve only the most specific models.
     *
     * @return never <code>null</code> but rather an empty collection.
     */
    private Collection<LookupResult> resolveBeanSources(Resource resource, Class<?> compatibleType, boolean resolveMostSpecific) {
        Collection<LookupResult> sources = new ArrayList<>(64);
        for (final String resourceType : mappableTypeHierarchyOf(resource)) {
            Collection<OsgiBeanSource<?>> allSourcesForType = this.typeNameToBeanSourcesMap.get(resourceType);
            Collection<OsgiBeanSource<?>> sourcesForCompatibleType = filter(allSourcesForType, compatibleType);
            if (sourcesForCompatibleType != null && !sourcesForCompatibleType.isEmpty()) {
                sources.addAll(sourcesForCompatibleType.stream().map(source -> new LookupResult(source, resourceType)).collect(Collectors.toList()));
                if (resolveMostSpecific) {
                    break;
                }
            }
        }
        return unmodifiableCollection(sources);
    }

    /**
     * Finds all {@link OsgiBeanSource bean sources} representing models for the given
     * {@link Resource} whos {@link io.neba.core.util.OsgiBeanSource#getBeanName() bean name}
     * matches the given bean name.
     *
     * @param resource must not be <code>null</code>.
     * @param beanName can be <code>null</code>.
     * @return never <code>null</code> but rather an empty collection.
     */
    private Collection<LookupResult> resolveMostSpecificBeanSources(Resource resource, String beanName) {
        Collection<LookupResult> sources = new ArrayList<>();
        for (final String resourceType : mappableTypeHierarchyOf(resource)) {
            Collection<OsgiBeanSource<?>> allSourcesForType = this.typeNameToBeanSourcesMap.get(resourceType);
            Collection<OsgiBeanSource<?>> sourcesWithMatchingBeanName = filter(allSourcesForType, beanName);
            if (sourcesWithMatchingBeanName != null && !sourcesWithMatchingBeanName.isEmpty()) {
                sources.addAll(sourcesWithMatchingBeanName.stream().map(source -> new LookupResult(source, resourceType)).collect(Collectors.toList()));
                break;
            }
        }
        return unmodifiableCollection(sources);
    }

    /**
     * A mapping might apply to any type somewhere within a resource's
     * {@link  MappableTypeHierarchy}. This cache saves the registrar from searching
     * this entire hierarchy each time the model is resolved by remembering a found
     * resource type -&gt; model relationship.
     */
    private void cache(final Key key, final Collection<LookupResult> sources, final int stateId) {
        synchronized (this) {
            if (stateId == this.state.get()) {
                this.lookupCache.put(key, sources);
            }
        }
    }

    private void markAsUnmapped(final Key key, final int stateId) {
        synchronized (this) {
            if (stateId == this.state.get()) {
                this.unmappedTypesCache.put(key, NULL_VALUE);
            }
        }
    }
}