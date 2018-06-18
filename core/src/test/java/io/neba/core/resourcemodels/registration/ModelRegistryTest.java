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

import io.neba.api.annotations.ResourceModel;
import io.neba.core.util.OsgiModelSource;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.Bundle;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @author Olaf Otto
 */
@RunWith(MockitoJUnitRunner.class)
public class ModelRegistryTest {
    //CHECKSTYLE:OFF (All classes are the same)
    private static class TargetType1 {}
    private static class ExtendedTargetType1 extends TargetType1 {}
    private static class TargetType2 {}
    private static class TargetType3 {}
    private static class TargetType4 {}
    //CHECKSTYLE:ON
    
    @Mock
	private Bundle bundle;
    @Mock
    private ResourceResolver resolver;

    private Set<ResourceModel> resourceModelAnnotations;
    private long bundleId;
    private Collection<LookupResult> lookedUpModels;
    
    @InjectMocks
    private ModelRegistry testee;

    @Before
    public void setUp() throws LoginException {
        this.resourceModelAnnotations = new HashSet<>();
    	withBundleId(12345L);
    }
    
    @Test
    public void testRegistryEmptiesOnShutdown() {
        withModelSources(2);

        assertRegistryHasModels(2);

        shutdownRegistry();

        assertRegistryIsEmpty();
    }

    @Test
    public void testUnregistrationOfModelsWhenSourceBundleIsRemoved() throws Exception {
        withModelSources(2);

        assertRegistryHasModels(2);

        removeBundle();

        assertRegistryIsEmpty();
    }

    @Test
    public void testModelSourceLookupByResourceType() throws Exception {
        withModelSources(10);

        assertRegistryHasModels(10);
        assertRegistryFindsResourceModelsByResourceType();
    }

    /**
     * The repeated lookup tests the caching behavior since a cache
     * is used if same lookup occurs more than once. 
     */
    @Test
    public void testRepeatedModelSourceLookupByResourceType() throws Exception {
        withModelSources(10);

        assertRegistryHasModels(10);
        assertRegistryFindsResourceModelsByResourceType();
        assertRegistryFindsResourceModelsByResourceType();
    }
    
    @Test
    public void testModelSourceLookupByResourceSuperType() throws Exception {
        withModelSources(10);

        assertRegistryHasModels(10);
        assertRegistryFindsResourceModelsByResourceSupertype();
    }
    
    @Test
    public void testModelSourceLookupForMostSpecificMapping() throws Exception {
        withResourceModel("some/resourcetype");
        withResourceModel("some/resourcetype/supertype");

        withModelSourcesForAllResourceModels();

        lookupMostSpecificModelSources(mockResourceWithResourceSuperType("some/resourcetype", "some/resourcetype/supertype"));

        assertRegistryHasModels(2);
        assertNumberOfLookedUpModelSourcesIs(1);
    }

    /**
     * The repeated lookup tests the caching behavior since a cache
     * is used if same lookup occurs more than once. 
     */
    @Test
    public void testRepeatedModelSourceLookupForMostSpecificMapping() throws Exception {
        withResourceModel("some/resourcetype");
        withResourceModel("some/resourcetype/supertype");

        withModelSourcesForAllResourceModels();

        lookupMostSpecificModelSources(mockResourceWithResourceSuperType("some/resourcetype", "some/resourcetype/supertype"));

        assertRegistryHasModels(2);
        assertNumberOfLookedUpModelSourcesIs(1);

        lookupMostSpecificModelSources(mockResourceWithResourceSuperType("some/resourcetype", "some/resourcetype/supertype"));

        assertRegistryHasModels(2);
        assertNumberOfLookedUpModelSourcesIs(1);
    }

    @Test
    public void testMultipleMappingsToSameResourceType() throws Exception {
        withResourceModel("some/resourcetype");
        withResourceModel("some/resourcetype");
        withModelSourcesForAllResourceModels();
        lookupMostSpecificModelSources(mockResourceWithResourceType("some/resourcetype"));
        assertNumberOfLookedUpModelSourcesIs(2);
    }
    
    @Test
    public void testRemovalOfBundleWithModelforSameResourceType() throws Exception {
        withResourceModel("some/resourcetype");
        withBundleId(1);
        withModelForType("some/resourcetype", TargetType1.class);
        withBundleId(2);
        withModelForType("some/resourcetype", TargetType2.class);

        lookupMostSpecificModelSources(mockResourceWithResourceType("some/resourcetype"));
        assertNumberOfLookedUpModelSourcesIs(2);

        removeBundle();
        lookupMostSpecificModelSources(mockResourceWithResourceType("some/resourcetype"));
        assertNumberOfLookedUpModelSourcesIs(1);
        
        withBundleId(1);
        removeBundle();
        lookupMostSpecificModelSources(mockResourceWithResourceType("some/resourcetype"));
        assertLookedUpModelSourcesAreNull();
    }
    
