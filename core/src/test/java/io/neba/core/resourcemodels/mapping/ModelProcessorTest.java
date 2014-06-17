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

import io.neba.api.annotations.PostMapping;
import io.neba.api.annotations.PreMapping;
import io.neba.core.resourcemodels.metadata.ResourceModelMetaData;
import org.apache.sling.api.resource.Resource;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.beans.factory.BeanFactory;

import static org.fest.assertions.Assertions.assertThat;

/**
 * @author Olaf Otto
 */
@RunWith(MockitoJUnitRunner.class)
public class ModelProcessorTest {
    private boolean throwExceptionDuringPreMapping;
    private boolean throwExceptionDuringPostMapping;

    /**
     * @author Olaf Otto
     */
    private class TestModel {
        @PostMapping
        public void postMapping() {
            setPostMappingCalled(true);
        }

        @PreMapping
        public void preMapping() {
            setPreMappingCalled(true);
        }
    }

    @Mock
    private BeanFactory factory;
    @Mock
    private Resource resource;

    private boolean postMappingCalled;
    private boolean preMappingCalled;
    private TestModel model;
    private ResourceModelMetaData metadata;

    @InjectMocks
    private ModelProcessor testee;

    @Before
    public void setUp() throws Exception {
        this.throwExceptionDuringPostMapping = false;
        this.throwExceptionDuringPreMapping = false;
    }

    @Test
    public void testPreMapping() throws Exception {
        withModel(new TestModel());
        processBeforeMapping();
        assertPostMappingMethodIsNotInvoked();
        assertPreMappingMethodIsInvoked();
    }

    @Test
    public void testPostMapping() throws Exception {
        withModel(new TestModel());
        processAfterMapping();
        assertPreMappingMethodIsNotInvoked();
        assertPostMappingMethodIsInvoked();
    }

    @Test
    public void testHandlingOfExceptionDuringPreMappingPhase() throws Exception {
        withModel(new TestModel());
        withExceptionDuringPreMappingMethodInvocation();
        processBeforeMapping();
    }

    @Test
    public void testHandlingOfExceptionDuringPostMappingPhase() throws Exception {
        withModel(new TestModel());
        withExceptionDuringPostMappingMethodInvocation();
        processAfterMapping();
    }

    private void withExceptionDuringPostMappingMethodInvocation() {
        this.throwExceptionDuringPostMapping = true;
    }

    private void withExceptionDuringPreMappingMethodInvocation() {
        this.throwExceptionDuringPreMapping = true;
    }

    public boolean isPostMappingCalled() {
        return postMappingCalled;
    }

    public void setPostMappingCalled(boolean postMappingCalled) {
        if (throwExceptionDuringPostMapping) {
            throw new RuntimeException("THIS IS AN EXPECTED TEST EXCEPTION");
        }
        this.postMappingCalled = postMappingCalled;
    }

    public boolean isPreMappingCalled() {
        return preMappingCalled;
    }

    public void setPreMappingCalled(boolean preMappingCalled) {
        if (throwExceptionDuringPreMapping) {
            throw new RuntimeException("THIS IS AN EXPECTED TEST EXCEPTION");
        }
        this.preMappingCalled = preMappingCalled;
    }

    private void assertPreMappingMethodIsNotInvoked() {
        assertThat(this.preMappingCalled).isFalse();
    }

    private void assertPreMappingMethodIsInvoked() {
        assertThat(this.preMappingCalled).isTrue();
    }

    private void assertPostMappingMethodIsNotInvoked() {
        assertThat(this.postMappingCalled).isFalse();
    }

    private void assertPostMappingMethodIsInvoked() {
        assertThat(this.postMappingCalled).isTrue();
    }

    private void withModel(final TestModel testModel) {
        this.model = testModel;
        this.metadata = new ResourceModelMetaData(this.model.getClass());
    }

    private void processBeforeMapping() {
        this.testee.processBeforeMapping(this.metadata, this.model);
    }

    private void processAfterMapping() {
        this.testee.processAfterMapping(this.metadata, this.model);
    }
}
