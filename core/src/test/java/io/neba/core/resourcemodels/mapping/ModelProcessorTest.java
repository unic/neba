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

import io.neba.api.annotations.AfterMapping;
import io.neba.core.resourcemodels.metadata.ResourceModelMetaData;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Olaf Otto
 */
public class ModelProcessorTest {
    private boolean throwExceptionDuringAfterMapping;
    private int timesAfterMappingCalled;
    private TestModel model;
    private ResourceModelMetaData metadata;

    private ModelProcessor testee;

    @Before
    public void setUp() {
        this.testee = new ModelProcessor();
        this.throwExceptionDuringAfterMapping = false;
        this.timesAfterMappingCalled = 0;
    }

    @Test(expected = IllegalArgumentException.class)
    public void testProcessAfterMappingRequiresNonNullMetaData() {
        this.testee.processAfterMapping(null, this.model);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testProcessAfterMappingRequiresNonNullModel() {
        this.testee.processAfterMapping(this.metadata, null);
    }

    @Test
    public void testAfterMapping() {
        withModel(new TestModel());
        processAfterMapping();
        assertAfterMappingMethodsAreInvoked();
    }

    @Test
    public void testHandlingOfExceptionDuringAfterMappingPhase() {
        withModel(new TestModel());
        withExceptionDuringAfterMappingMethodInvocation();
        processAfterMapping();
    }

    private void withExceptionDuringAfterMappingMethodInvocation() {
        this.throwExceptionDuringAfterMapping = true;
    }


    private void afterMappingWasCalled() {
        if (throwExceptionDuringAfterMapping) {
            throw new RuntimeException("THIS IS AN EXPECTED TEST EXCEPTION");
        }
        this.timesAfterMappingCalled++;
    }

    private void assertAfterMappingMethodsAreInvoked() {
        assertThat(this.timesAfterMappingCalled).isEqualTo(this.metadata.getAfterMappingMethods().length);
    }

    private void withModel(final TestModel testModel) {
        this.model = testModel;
        this.metadata = new ResourceModelMetaData(this.model.getClass());
    }

    private void processAfterMapping() {
        this.testee.processAfterMapping(this.metadata, this.model);
    }

    /**
     * @author Olaf Otto
     */
    @SuppressWarnings("unused")
    private class TestModel {
        @AfterMapping
        public void publicAfterMapping() {
            afterMappingWasCalled();
        }

        @AfterMapping
        protected void protectedAfterMapping() {
            afterMappingWasCalled();
        }

        @AfterMapping
        private void privateAfterMapping() {
            afterMappingWasCalled();
        }

        @AfterMapping
        void packagePrivateAfterMapping() {
            afterMappingWasCalled();
        }
    }
}
