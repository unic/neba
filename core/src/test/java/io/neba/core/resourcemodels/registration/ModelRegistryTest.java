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

package io.neba.core.resourcemodels.registration;

import io.neba.api.annotations.ResourceModel;
import io.neba.core.blueprint.ReferenceConsistencyChecker;
import io.neba.core.sling.AdministrativeResourceResolver;
import io.neba.core.util.OsgiBeanSource;

import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
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

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
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
    private ReferenceConsistencyChecker consistencyChecker;
    @Mock
    private ResourceResolver resolver;
    @Mock
    private AdministrativeResourceResolver administrativeResourceResolver;

    private Set<ResourceModel> resourceModelAnnotations;
    private long bundleId;
    private Collection<LookupResult> lookedUpModels;
    
    @InjectMocks
    private ModelRegistry testee;

    @Before
    public void setUp() throws LoginException {
        doReturn(true).when(this.consistencyChecker).isValid(isA(OsgiBeanSource.class));
        doReturn(new String[]{}).when(this.resolver).getSearchPath();
        doReturn(this.resolver).when(this.administrativeResourceResolver).getResolver();
        this.resourceModelAnnotations = new HashSet<ResourceModel>();
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
        mockBeanSourcesForAllResourceModels();
        lookupMostSpecificBeanSources(mockResourceWithType("some/resourcetype", "some/resourcetype/supertype"));
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
        mockBeanSourcesForAllResourceModels();
        lookupMostSpecificBeanSources(mockResourceWithType("some/resourcetype", "some/resourcetype/supertype"));
        assertRegistryHasModels(2);
        assertNumberOfLookedUpBeanSourcesIs(1);

        lookupMostSpecificBeanSources(mockResourceWithType("some/resourcetype", "some/resourcetype/supertype"));
        assertRegistryHasModels(2);
        assertNumberOfLookedUpBeanSourcesIs(1);
    }

    @Test
    public void testMultipleMappingsToSameResourceType() throws Exception {
        withResourceModel("some/resourcetype");
        withResourceModel("some/resourcetype");
        mockBeanSourcesForAllResourceModels();
        lookupMostSpecificBeanSources(mockResourceWithType("some/resourcetype"));
        assertNumberOfLookedUpBeanSourcesIs(2);
    }
    
    @Test
    public void testRemovalOfBundleWithModelforSameResourceType() throws Exception {
        withResourceModel("some/resourcetype");
        withBundleId(1);
        mockBeanSource("some/resourcetype", TargetType1.class);
        withBundleId(2);
        mockBeanSource("some/resourcetype", TargetType2.class);

        lookupMostSpecificBeanSources(mockResourceWithType("some/resourcetype"));
        assertNumberOfLookedUpBeanSourcesIs(2);

        removeBundle();
        lookupMostSpecificBeanSources(mockResourceWithType("some/resourcetype"));
        assertNumberOfLookedUpBeanSourcesIs(1);
        
        withBundleId(1);
        removeBundle();
        lookupMostSpecificBeanSources(mockResourceWithType("some/resourcetype"));
        assertLookedUpBeanSourcesAreNull();
    }
    
    @Test
    public void testNoMappingsToResourceType() throws Exception {
        mockBeanSourcesForAllResourceModels();
        lookupMostSpecificBeanSources(mockResourceWithType("some/resourcetype"));
        assertLookedUpBeanSourcesAreNull();
    }
    
    @Test
    public void testLookupOfBeanSourceForSpecificTypeWithSingleMapping() throws Exception {
        mockBeanSource("some/resourcetype", TargetType1.class);
        Resource resource = mockResourceWithType("some/resourcetype");
        lookupBeanSourcesForType(TargetType1.class, resource);
        assertLookedUpBeanSourcesAreNotNull();
        assertLookedUpModelTypesAre(TargetType1.class);
    }

    @Test
    public void testLookupOfBeanSourceForSpecificTypeWithMultipleCompatibleModels() throws Exception {
        mockBeanSource("some/resourcetype/parent", TargetType1.class);
        mockBeanSource("some/resourcetype", ExtendedTargetType1.class);
        Resource resource = mockResourceWithType("some/resourcetype", "some/resourcetype/parent");
        lookupBeanSourcesForType(TargetType1.class, resource);
        assertLookedUpBeanSourcesAreNotNull();
        assertNumberOfLookedUpBeanSourcesIs(1);
    }

    @Test
    public void testlookupOfBeanSourceForSpecificTypeWithoutModel() throws Exception {
        Resource resource = mockResourceWithType("some/resourcetype");
        lookupBeanSourcesForType(TargetType1.class, resource);
        assertLookedUpBeanSourcesAreNull();
    }

    @Test
    public void testLookupOfBeanSourceForTypeWithMultipleIncompatibleModels() throws Exception {
        Resource resource = mockResourceWithType("some/resourcetype");
        mockBeanSource("some/resourcetype", TargetType1.class);
        mockBeanSource("some/resourcetype", TargetType2.class);
        lookupBeanSourcesForType(TargetType1.class, resource);
        assertLookedUpBeanSourcesAreNotNull();
        assertLookedUpModelTypesAre(TargetType1.class);
    }
    
    @Test
    public void testRepeatedLookupOfModelWithTargetType() throws Exception {
        Resource resource = mockResourceWithType("some/resourcetype");
        mockBeanSource("some/resourcetype", TargetType1.class);
        mockBeanSource("some/resourcetype", TargetType2.class);
        mockBeanSource("some/resourcetype", TargetType3.class);
        mockBeanSource("some/resourcetype", TargetType4.class);
        
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

    @Test
    public void testLookupOfBeanSourceForTypeWithMultipleCompatibleModels() throws Exception {
        Resource resource = mockResourceWithType("some/resourcetype");
        mockBeanSource("some/resourcetype", TargetType1.class);
        mockBeanSource("some/resourcetype", ExtendedTargetType1.class);

        lookupBeanSourcesForType(TargetType1.class, resource);
        assertLookedUpBeanSourcesAreNotNull();
		assertNumberOfLookedUpBeanSourcesIs(2);
    }

    @Test
    public void testLookupOfModelWithSpecificBeanName() throws Exception {
        Resource resource = mockResourceWithType("some/resourcetype");
        mockBeanSource("some/resourcetype", TargetType1.class, "junitBeanOne");
        mockBeanSource("some/resourcetype", ExtendedTargetType1.class, "junitBeanTwo");

        lookupBeanSourcesWithBeanName("junitBeanTwo", resource);
        assertLookedUpModelTypesAre(ExtendedTargetType1.class);
    }

    @Test
    public void testLookupOfModelWithSpecificBeanNameProvidesMostSpecificModel() throws Exception {
        Resource resource = mockResourceWithType("some/resourcetype", "some/resource/supertype");
        mockBeanSource("some/resourcetype", TargetType1.class, "junitBean");
        mockBeanSource("some/resource/supertype", ExtendedTargetType1.class, "junitBean");

        lookupBeanSourcesWithBeanName("junitBean", resource);

        assertLookedUpModelTypesAre(TargetType1.class);
    }

    @Test
    public void testCachedLookupOfModelWithSpecificBeanName() throws Exception {
        Resource resource = mockResourceWithType("some/resourcetype");
        mockBeanSource("some/resourcetype", TargetType1.class, "junitBeanOne");
        mockBeanSource("some/resourcetype", ExtendedTargetType1.class, "junitBeanTwo");

        lookupBeanSourcesWithBeanName("junitBeanTwo", resource);
        // The second request also tests the result from the cache
        lookupBeanSourcesWithBeanName("junitBeanTwo", resource);

        assertLookedUpModelTypesAre(ExtendedTargetType1.class);
    }

    @Test
    public void testLookupOfModelWithSpecificNonexistentBeanName() throws Exception {
        Resource resource = mockResourceWithType("some/resourcetype");
        mockBeanSource("some/resourcetype", TargetType1.class, "junitBeanOne");
        mockBeanSource("some/resourcetype", ExtendedTargetType1.class, "junitBeanTwo");

        lookupBeanSourcesWithBeanName("junitBeanThree", resource);

        assertLookedUpBeanSourcesAreNull();
    }

    @Test
    public void testCachedLookupOfModelWithSpecificNonexistentBeanName() throws Exception {
        Resource resource = mockResourceWithType("some/resourcetype");
        mockBeanSource("some/resourcetype", TargetType1.class, "junitBeanOne");
        mockBeanSource("some/resourcetype", ExtendedTargetType1.class, "junitBeanTwo");

        lookupBeanSourcesWithBeanName("junitBeanThree", resource);
        // The second request also tests the result from the cache
        lookupBeanSourcesWithBeanName("junitBeanThree", resource);

        assertLookedUpBeanSourcesAreNull();
    }

    @Test
    public void testResourceMappingForSameSlingResourceTypeAndDeviatingPrimaryType() throws Exception {
        mockBeanSource("some:JcrType", TargetType1.class);

        Resource resource = mockResourceWithType("some/resourcetype");

        withPrimaryType(resource, "some:JcrType");
        lookupBeanSourcesForType(TargetType1.class, resource);
        assertNumberOfLookedUpBeanSourcesIs(1);

        withPrimaryType(resource, "nt:unstructured");
        lookupBeanSourcesForType(TargetType1.class, resource);
        assertLookedUpBeanSourcesAreNull();
    }

    @Test
    public void testLookupOfAllModelsForResource() throws Exception {
        mockBeanSource("some/resourcetype/parent", TargetType1.class);
        mockBeanSource("some/resourcetype", TargetType2.class);
        Resource resource = mockResourceWithType("some/resourcetype", "some/resourcetype/parent");
        lookupAllBeanSourcesFor(resource);
        assertLookedUpBeanSourcesAreNotNull();
        assertLookedUpModelTypesAre(TargetType1.class, TargetType2.class);
    }

    @Test
    public void testRemovalOfInvalidReferencesToModels() throws Exception {
        mockBeanSource("some/resource/type", TargetType1.class);
        assertRegistryHasModels(1);

        withAllBeanSourcesRegardedAsInvalid();
        removeInvalidReferences();

        verifyConsistencyCheckerWasInvoked();
        assertRegistryHasModels(0);
    }

    @Test
    public void testValidBeanSourcesAreNotRemovedUponConsistencyCheck() throws Exception {
        mockBeanSource("some/resource/type", TargetType1.class);
        assertRegistryHasModels(1);

        removeInvalidReferences();

        verifyConsistencyCheckerWasInvoked();
        assertRegistryHasModels(1);
    }

    @Test
    public void testResourceMappingToSameModelWithDeviatingPrimaryType() throws Exception {
        mockBeanSource("my/page/type", TargetType1.class);

        Resource resource = mockResourceWithType("my/page/type");
        withPrimaryType(resource, "some:JcrType");

        lookupBeanSourcesForType(TargetType1.class, resource);
        assertNumberOfLookedUpBeanSourcesIs(1);

        withPrimaryType(resource, "nt:unstructured");
        lookupBeanSourcesForType(TargetType1.class, resource);
        assertNumberOfLookedUpBeanSourcesIs(1);
    }

    private void verifyConsistencyCheckerWasInvoked() {
        verify(this.consistencyChecker).isValid(isA(OsgiBeanSource.class));
    }

    private void withAllBeanSourcesRegardedAsInvalid() {
        doReturn(false).when(this.consistencyChecker).isValid(isA(OsgiBeanSource.class));
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
        when(resource.adaptTo(eq(Node.class))).thenReturn(node);
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

    private void assertLookedUpModelTypesAre(Class<?>... types) {
        assertThat(this.lookedUpModels).onProperty("source.beanType").containsOnly((Object[]) types);
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
            Resource resource = mockResourceWithType(resourceTypeName);
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
    
    private Resource mockResourceWithType(String resourceTypeName) {
        return mockResourceWithType(resourceTypeName, null);
    }

    private Resource mockResourceWithType(String resourceTypeName, String resourceSuperType) {
        Resource resource = mock(Resource.class);
        ValueMap values = mock(ValueMap.class);
        when(values.get(eq("sling:resourceType"), eq(String.class))).thenReturn(resourceTypeName);
        when(resource.adaptTo(eq(ValueMap.class))).thenReturn(values);
        when(resource.getResourceType()).thenReturn(resourceTypeName);
        if (resourceSuperType != null) {
            when(resource.getResourceSuperType()).thenReturn(resourceSuperType);
        }
        return resource;
    }

    private Resource mockResourceWithSupertype(String resourceSuperTypeTypeName) {
        final String resourceTypeName = "childOf/" + resourceSuperTypeTypeName;

        Resource resource = mock(Resource.class);
        Resource superResource = mock(Resource.class);

        ValueMap values = mock(ValueMap.class);
        when(values.get(eq("sling:resourceType"), eq(String.class))).thenReturn(resourceTypeName);
        when(resource.adaptTo(eq(ValueMap.class))).thenReturn(values);
        when(resource.getResourceType()).thenReturn(resourceTypeName);
        when(resource.getResourceSuperType()).thenReturn(resourceSuperTypeTypeName);
        when(superResource.getResourceType()).thenReturn(resourceSuperTypeTypeName);
        when(this.resolver.getResource(eq(resourceSuperTypeTypeName))).thenReturn(superResource);
        return resource;
    }
    
    private void shutdownRegistry() {
        this.testee.shutdown();
    }

    private void withBeanSources(int i) {
        for (int k = 0; k < i; ++k) {
            String resourceType = "/mock/resourcetype/" + k;
            withResourceModel(resourceType);
        }
        mockBeanSourcesForAllResourceModels();
    }
    
    @SuppressWarnings({ "rawtypes" })
    private void mockBeanSource(String resourceType, Class modelType) {
        mockBeanSource(resourceType, modelType, "defaultBeanName");
    }

    @SuppressWarnings("unchecked")
	private void mockBeanSource(String resourceType, @SuppressWarnings("rawtypes") Class modelType, String modelBeanName) {
        OsgiBeanSource<?> source = mock(OsgiBeanSource.class);
        when(source.getBeanType()).thenReturn(modelType);
        when(source.getBundleId()).thenReturn(this.bundleId);
        when(source.getBeanName()).thenReturn(modelBeanName);
        this.testee.add(new String[] {resourceType}, source);
    }

    private void mockBeanSourcesForAllResourceModels() {
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
