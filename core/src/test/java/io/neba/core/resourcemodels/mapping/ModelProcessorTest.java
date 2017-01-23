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

import static org.assertj.core.api.Assertions.assertThat;

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
        public void publicPostMapping() {
            postMappingWasCalled();
        }

        @PostMapping
        protected void protectedPostMapping() {
            postMappingWasCalled();
        }

        @PostMapping
        private void privatePostMapping() {
            postMappingWasCalled();
        }

        @PostMapping
        void packagePrivatePostMapping() {
            postMappingWasCalled();
        }

        @PreMapping
        public void publicPreMapping() {
            preMappingWasCalled();
        }

        @PreMapping
        protected void protectedPreMapping() {
            preMappingWasCalled();
        }

        @PreMapping
        private void privatePreMapping() {
            preMappingWasCalled();
        }

        @PreMapping
        void packagePrivatePreMapping() {
            preMappingWasCalled();
        }
    }

    @Mock
    private BeanFactory factory;
    @Mock
    private Resource resource;

    private int timesPostMappingCalled;
    private int timesPreMappingCalled;
    private TestModel model;
    private ResourceModelMetaData metadata;

    @InjectMocks
    private ModelProcessor testee;

    @Before
    public void setUp() throws Exception {
        this.throwExceptionDuringPostMapping = false;
        this.throwExceptionDuringPreMapping = false;
        this.timesPostMappingCalled = 0;
        this.timesPreMappingCalled = 0;
    }

    @Test(expected = IllegalArgumentException.class)
    public void testProcessBeforeMappingRequiresNonNullMetaData() throws Exception {
        this.testee.processBeforeMapping(null, this.model);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testProcessBeforeMappingRequiresNonNullModel() throws Exception {
        this.testee.processBeforeMapping(this.metadata, null);
    }


    @Test(expected = IllegalArgumentException.class)
    public void testProcessAfterMappingRequiresNonNullMetaData() throws Exception {
        this.testee.processAfterMapping(null, this.model);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testProcessAfterMappingRequiresNonNullModel() throws Exception {
        this.testee.processAfterMapping(this.metadata, null);
    }

    @Test
    public void testPreMapping() throws Exception {
        withModel(new TestModel());
        processBeforeMapping();
        assertPostMappingMethodsAreNotInvoked();
        assertPreMappingMethodsAreInvoked();
    }

    @Test
    public void testPostMapping() throws Exception {
        withModel(new TestModel());
        processAfterMapping();
        assertPreMappingMethodsAreNotInvoked();
        assertPostMappingMethodsAreInvoked();
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

    private void postMappingWasCalled() {
        if (throwExceptionDuringPostMapping) {
            throw new RuntimeException("THIS IS AN EXPECTED TEST EXCEPTION");
        }
        this.timesPostMappingCalled++;
    }

    private void preMappingWasCalled() {
        if (throwExceptionDuringPreMapping) {
            throw new RuntimeException("THIS IS AN EXPECTED TEST EXCEPTION");
        }
        this.timesPreMappingCalled++;
    }

    private void assertPreMappingMethodsAreNotInvoked() {
        assertThat(this.timesPreMappingCalled).isZero();
    }

    private void assertPreMappingMethodsAreInvoked() {
        assertThat(this.timesPreMappingCalled).isEqualTo(this.metadata.getPreMappingMethods().length);
    }

    private void assertPostMappingMethodsAreNotInvoked() {
        assertThat(this.timesPostMappingCalled).isZero();
    }

    private void assertPostMappingMethodsAreInvoked() {
        assertThat(this.timesPostMappingCalled).isEqualTo(this.metadata.getPostMappingMethods().length);
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
