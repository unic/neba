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

package io.neba.core.rendering;

import io.neba.core.sling.AdministrativeResourceResolver;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceMetadata;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.InputStream;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Olaf Otto
 */
@RunWith(MockitoJUnitRunner.class)
public class JcrResourceLoaderTest {
	@Mock
    private AdministrativeResourceResolver administrativeResourceResolver;

    private String templatePath;
    private Resource resource;
    private ResourceMetadata resourceMetadata;
    private org.apache.velocity.runtime.resource.Resource velocityResource;
    private boolean resourceChanged;

    @InjectMocks
    private JcrResourceLoader testee;
    private long lastModified;

    @Before
    public void initResourceLoader() {
        this.testee.init(null);
    }

    @Test
    public void testResourceLoaderAdaptsResourceToInputStream() throws Exception {
        withTemplatePath("/junit/resource");
        withResourceForTemplatePath();
        getResourceStream();
        verifyResourceIsAdaptedToInputStream();
    }
        
    @Test
    public void testModificationDetectionByModificationDate() throws Exception {
        withTemplatePath("/junit/resource");
        withResourceForTemplatePath();
        withVelocityResourceForTemplatePath();

        withLastResourceModificationTime(123L);
        withLastVelocityResourceModificationTime(1234L);
        assertResourceWasNotModified();
        
        withLastVelocityResourceModificationTime(123L);
        assertResourceWasNotModified();

        withLastVelocityResourceModificationTime(12L);
        assertResourceWasModified();
    }
    
    @Test
	public void testModificationDetectionByResourceRemoval() throws Exception {
    	withTemplatePath("/junit/resource");
    	withVelocityResourceForTemplatePath();
    	// No JCR resource is mocked for the velocity template.
    	assertResourceWasModified();
	}

    @Test
    public void testGetLastModifiedFromUnderlyingJcrResource() throws Exception {
        withTemplatePath("/junit/resource");
        withResourceForTemplatePath();
        withVelocityResourceForTemplatePath();
        withLastResourceModificationTime(123L);

        getLastModificationTimeOfVelocityResource();

        assertLastModificationTimeIs(123L);
    }

    private void getLastModificationTimeOfVelocityResource() {
        this.lastModified = this.testee.getLastModified(this.velocityResource);
    }

    private void assertLastModificationTimeIs(long time) {
        assertThat(this.lastModified).isEqualTo(time);
    }

    private void assertResourceWasModified() {
        checkIfVelocityResourceIsModified();
        assertThat(this.resourceChanged).isTrue();
    }

    private void assertResourceWasNotModified() {
        checkIfVelocityResourceIsModified();
        assertThat(this.resourceChanged).isFalse();
    }

    private void checkIfVelocityResourceIsModified() {
        this.resourceChanged = this.testee.isSourceModified(this.velocityResource);
    }

    private void withLastVelocityResourceModificationTime(long modificationTime) {
        when(this.velocityResource.getLastModified()).thenReturn(modificationTime);
    }

    private void withVelocityResourceForTemplatePath() {
        this.velocityResource = mock(org.apache.velocity.runtime.resource.Resource.class);
        when(this.velocityResource.getName()).thenReturn(this.templatePath);
    }

    private void withResourceForTemplatePath() {
        this.resource = mock(Resource.class);
        when(this.administrativeResourceResolver.get(eq(this.templatePath))).thenReturn(this.resource);
        this.resourceMetadata = mock(ResourceMetadata.class);
        when(this.resource.getResourceMetadata()).thenReturn(this.resourceMetadata);
    }

    private void withLastResourceModificationTime(long modificationTime) {
        when(this.resourceMetadata.getModificationTime()).thenReturn(modificationTime);
    }
    
    private void verifyResourceIsAdaptedToInputStream() {
        verify(this.resource, times(1)).adaptTo(Mockito.eq(InputStream.class));
    }
   
    private void withTemplatePath(String templatePath) {
        this.templatePath = templatePath;
    }

    private void getResourceStream() {
        this.testee.getResourceStream(this.templatePath);
    }
}