    @Test
    public void testNoMappingsToResourceType() throws Exception {
        withModelSourcesForAllResourceModels();

        lookupMostSpecificModelSources(mockResourceWithResourceType("some/resourcetype"));

        assertLookedUpModelSourcesAreNull();
    }
    
    @Test
    public void testLookupOfModelSourceForSpecificTypeWithSingleMapping() throws Exception {
        withModelForType("some/resourcetype", TargetType1.class);

        Resource resource = mockResourceWithResourceType("some/resourcetype");

        lookupModelSourcesForType(TargetType1.class, resource);

        assertLookedUpModelSourcesAreNotNull();
        assertLookedUpModelTypesAre(TargetType1.class);
    }

    @Test
    public void testLookupOfModelSourceForSpecificTypeWithMultipleCompatibleModels() throws Exception {
        withModelForType("some/resourcetype/parent", TargetType1.class);
        withModelForType("some/resourcetype", ExtendedTargetType1.class);

        Resource resource = mockResourceWithResourceSuperType("some/resourcetype", "some/resourcetype/parent");

        lookupModelSourcesForType(TargetType1.class, resource);

        assertLookedUpModelSourcesAreNotNull();
        assertNumberOfLookedUpModelSourcesIs(1);
    }

    @Test
    public void testlookupOfModelSourceForSpecificTypeWithoutModel() throws Exception {
        Resource resource = mockResourceWithResourceType("some/resourcetype");

        lookupModelSourcesForType(TargetType1.class, resource);

        assertLookedUpModelSourcesAreNull();
    }

    /**
     * Multiple models may apply to the same sling resource type. When queried
     * for a model compatible to a specific java type, only compatible models
     * must be provided by the registry.
     */
    @Test
    public void testLookupOfModelSourceForTypeWithMultipleIncompatibleModels() throws Exception {
        withModelForType("some/resourcetype", TargetType1.class);
        withModelForType("some/resourcetype", TargetType2.class);

        Resource resource = mockResourceWithResourceType("some/resourcetype");

        lookupModelSourcesForType(TargetType1.class, resource);

        assertLookedUpModelSourcesAreNotNull();
        assertLookedUpModelTypesAre(TargetType1.class);
    }

    /**
     * Different models may be provided for the same resource type. The registry must
     * always provide the model compatible for the desired type, regardless of whether the
     * mapping information was cached.
     */
    @Test
    public void testRepeatedLookupOfModelWithTargetType() throws Exception {
        withModelForType("some/resourcetype", TargetType1.class);
        withModelForType("some/resourcetype", TargetType2.class);
        withModelForType("some/resourcetype", TargetType3.class);
        withModelForType("some/resourcetype", TargetType4.class);

        Resource resource = mockResourceWithResourceType("some/resourcetype");

        lookupModelSourcesForType(TargetType1.class, resource);
        assertLookedUpModelSourcesAreNotNull();
        assertLookedUpModelTypesAre(TargetType1.class);
        
        lookupModelSourcesForType(TargetType1.class, resource);
        assertLookedUpModelSourcesAreNotNull();
        assertLookedUpModelTypesAre(TargetType1.class);

        lookupModelSourcesForType(TargetType3.class, resource);
        assertLookedUpModelSourcesAreNotNull();
        assertLookedUpModelTypesAre(TargetType3.class);

        lookupModelSourcesForType(TargetType2.class, resource);
        assertLookedUpModelSourcesAreNotNull();
        assertLookedUpModelTypesAre(TargetType2.class);

        lookupModelSourcesForType(TargetType4.class, resource);
        assertLookedUpModelSourcesAreNotNull();
        assertLookedUpModelTypesAre(TargetType4.class);
    }

    /**
     * Multiple models may be compatible to the same java type and resource type. The compatible
     * models must be provided by the registry.
     */
    @Test
    public void testLookupOfModelSourceForTypeWithMultipleCompatibleModels() throws Exception {
        withModelForType("some/resourcetype", TargetType1.class);
        withModelForType("some/resourcetype", ExtendedTargetType1.class);

        Resource resource = mockResourceWithResourceType("some/resourcetype");

        lookupModelSourcesForType(TargetType1.class, resource);
        assertLookedUpModelSourcesAreNotNull();
		assertNumberOfLookedUpModelSourcesIs(2);
    }

