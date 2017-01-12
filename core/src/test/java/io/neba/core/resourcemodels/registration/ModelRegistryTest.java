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
import io.neba.core.util.OsgiBeanSource;
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

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

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
        withBeanSources(2);

        assertRegistryHasModels(2);

        shutdownRegistry();

        assertRegistryIsEmpty();
    }

    @Test
    public void testUnregistrationOfModelsWhenSourceBundleIsRemoved() throws Exception {
        withBeanSources(2);

        assertRegistryHasModels(2);

        removeBundle();

        assertRegistryIsEmpty();
    }

    @Test
    public void testBeanSourceLookupByResourceType() throws Exception {
        withBeanSources(10);

        assertRegistryHasModels(10);
        assertRegistryFindsResourceModelsByResourceType();
    }

    /**
     * The repeated lookup tests the caching behavior since a cache
     * is used if same lookup occurs more than once. 
     */
    @Test
    public void testRepeatedBeanSourceLookupByResourceType() throws Exception {
        withBeanSources(10);

        assertRegistryHasModels(10);
        assertRegistryFindsResourceModelsByResourceType();
        assertRegistryFindsResourceModelsByResourceType();
    }
    
    @Test
    public void testBeanSourceLookupByResourceSuperType() throws Exception {
        withBeanSources(10);

        assertRegistryHasModels(10);
        assertRegistryFindsResourceModelsByResourceSupertype();
    }
    
    @Test
    public void testBeanSourceLookupForMostSpecificMapping() throws Exception {
        withResourceModel("some/resourcetype");
        withResourceModel("some/resourcetype/supertype");

        withBeanSourcesForAllResourceModels();

        lookupMostSpecificBeanSources(mockResourceWithResourceSuperType("some/resourcetype", "some/resourcetype/supertype"));

        assertRegistryHasModels(2);
        assertNumberOfLookedUpBeanSourcesIs(1);
    }

    /**
     * The repeated lookup tests the caching behavior since a cache
     * is used if same lookup occurs more than once. 
     */
    @Test
    public void testRepeatedBeanSourceLookupForMostSpecificMapping() throws Exception {
        withResourceModel("some/resourcetype");
        withResourceModel("some/resourcetype/supertype");

        withBeanSourcesForAllResourceModels();

        lookupMostSpecificBeanSources(mockResourceWithResourceSuperType("some/resourcetype", "some/resourcetype/supertype"));

        assertRegistryHasModels(2);
        assertNumberOfLookedUpBeanSourcesIs(1);

        lookupMostSpecificBeanSources(mockResourceWithResourceSuperType("some/resourcetype", "some/resourcetype/supertype"));

        assertRegistryHasModels(2);
        assertNumberOfLookedUpBeanSourcesIs(1);
    }

    @Test
    public void testMultipleMappingsToSameResourceType() throws Exception {
        withResourceModel("some/resourcetype");
        withResourceModel("some/resourcetype");
        withBeanSourcesForAllResourceModels();
        lookupMostSpecificBeanSources(mockResourceWithResourceType("some/resourcetype"));
        assertNumberOfLookedUpBeanSourcesIs(2);
    }
    
    @Test
    public void testRemovalOfBundleWithModelforSameResourceType() throws Exception {
        withResourceModel("some/resourcetype");
        withBundleId(1);
        withModelForType("some/resourcetype", TargetType1.class);
        withBundleId(2);
        withModelForType("some/resourcetype", TargetType2.class);

        lookupMostSpecificBeanSources(mockResourceWithResourceType("some/resourcetype"));
        assertNumberOfLookedUpBeanSourcesIs(2);

        removeBundle();
        lookupMostSpecificBeanSources(mockResourceWithResourceType("some/resourcetype"));
        assertNumberOfLookedUpBeanSourcesIs(1);
        
        withBundleId(1);
        removeBundle();
        lookupMostSpecificBeanSources(mockResourceWithResourceType("some/resourcetype"));
        assertLookedUpBeanSourcesAreNull();
    }
    
    @Test
    public void testNoMappingsToResourceType() throws Exception {
        withBeanSourcesForAllResourceModels();

        lookupMostSpecificBeanSources(mockResourceWithResourceType("some/resourcetype"));

        assertLookedUpBeanSourcesAreNull();
    }
    
    @Test
    public void testLookupOfBeanSourceForSpecificTypeWithSingleMapping() throws Exception {
        withModelForType("some/resourcetype", TargetType1.class);

        Resource resource = mockResourceWithResourceType("some/resourcetype");

        lookupBeanSourcesForType(TargetType1.class, resource);

        assertLookedUpBeanSourcesAreNotNull();
        assertLookedUpModelTypesAre(TargetType1.class);
    }

    @Test
    public void testLookupOfBeanSourceForSpecificTypeWithMultipleCompatibleModels() throws Exception {
        withModelForType("some/resourcetype/parent", TargetType1.class);
        withModelForType("some/resourcetype", ExtendedTargetType1.class);

        Resource resource = mockResourceWithResourceSuperType("some/resourcetype", "some/resourcetype/parent");

        lookupBeanSourcesForType(TargetType1.class, resource);

        assertLookedUpBeanSourcesAreNotNull();
        assertNumberOfLookedUpBeanSourcesIs(1);
    }

    @Test
    public void testlookupOfBeanSourceForSpecificTypeWithoutModel() throws Exception {
        Resource resource = mockResourceWithResourceType("some/resourcetype");

        lookupBeanSourcesForType(TargetType1.class, resource);

        assertLookedUpBeanSourcesAreNull();
    }

    /**
     * Multiple models may apply to the same sling resource type. When queried
     * for a model compatible to a specific java type, only compatible models
     * must be provided by the registry.
     */
    @Test
    public void testLookupOfBeanSourceForTypeWithMultipleIncompatibleModels() throws Exception {
        withModelForType("some/resourcetype", TargetType1.class);
        withModelForType("some/resourcetype", TargetType2.class);

        Resource resource = mockResourceWithResourceType("some/resourcetype");

        lookupBeanSourcesForType(TargetType1.class, resource);

        assertLookedUpBeanSourcesAreNotNull();
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

        lookupBeanSourcesForType(TargetType1.class, resource);
        assertLookedUpBeanSourcesAreNotNull();
        assertLookedUpModelTypesAre(TargetType1.class);
        
        lookupBeanSourcesForType(TargetType1.class, resource);
        assertLookedUpBeanSourcesAreNotNull();
        assertLookedUpModelTypesAre(TargetType1.class);

        lookupBeanSourcesForType(TargetType3.class, resource);
        assertLookedUpBeanSourcesAreNotNull();
        assertLookedUpModelTypesAre(TargetType3.class);

        lookupBeanSourcesForType(TargetType2.class, resource);
        assertLookedUpBeanSourcesAreNotNull();
        assertLookedUpModelTypesAre(TargetType2.class);

        lookupBeanSourcesForType(TargetType4.class, resource);
        assertLookedUpBeanSourcesAreNotNull();
        assertLookedUpModelTypesAre(TargetType4.class);
    }

    /**
     * Multiple models may be compatible to the same java type and resource type. The compatible
     * models must be provided by the registry.
     */
    @Test
    public void testLookupOfBeanSourceForTypeWithMultipleCompatibleModels() throws Exception {
        withModelForType("some/resourcetype", TargetType1.class);
        withModelForType("some/resourcetype", ExtendedTargetType1.class);

        Resource resource = mockResourceWithResourceType("some/resourcetype");

        lookupBeanSourcesForType(TargetType1.class, resource);
        assertLookedUpBeanSourcesAreNotNull();
		assertNumberOfLookedUpBeanSourcesIs(2);
    }

    /**
     * One may query the model registry for a model with a specific type and specific name for a given resource.
     * If a bean with the specific name exists, the registry must return it.
     */
    @Test
    public void testLookupOfModelWithSpecificBeanName() throws Exception {
        withModelForType("some/resourcetype", TargetType1.class, "junitBeanOne");
        withModelForType("some/resourcetype", ExtendedTargetType1.class, "junitBeanTwo");

        Resource resource = mockResourceWithResourceType("some/resourcetype");

        lookupBeanSourcesWithBeanName("junitBeanTwo", resource);
        assertLookedUpModelTypesAre(ExtendedTargetType1.class);
    }

    /**
     * The behavior tested in {@link #testLookupOfModelWithSpecificBeanName()} above
     * must be still consistent when the result is fetched from the cache.
     */
    @Test
    public void testCachedLookupOfModelWithSpecificBeanName() throws Exception {
        withModelForType("some/resourcetype", TargetType1.class, "junitBeanOne");
        withModelForType("some/resourcetype", ExtendedTargetType1.class, "junitBeanTwo");

        Resource resource = mockResourceWithResourceType("some/resourcetype");

        lookupBeanSourcesWithBeanName("junitBeanTwo", resource);
        // The second request also tests the result from the cache
        lookupBeanSourcesWithBeanName("junitBeanTwo", resource);

        assertLookedUpModelTypesAre(ExtendedTargetType1.class);
    }

    @Test
    public void testLookupOfModelWithSpecificBeanNameProvidesMostSpecificModel() throws Exception {
        withModelForType("some/resourcetype", TargetType1.class, "junitBean");
        withModelForType("some/resource/supertype", ExtendedTargetType1.class, "junitBean");

        Resource resource = mockResourceWithResourceSuperType("some/resourcetype", "some/resource/supertype");

        lookupBeanSourcesWithBeanName("junitBean", resource);

        assertLookedUpModelTypesAre(TargetType1.class);
    }

    /**
     * One may query the model registry for a model with a specific type and specific name for a given resource.
     * If no bean with the specific name exists, the registry must return no model.
     */
    @Test
    public void testLookupOfModelWithSpecificNonexistentBeanName() throws Exception {
        withModelForType("some/resourcetype", TargetType1.class, "junitBeanOne");
        withModelForType("some/resourcetype", ExtendedTargetType1.class, "junitBeanTwo");

        Resource resource = mockResourceWithResourceType("some/resourcetype");

        lookupBeanSourcesWithBeanName("junitBeanThree", resource);

        assertLookedUpBeanSourcesAreNull();
    }

    /**
     * The behavior tested in {@link #testLookupOfModelWithSpecificNonexistentBeanName()} above
     * must be still consistent when the result is fetched from the cache.
     */
    @Test
    public void testCachedLookupOfModelWithSpecificNonexistentBeanName() throws Exception {
        withModelForType("some/resourcetype", TargetType1.class, "junitBeanOne");
        withModelForType("some/resourcetype", ExtendedTargetType1.class, "junitBeanTwo");

        Resource resource = mockResourceWithResourceType("some/resourcetype");

        lookupBeanSourcesWithBeanName("junitBeanThree", resource);
        // The second request also tests the result from the cache
        lookupBeanSourcesWithBeanName("junitBeanThree", resource);

        assertLookedUpBeanSourcesAreNull();
    }

    @Test
    public void testLookupOfAllModelsForResource() throws Exception {
        withModelForType("some/resourcetype/parent", TargetType1.class);
        withModelForType("some/resourcetype", TargetType2.class);

        Resource resource = mockResourceWithResourceSuperType("some/resourcetype", "some/resourcetype/parent");

        lookupAllBeanSourcesFor(resource);

        assertLookedUpBeanSourcesAreNotNull();
        assertLookedUpModelTypesAre(TargetType1.class, TargetType2.class);
    }

    @Test
    public void testRemovalOfInvalidReferencesToModels() throws Exception {
        withInvalidBeanSource("some/resource/type", TargetType1.class);
        assertRegistryHasModels(1);

        removeInvalidReferences();

        assertRegistryHasModels(0);
    }

    @Test
    public void testValidBeanSourcesAreNotRemovedUponConsistencyCheck() throws Exception {
        withModelForType("some/resource/type", TargetType1.class);

        assertRegistryHasModels(1);

        removeInvalidReferences();

        verifySourcesWhereTestedForValidity();
        assertRegistryHasModels(1);
    }

    /**
     * Make sure that models for a resource's primary type do not depend on the sling:resourceType of the resource.
     */
    @Test
    public void testResourceMappingForSameSlingResourceTypeAndDeviatingPrimaryType() throws Exception {
        withModelForType("some:JcrType", TargetType1.class);

        Resource resource = mockResourceWithResourceType("some/resourcetype");

        withPrimaryType(resource, "some:JcrType");
        lookupBeanSourcesForType(TargetType1.class, resource);
        assertNumberOfLookedUpBeanSourcesIs(1);

        withPrimaryType(resource, "nt:unstructured");
        lookupBeanSourcesForType(TargetType1.class, resource);
        assertLookedUpBeanSourcesAreNull();
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

        lookupBeanSourcesForType(TargetType1.class, resource);
        assertNumberOfLookedUpBeanSourcesIs(1);

        withPrimaryType(resource, "nt:unstructured");
        lookupBeanSourcesForType(TargetType1.class, resource);
        assertNumberOfLookedUpBeanSourcesIs(1);
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

        lookupBeanSourcesForType(TargetType1.class, resource);
        assertNumberOfLookedUpBeanSourcesIs(1);

        withMixinTypes(resource, "mix:DifferentMixin", "mix:OtherMixin");

        lookupBeanSourcesForType(TargetType1.class, resource);
        assertLookedUpBeanSourcesAreNull();
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

        lookupBeanSourcesForType(TargetType1.class, resource);
        assertNumberOfLookedUpBeanSourcesIs(1);

        resource = mockResourceWithResourceType("my/page/type");
        withPrimaryType(resource, "nt:unstructured");

        lookupBeanSourcesForType(TargetType1.class, resource);
        assertLookedUpBeanSourcesAreNull();
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
        when(node.getMixinNodeTypes()).thenReturn(mixinTypes);
    }

    private void removeInvalidReferences() {
        this.testee.removeInvalidReferences();
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

    private void lookupAllBeanSourcesFor(Resource resource) {
        this.lookedUpModels = this.testee.lookupAllModels(resource);
    }

    private void lookupMostSpecificBeanSources(Resource resource) {
        this.lookedUpModels = this.testee.lookupMostSpecificModels(resource);
    }

    private void lookupBeanSourcesForType(Class<?> targetType, Resource resource) {
        this.lookedUpModels = this.testee.lookupMostSpecificModels(resource, targetType);
    }

    private void lookupBeanSourcesWithBeanName(String beanName, Resource resource) {
        this.lookedUpModels = this.testee.lookupMostSpecificModels(resource, beanName);
    }

    private void withResourceModel(String resourceType) {
        ResourceModel annotation = mock(ResourceModel.class);
        when(annotation.types()).thenReturn(new String[] {resourceType});
        this.resourceModelAnnotations.add(annotation);
    }

    private void verifySourcesWhereTestedForValidity() {
        for (OsgiBeanSource<?> source : this.testee.getBeanSources()) {
            verify(source).isValid();
        }
    }

    private void assertLookedUpModelTypesAre(Class<?>... types) {
        assertThat(this.lookedUpModels).extracting("source.beanType").containsOnly((Object[]) types);
    }

	private void assertNumberOfLookedUpBeanSourcesIs(int i) {
        assertThat(this.lookedUpModels).hasSize(i);
	}
        
    private void assertLookedUpBeanSourcesAreNull() {
        assertThat(this.lookedUpModels).isNull();
    }

    private void assertLookedUpBeanSourcesAreNotNull() {
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
    	assertThat(this.testee.getBeanSources()).hasSize(i);
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
        this.testee.shutdown();
    }

    private void withBeanSources(int i) {
        for (int k = 0; k < i; ++k) {
            String resourceType = "/mock/resourcetype/" + k;
            withResourceModel(resourceType);
        }
        withBeanSourcesForAllResourceModels();
    }
    
    private void withModelForType(String resourceType, Class modelType) {
        withModelForType(resourceType, modelType, "defaultBeanName");
    }

    private void withInvalidBeanSource(String resourceType, @SuppressWarnings("rawtypes") Class modelType) {
        withModelForType(resourceType, modelType, "defaultBeanName", false);
    }

	private void withModelForType(String resourceType, @SuppressWarnings("rawtypes") Class modelType, String modelBeanName) {
        withModelForType(resourceType, modelType, modelBeanName, true);
    }

    @SuppressWarnings("unchecked")
    private void withModelForType(String resourceType, @SuppressWarnings("rawtypes") Class modelType, String modelBeanName, boolean isValid) {
        OsgiBeanSource<?> source = mock(OsgiBeanSource.class);
        when(source.getBeanType()).thenReturn(modelType);
        when(source.getBundleId()).thenReturn(this.bundleId);
        when(source.getBeanName()).thenReturn(modelBeanName);
        when(source.isValid()).thenReturn(isValid);
        this.testee.add(new String[] {resourceType}, source);
    }

    private void withBeanSourcesForAllResourceModels() {
        for (ResourceModel model : this.resourceModelAnnotations) {
            OsgiBeanSource<?> source = mock(OsgiBeanSource.class);
            when(source.getBundleId()).thenReturn(this.bundleId);
            this.testee.add(model.types(), source);
        }
    }
    
    private void removeBundle() {
        this.testee.removeResourceModels(this.bundle);
    }
}
