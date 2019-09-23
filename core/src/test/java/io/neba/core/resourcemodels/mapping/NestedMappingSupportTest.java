/*
  Copyright 2013 the original author or authors.
  <p/>
  Licensed under the Apache License, Version 2.0 the "License";
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at
  <p/>
  http://www.apache.org/licenses/LICENSE-2.0
  <p/>
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
 */

package io.neba.core.resourcemodels.mapping;

import io.neba.core.resourcemodels.metadata.ResourceModelMetaData;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * @author Olaf Otto
 */
@RunWith(MockitoJUnitRunner.class)
public class NestedMappingSupportTest {
    private Mapping<?> mapping;
    private Mapping<?> alreadyOngoingMapping;
    @SuppressWarnings("rawtypes")
    private Iterable<Mapping> ongoingMappings;

    @InjectMocks
    private NestedMappingSupport testee;

    @Before
    public void setUp() {
        withNewMapping();
    }

    @Test
    public void testCycleDetection() {
        beginMapping();
        assertMappingWasNotAlreadyStarted();
        beginMapping();
        assertAlreadyStartedMappingIsDetected();
    }

    @Test
    public void testMappingCanBeginAgainAfterItHasEnded() {
        beginMapping();
        assertMappingWasNotAlreadyStarted();
        endMapping();

        beginMapping();
        assertMappingWasNotAlreadyStarted();
    }

    @Test
    public void testOngoingMappingsAreEmptyWithoutMappings() {
        getOngoingMappings();
        assertOngoingMappingsAreEmpty();
    }

    @Test
    public void testOngoingMappingsAreEmptyAfterMappingEnds() {
        beginMapping();
        endMapping();
        getOngoingMappings();
        assertOngoingMappingsAreEmpty();
    }

    @Test
    public void testOngoingMappingsContainOngoingMapping() {
        beginMapping();
        getOngoingMappings();
        assertOngoingMappingsContainMapping();
    }


    @Test
    public void testNoFalsePositiveDetectionOfResourceModelInOngoingMappings() {
        beginMapping();
        withNewMapping();
        assertNoMappingForCurrentResourceModelTypeExists();
    }

    @Test
    public void testDetectionOfResourceModelInOngoingMappings() {
        beginMapping();
        assertMappingForCurrentResourceModelTypeExists();

        withNewMappingForSameResourceModel();
        beginMapping();
        assertMappingForCurrentResourceModelTypeExists();

        endMapping();
        assertMappingForCurrentResourceModelTypeExists();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCheckingForResourceModelMappingDoesNotAcceptNull() {
        this.testee.hasOngoingMapping(null);
    }

    private void assertMappingForCurrentResourceModelTypeExists() {
        assertThat(this.testee.hasOngoingMapping(this.mapping.getMetadata())).isTrue();
    }

    private void assertNoMappingForCurrentResourceModelTypeExists() {
        assertThat(this.testee.hasOngoingMapping(this.mapping.getMetadata())).isFalse();
    }

    private void assertOngoingMappingsContainMapping() {
        assertThat(this.ongoingMappings).contains(this.mapping);
    }

    private void assertOngoingMappingsAreEmpty() {
        assertThat(this.ongoingMappings).isEmpty();
    }

    private void getOngoingMappings() {
        this.ongoingMappings = this.testee.getMappingStack();
    }

    private void endMapping() {
        this.testee.pop();
    }

    private void assertAlreadyStartedMappingIsDetected() {
        assertThat(this.alreadyOngoingMapping).isNotNull();
    }

    private void beginMapping() {
        this.alreadyOngoingMapping = this.testee.push(this.mapping);
    }

    private void assertMappingWasNotAlreadyStarted() {
        assertThat(this.alreadyOngoingMapping).isNull();
    }

    private void withNewMapping() {
        this.mapping = mock(Mapping.class);
        ResourceModelMetaData metaData = mock(ResourceModelMetaData.class);
        doReturn(metaData).when(this.mapping).getMetadata();
    }

    private void withNewMappingForSameResourceModel() {
        ResourceModelMetaData previousMetadata = this.mapping.getMetadata();
        this.mapping = mock(Mapping.class);
        doReturn(previousMetadata).when(this.mapping).getMetadata();
    }
}