    /**
     * One may query the model registry for a model with a specific type and specific name for a given resource.
     * If a model with the specific name exists, the registry must return it.
     */
    @Test
    public void testLookupOfModelWithSpecificModelName() throws Exception {
        withModelForType("some/resourcetype", TargetType1.class, "junitModelOne");
        withModelForType("some/resourcetype", ExtendedTargetType1.class, "junitModelTwo");

        Resource resource = mockResourceWithResourceType("some/resourcetype");

        lookupModelSourcesWithModelName("junitModelTwo", resource);
        assertLookedUpModelTypesAre(ExtendedTargetType1.class);
    }

    /**
     * The behavior tested in {@link #testLookupOfModelWithSpecificModelName()} above
     * must be still consistent when the result is fetched from the cache.
     */
    @Test
    public void testCachedLookupOfModelWithSpecificModelName() throws Exception {
        withModelForType("some/resourcetype", TargetType1.class, "junitModelOne");
        withModelForType("some/resourcetype", ExtendedTargetType1.class, "junitModelTwo");

        Resource resource = mockResourceWithResourceType("some/resourcetype");

        lookupModelSourcesWithModelName("junitModelTwo", resource);
        // The second request also tests the result from the cache
        lookupModelSourcesWithModelName("junitModelTwo", resource);

        assertLookedUpModelTypesAre(ExtendedTargetType1.class);
    }

    @Test
    public void testLookupOfModelWithSpecificModelNameProvidesMostSpecificModel() throws Exception {
        withModelForType("some/resourcetype", TargetType1.class, "junitModel");
        withModelForType("some/resource/supertype", ExtendedTargetType1.class, "junitModel");

        Resource resource = mockResourceWithResourceSuperType("some/resourcetype", "some/resource/supertype");

        lookupModelSourcesWithModelName("junitModel", resource);

        assertLookedUpModelTypesAre(TargetType1.class);
    }

    /**
     * One may query the model registry for a model with a specific type and specific name for a given resource.
     * If no model with the specific name exists, the registry must return no model.
     */
    @Test
    public void testLookupOfModelWithSpecificNonexistentModelName() throws Exception {
        withModelForType("some/resourcetype", TargetType1.class, "junitModelOne");
        withModelForType("some/resourcetype", ExtendedTargetType1.class, "junitModelTwo");

        Resource resource = mockResourceWithResourceType("some/resourcetype");

        lookupModelSourcesWithModelName("junitModelThree", resource);

        assertLookedUpModelSourcesAreNull();
    }

    /**
     * The behavior tested in {@link #testLookupOfModelWithSpecificNonexistentModelName()} above
     * must be still consistent when the result is fetched from the cache.
     */
    @Test
    public void testCachedLookupOfModelWithSpecificNonexistentModelName() throws Exception {
        withModelForType("some/resourcetype", TargetType1.class, "junitModelOne");
        withModelForType("some/resourcetype", ExtendedTargetType1.class, "junitModelTwo");

        Resource resource = mockResourceWithResourceType("some/resourcetype");

        lookupModelSourcesWithModelName("junitModelThree", resource);
        // The second request also tests the result from the cache
        lookupModelSourcesWithModelName("junitModelThree", resource);

        assertLookedUpModelSourcesAreNull();
    }

    @Test
    public void testLookupOfAllModelsForResource() throws Exception {
        withModelForType("some/resourcetype/parent", TargetType1.class);
        withModelForType("some/resourcetype", TargetType2.class);

        Resource resource = mockResourceWithResourceSuperType("some/resourcetype", "some/resourcetype/parent");

        lookupAllModelSourcesFor(resource);

        assertLookedUpModelSourcesAreNotNull();
        assertLookedUpModelTypesAre(TargetType1.class, TargetType2.class);
    }

    /**
     * Make sure that models for a resource's primary type do not depend on the sling:resourceType of the resource.
     */
    @Test
    public void testResourceMappingForSameSlingResourceTypeAndDeviatingPrimaryType() throws Exception {
        withModelForType("some:JcrType", TargetType1.class);

        Resource resource = mockResourceWithResourceType("some/resourcetype");

        withPrimaryType(resource, "some:JcrType");
        lookupModelSourcesForType(TargetType1.class, resource);
        assertNumberOfLookedUpModelSourcesIs(1);

        withPrimaryType(resource, "nt:unstructured");
        lookupModelSourcesForType(TargetType1.class, resource);
        assertLookedUpModelSourcesAreNull();
    }


