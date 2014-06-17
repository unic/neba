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

import io.neba.core.resourcemodels.metadata.ResourceModelMetaData;
import io.neba.core.resourcemodels.metadata.ResourceModelStatistics;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.verification.VerificationMode;

import java.util.Set;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Olaf Otto
 */
@RunWith(MockitoJUnitRunner.class)
public class CyclicMappingSupportTest {
    @Mock
    private ResourceModelStatistics statistics;
    @Mock
    private ResourceModelMetaData metaData;

    private Mapping mapping;
    private Mapping<?> alreadyOngoingMapping;
    private Set<Mapping> ongoingMappings;

	@InjectMocks
    private CyclicMappingSupport testee;

    @Before
    public void setUp() throws Exception {
        withNewMapping();
    }

    @Test
    public void testCycleDetection() throws Exception {
        beginMapping();
        assertMappingWasNotAlreadyStarted();
        beginMapping();
        assertAlreadyStartedMappingIsDetected();
    }
    
    @Test
    public void testRemovalOfMapping() throws Exception {
        beginMapping();
        assertMappingWasNotAlreadyStarted();
        endMapping();

        beginMapping();
        assertMappingWasNotAlreadyStarted();
    }

    @Test
    public void testGetOngoingMappingsAreNotNullWithoutMappings() throws Exception {
        getOngoingMappings();
        assertOngoingMappingsAreEmpty();
    }

    @Test
    public void testGetOngoingMappingsAreEmptyIfMappingEnds() throws Exception {
        beginMapping();
        endMapping();
        getOngoingMappings();
        assertOngoingMappingsAreEmpty();
    }

    @Test
    public void testGetOngoingMappingsContainOngoingMapping() throws Exception {
        beginMapping();
        getOngoingMappings();
        assertOngoingMappingsContainMapping();
    }

    @Test
    public void testTrackingOfMappingDepths() throws Exception {
        beginMapping();
        verifyMappingIsNotTracked();

        withNewMapping();
        beginMapping();
        verifyMappingIsTrackedWithDepth(1);

        withNewMapping();
        beginMapping();
        verifyMappingIsTrackedWithDepth(times(2), 1);
        verifyMappingIsTrackedWithDepth(2);
    }

    private void verifyMappingIsNotTracked() {
        verify(this.statistics, never()).countMappingDuration(anyInt());
    }

    private void verifyMappingIsTrackedWithDepth(VerificationMode times, int depth) {
        verify(this.statistics, times).countMappings(depth);
    }

    private void verifyMappingIsTrackedWithDepth(int depth) {
        verifyMappingIsTrackedWithDepth(times(1), depth);
    }

    private void assertOngoingMappingsContainMapping() {
        assertThat(this.ongoingMappings).contains(this.mapping);
    }

    private void assertOngoingMappingsAreEmpty() {
        assertThat(this.ongoingMappings).isEmpty();
    }

    private void getOngoingMappings() {
        this.ongoingMappings = this.testee.getOngoingMappings();
    }

    private void endMapping() {
        this.testee.end(this.mapping);
    }

    private void assertAlreadyStartedMappingIsDetected() {
        assertThat(this.alreadyOngoingMapping).isNotNull();
    }

    @SuppressWarnings("unchecked")
    private void beginMapping() {
        this.alreadyOngoingMapping = this.testee.begin(this.mapping);
    }

    private void assertMappingWasNotAlreadyStarted() {
        assertThat(this.alreadyOngoingMapping).isNull();
    }

    private void withNewMapping() {
        this.mapping = mock(Mapping.class);
        doReturn(this.metaData).when(this.mapping).getMetadata();
        doReturn(this.statistics).when(this.metaData).getStatistics();
    }
}
