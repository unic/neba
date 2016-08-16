/**
 * Copyright 2013 the original author or authors.
 * <p/>
 * Licensed under the Apache License, Version 2.0 the "License";
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
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
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * @author Olaf Otto
 */
@RunWith(MockitoJUnitRunner.class)
public class NestedMappingSupportTest {
    private Mapping<?> mapping;
    private Mapping<?> alreadyOngoingMapping;
    @SuppressWarnings("rawtypes")
    private Set<Mapping> ongoingMappings;

    @InjectMocks
    private NestedMappingSupport testee;

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
    public void testMappingCanBeginAgainAfterItHasEnded() throws Exception {
        beginMapping();
        assertMappingWasNotAlreadyStarted();
        endMapping();

        beginMapping();
        assertMappingWasNotAlreadyStarted();
    }

    @Test
    public void testOngoingMappingsAreEmptyWithoutMappings() throws Exception {
        getOngoingMappings();
        assertOngoingMappingsAreEmpty();
    }

    @Test
    public void testOngoingMappingsAreEmptyAfterMappingEnds() throws Exception {
        beginMapping();
        endMapping();
        getOngoingMappings();
        assertOngoingMappingsAreEmpty();
    }

    @Test
    public void testOngoingMappingsContainOngoingMapping() throws Exception {
        beginMapping();
        getOngoingMappings();
        assertOngoingMappingsContainMapping();
    }

    @Test
    public void testTrackingOfSubsequentMappings() throws Exception {
        beginMapping();
        verifyNumberOfSubsequentMappingsIs(0);

        withNewMapping();
        beginMapping();
        verifyNumberOfSubsequentMappingsIs(1, 0);

        withNewMapping();
        beginMapping();
        verifyNumberOfSubsequentMappingsIs(2, 1, 0);
    }

    @Test
    public void testSubsequentMappingsOfSameResourceModelAreOnlyCountedOnce() throws Exception {
        beginMapping();
        verifyNumberOfSubsequentMappingsIs(0);

        withNewMappingForSameResourceModel();
        beginMapping();
        verifyNumberOfSubsequentMappingsIs(1, 1);

        withNewMappingForSameResourceModel();
        beginMapping();
        verifyNumberOfSubsequentMappingsIs(2, 2, 2);
    }

    @Test
    public void testNoFalsePositiveDetectionOfResourceModelInOngoingMappings() throws Exception {
        beginMapping();
        withNewMapping();
        assertNoMappingForCurrentResourceModelTypeExists();
    }

    @Test
    public void testDetectionOfResourceModelInOngoingMappings() throws Exception {
        beginMapping();
        assertMappingForCurrentResourceModelTypeExists();

        withNewMappingForSameResourceModel();
        beginMapping();
        assertMappingForCurrentResourceModelTypeExists();

        endMapping();
        assertMappingForCurrentResourceModelTypeExists();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCheckingForResourceModelMappingDoesNotAcceptNull() throws Exception {
        this.testee.hasOngoingMapping(null);
    }

    private void assertMappingForCurrentResourceModelTypeExists() {
        assertThat(this.testee.hasOngoingMapping(this.mapping.getMetadata())).isTrue();
    }

    private void assertNoMappingForCurrentResourceModelTypeExists() {
        assertThat(this.testee.hasOngoingMapping(this.mapping.getMetadata())).isFalse();
    }

    private void verifyNumberOfSubsequentMappingsIs(int... mappings) {
        getOngoingMappings();
        Mapping[] recordedMappings = this.ongoingMappings.toArray(new Mapping[this.ongoingMappings.size()]);
        assertThat(recordedMappings).hasSize(mappings.length);
        for (int i = 0; i < mappings.length; ++i) {
            verify(recordedMappings[i].getMetadata().getStatistics(), times(mappings[i])).countSubsequentMapping();
        }
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

    private void beginMapping() {
        this.alreadyOngoingMapping = this.testee.begin(this.mapping);
    }

    private void assertMappingWasNotAlreadyStarted() {
        assertThat(this.alreadyOngoingMapping).isNull();
    }

    private void withNewMapping() {
        this.mapping = mock(Mapping.class);
        ResourceModelMetaData metaData = mock(ResourceModelMetaData.class);
        ResourceModelStatistics statistics = mock(ResourceModelStatistics.class);
        doReturn(metaData).when(this.mapping).getMetadata();
        doReturn(statistics).when(metaData).getStatistics();
    }

    private void withNewMappingForSameResourceModel() {
        ResourceModelMetaData previousMetadata = this.mapping.getMetadata();
        this.mapping = mock(Mapping.class);
        doReturn(previousMetadata).when(this.mapping).getMetadata();
    }
}