    /**
     * Resource may share the same sling:resourceType but have different jcr:primaryTypes. Differences in the
     * primary type must not affect the resources relationship to models mapping to their sling:resourceType.
     */
    @Test
    public void testResourceMappingToSameModelWithDeviatingPrimaryType() throws Exception {
        withModelForType("my/page/type", TargetType1.class);

        Resource resource = mockResourceWithResourceType("my/page/type");
        withPrimaryType(resource, "some:JcrType");

        lookupModelSourcesForType(TargetType1.class, resource);
        assertNumberOfLookedUpModelSourcesIs(1);

        withPrimaryType(resource, "nt:unstructured");
        lookupModelSourcesForType(TargetType1.class, resource);
        assertNumberOfLookedUpModelSourcesIs(1);
    }

    /**
     * Besides the primary type, sling resource type and sling resource super type of a node,
     * a node may also have mixin types to which a model applies. Thus, mixin types must be part of a
     * resource type - model type relationship and also respected when the relationship is cached.
     */
    @Test
    public void testLookupDependsOnMixinTypes() throws Exception {
        withModelForType("mix:SomeMixing", TargetType1.class);

        Resource resource = mockResourceWithResourceType("my/page/type");
        withPrimaryType(resource, "nt:unstructured");

        withMixinTypes(resource, "mix:SomeMixing", "mix:OtherMixin");

        lookupModelSourcesForType(TargetType1.class, resource);
        assertNumberOfLookedUpModelSourcesIs(1);

        withMixinTypes(resource, "mix:DifferentMixin", "mix:OtherMixin");

        lookupModelSourcesForType(TargetType1.class, resource);
        assertLookedUpModelSourcesAreNull();
    }

    /**
     * The sling resource super type of a resource may stem from either the <em>implicit</em> resource super
     * type, i.e. the resource type's super type, retrieved via {@link ResourceResolver#getParentResourceType(Resource)} or from
     * a supertype explicitly specified in the "sling:resourceSuperType" property of a resource, retrieved via
     * {@link Resource#getResourceSuperType()}. The latter is overriding the former, thus the registry must be sensitive to
     * an explicitly defined sling:resourceSuperType.
     */
    @Test
    public void testCachedLookupDependsOnExplicitlyDefinedResourceSuperType() throws Exception {
        withModelForType("some/super/type", TargetType1.class);

        Resource resource = mockResourceWithResourceSuperType("my/page/type", "some/super/type");
        withPrimaryType(resource, "nt:unstructured");

        lookupModelSourcesForType(TargetType1.class, resource);
        assertNumberOfLookedUpModelSourcesIs(1);

        resource = mockResourceWithResourceType("my/page/type");
        withPrimaryType(resource, "nt:unstructured");

        lookupModelSourcesForType(TargetType1.class, resource);
        assertLookedUpModelSourcesAreNull();
    }

    /**
     * Requires the {@link Node} to have been mocked before hand, e.g. usig {@link #withPrimaryType(Resource, String)}.
     */
    private void withMixinTypes(Resource resource, String... mixins) throws RepositoryException {
        Node node = resource.adaptTo(Node.class);
        NodeType[] mixinTypes = new NodeType[mixins.length];
        for (int i = 0; i < mixins.length; ++i) {
            mixinTypes[i] = mock(NodeType.class);
            when(mixinTypes[i].getName()).thenReturn(mixins[i]);
        }
        if (node != null) {
            when(node.getMixinNodeTypes()).thenReturn(mixinTypes);
        }
    }

    private void withBundleId(final long withBundleId) {
        this.bundleId = withBundleId;
        when(this.bundle.getBundleId()).thenReturn(bundleId);
    }

    private void withPrimaryType(Resource resource, String nodeTypeName) throws RepositoryException {
        Node node = mock(Node.class);
        NodeType nodeType = mock(NodeType.class);
        when(node.getPrimaryNodeType()).thenReturn(nodeType);
        when(nodeType.getName()).thenReturn(nodeTypeName);
        when(resource.adaptTo(Node.class)).thenReturn(node);
    }

    private void lookupAllModelSourcesFor(Resource resource) {
        this.lookedUpModels = this.testee.lookupAllModels(resource);
    }

    private void lookupMostSpecificModelSources(Resource resource) {
        this.lookedUpModels = this.testee.lookupMostSpecificModels(resource);
    }

    private void lookupModelSourcesForType(Class<?> targetType, Resource resource) {
        this.lookedUpModels = this.testee.lookupMostSpecificModels(resource, targetType);
    }

    private void lookupModelSourcesWithModelName(String modelName, Resource resource) {
        this.lookedUpModels = this.testee.lookupMostSpecificModels(resource, modelName);
    }

    private void withResourceModel(String resourceType) {
        ResourceModel annotation = mock(ResourceModel.class);
        when(annotation.types()).thenReturn(new String[] {resourceType});
        this.resourceModelAnnotations.add(annotation);
    }

    private void assertLookedUpModelTypesAre(Class<?>... types) {
        assertThat(this.lookedUpModels).extracting("source.modelType").containsOnly((Object[]) types);
    }

	private void assertNumberOfLookedUpModelSourcesIs(int i) {
        assertThat(this.lookedUpModels).hasSize(i);
	}
        
    private void assertLookedUpModelSourcesAreNull() {
        assertThat(this.lookedUpModels).isNull();
    }

    private void assertLookedUpModelSourcesAreNotNull() {
        assertThat(this.lookedUpModels).isNotNull();
    }

    private void assertRegistryIsEmpty() {
        assertRegistryHasModels(0);
    }
    
    private void assertRegistryFindsResourceModelsByResourceType() {
        for (ResourceModel resourceModel : this.resourceModelAnnotations) {
            String resourceTypeName = resourceModel.types()[0];
            Resource resource = mockResourceWithResourceType(resourceTypeName);
            Collection<LookupResult> models = this.testee.lookupMostSpecificModels(resource);
            assertThat(models).hasSize(1);
        }
    }

    private void assertRegistryFindsResourceModelsByResourceSupertype() {
        for (ResourceModel resourceModel : this.resourceModelAnnotations) {
            String resourceTypeName = resourceModel.types()[0];
            Resource resource = mockResourceWithSupertype(resourceTypeName);
            Collection<LookupResult> models = this.testee.lookupMostSpecificModels(resource);
            assertThat(models).hasSize(1);
        }
    }

    private void assertRegistryHasModels(int i) {
    	assertThat(this.testee.getModelSources()).hasSize(i);
    }
    
    private Resource mockResourceWithResourceType(String resourceTypeName) {
        return mockResourceWithResourceSuperType(resourceTypeName, null);
    }

    private Resource mockResourceWithResourceSuperType(String resourceTypeName, String resourceSuperType) {
        Resource resource = mock(Resource.class);

        when(resource.getResourceResolver()).thenReturn(this.resolver);
        when(resource.getResourceType()).thenReturn(resourceTypeName);
        when(resource.getResourceSuperType()).thenReturn(resourceSuperType);
        when(this.resolver.getParentResourceType(resourceTypeName)).thenReturn(resourceSuperType);
        return resource;
    }

    private Resource mockResourceWithSupertype(String resourceSuperTypeTypeName) {
        final String resourceTypeName = "childOf/" + resourceSuperTypeTypeName;
        return mockResourceWithResourceSuperType(resourceTypeName, resourceSuperTypeTypeName);
    }
    
    private void shutdownRegistry() {
        this.testee.deActivate();
    }

    private void withModelSources(int i) {
        for (int k = 0; k < i; ++k) {
            String resourceType = "/mock/resourcetype/" + k;
            withResourceModel(resourceType);
        }
        withModelSourcesForAllResourceModels();
    }
    
    private void withModelForType(String resourceType, Class modelType) {
        withModelForType(resourceType, modelType, "defaultModelName");
    }

    @SuppressWarnings("unchecked")
    private void withModelForType(String resourceType, @SuppressWarnings("rawtypes") Class modelType, String modelModelName) {
        OsgiModelSource<?> source = mock(OsgiModelSource.class);
        when(source.getModelType()).thenReturn(modelType);
        when(source.getBundleId()).thenReturn(this.bundleId);
        when(source.getModelName()).thenReturn(modelModelName);
        this.testee.add(new String[] {resourceType}, source);
    }

    private void withModelSourcesForAllResourceModels() {
        for (ResourceModel model : this.resourceModelAnnotations) {
            OsgiModelSource<?> source = mock(OsgiModelSource.class);
            when(source.getBundleId()).thenReturn(this.bundleId);
            this.testee.add(model.types(), source);
        }
    }
    
    private void removeBundle() {
        this.testee.removeResourceModels(this.bundle);
    }
}
