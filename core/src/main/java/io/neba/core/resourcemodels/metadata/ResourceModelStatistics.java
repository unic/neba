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

package io.neba.core.resourcemodels.metadata;

import static java.lang.Math.max;
import static java.lang.System.currentTimeMillis;
import static java.util.Arrays.copyOf;
import static java.util.Arrays.fill;

/**
 * Represents statistical data regarding the usage of a {@link io.neba.api.annotations.ResourceModel}.
 * This implementation is intentionally not thread-safe, i.e. the calculations will loose accuracy in case the
 * statics are modified concurrently. This is intended: We can give up some precision in favor of a massive performance gain,
 * as this model is accessed hundreds of times per request, e.g. during page rendering.<br />
 * However, it is expected that the statistics still provide an accurate picture and gain
 * precision as time passes and data accumulates.
 *
 * @author Olaf Otto
 */
public class ResourceModelStatistics {
    private final long since = currentTimeMillis();
    // This will consume approx 4 Byte * 16 + c
    // (= 68 Byte + constant management overhead for the array) of main memory.
    // The mapping duration intervals are of the form [0, 1), [1, 2), [2, 4), [4, 8),
    // i.e. use a base-2 exponential to build frequency groups up to [2^(i - 1), 2^i) where i
    // is the size of the frequency table.
    private final int[] mappingDurationFrequencies = new int[16];
    private final int[] indexBoundaries = new int[mappingDurationFrequencies.length];

    private long instantiations;
    private long mappings;
    private long cacheHits;

    public ResourceModelStatistics() {
        reset();
        int boundary = 1;
        for (int i = 0; i < indexBoundaries.length; ++i) {
            indexBoundaries[i] = boundary;
            boundary *= 2;
        }
    }

    /**
     * Clears all collected statistical data.
     */
    public void reset() {
        fill(this.mappingDurationFrequencies, 0);
        this.instantiations = 0;
        this.mappings = 0;
        this.cacheHits = 0;
    }

    /**
     * @return The age of these statistics in terms of {@link System#currentTimeMillis()}.
     */
    public long getSince() {
        return since;
    }

    /**
     * @return The number of times this resource model instantiated.
     */
    public long getInstantiations() {
        return instantiations;
    }

    /**
     * Increments the number of times the resource model was instantiated.
     *
     * @return this instance.
     */
    public ResourceModelStatistics countInstantiation() {
        ++this.instantiations;
        return this;
    }

    /**
     * Increments the number of subsequent mappings.
     *
     * @return this instance.
     */
    public ResourceModelStatistics countSubsequentMapping() {
        ++this.mappings;
        return this;
    }

    /**
     * @return The number of types a {@link io.neba.api.resourcemodels.ResourceModelCache} contained an instance
     * of the resource model.
     */
    public long getCacheHits() {
        return cacheHits;
    }

    /**
     * Increment the number of cache hits for this model.
     *
     * @return this instance.
     */
    public ResourceModelStatistics countCacheHit() {
        ++this.cacheHits;
        return this;
    }

    /**
     * @return the total number of recorded subsequent resource-to-resourcemodel mappings
     *         that occurred during the mapping of this model.
     */
    public long getNumberOfMappings() {
        return this.mappings;
    }

    /**
     * Adds the mapping with the duration to the statistics.
     *
     * @return this instance.
     */
    public ResourceModelStatistics countMappingDuration(int durationInMs) {
        // The right-hand interval boundaries are pre-calculated. We start at the smallest (1) of the
        // interval [0, 1). Thus, when our value is less than the boundary, we know it is in the current interval (i),
        // as the right-hand boundary is exclusive.
        for (int i = 0; i < this.indexBoundaries.length; ++i) {
            if (durationInMs < this.indexBoundaries[i]) {
                ++this.mappingDurationFrequencies[i];
                return this;
            }
        }

        // The mapping duration time exceeds the frequency table boundaries.
        // Fallback: count it as the longest possible duration
        ++this.mappingDurationFrequencies[this.mappingDurationFrequencies.length - 1];
        return this;
    }

    /**
     * @return the average mapping duration of all {@link #countMappingDuration(int) counted mappings} in ms.
     */
    public double getAverageMappingDuration() {
        return getTotalMappingDuration() / (double) max(getNumberOfMappingDurationSamples(), 1);
    }

    /**
     * @return the sum of all recorded mapping durations in ms,
     *         i.e. summed up averages of the mapping duration frequency table interval means.
     */
    public double getTotalMappingDuration() {
        double totalDuration = 0L;
        double leftBoundary = 0;
        for (int i = 0; i < this.mappingDurationFrequencies.length; ++i) {
            double rightBoundary = this.indexBoundaries[i];
            double intervalMean = (leftBoundary + rightBoundary) / 2;
            totalDuration += intervalMean * this.mappingDurationFrequencies[i];
            leftBoundary = this.indexBoundaries[i];
        }
        return totalDuration;
    }

    /**
     * @return the median of the mapping durations based on a frequency table.
     */
    public double getMappingDurationMedian() {
        double median;

        long numberOfSamples = getNumberOfMappingDurationSamples();

        if (numberOfSamples == 0) {
            return 0;
        }

        if (numberOfSamples % 2 == 0) {
            // Even number of occurrences: The median is the average of the two center most elements (around the 50% mark)
            long sample = (numberOfSamples / 2L);
            // Obtain mapping depth x1, x2 at the center
            double[] depths = mappingDurationOfSampleAndSuccessor(sample);
            median = (depths[0] + depths[1]) / 2D;
        } else {
            // Odd number of occurrences: The median is defined by the sample representing the 50% mark.
            long sample = (numberOfSamples + 1) / 2L;
            return mappingDurationOf(sample);
        }
        return median;
    }

    /**
     * @return The maximum {@link #countMappingDuration(int) recorded mapping duration} of this resource model in ms.
     */
    public double getMaximumMappingDuration() {
        for (int i = this.mappingDurationFrequencies.length - 1; i >= 0; --i) {
            if (this.mappingDurationFrequencies[i] != 0) {
                double leftHandBoundary = i == 0 ? 0 : this.indexBoundaries[i - 1];
                double rightHandBoundary = this.indexBoundaries[i];
                return (leftHandBoundary + rightHandBoundary) / 2D;
            }
        }
        return 0;
    }

    /**
     * @return The minimum {@link #countMappingDuration(int) recorded mapping duration} of this resource model in ms.
     */
    public double getMinimumMappingDuration() {
        for (int i = 0; i < this.mappingDurationFrequencies.length; ++i) {
            if (this.mappingDurationFrequencies[i] != 0) {
                double leftHandBoundary = i == 0 ? 0 : this.indexBoundaries[i - 1];
                double rightHandBoundary = this.indexBoundaries[i];
                return (leftHandBoundary + rightHandBoundary) / 2D;
            }
        }
        return 0;
    }

    /**
     * @return the summed up mapping duration counts, i.e. all frequencies (number of times) any mapping time was recorded.
     */
    private long getNumberOfMappingDurationSamples() {
        long sum = 0L;
        for (int samples : this.mappingDurationFrequencies) {
            sum += samples;
        }
        return sum;
    }

    /**
     * @return the mapping duration corresponding to the nth measured duration.
     *         Example: if the duration was measured seven times, get the position of the third sample from the
     *         ordered frequency table; it represents the duration which has 50% of all mapping durations below
     *         and 50% of all mapping durations above: <code>[0 1 2 (3) 4 5 5]</code>.
     */
    private double mappingDurationOf(long nthSample) {
        int index = 0;
        for (long sum = 0; sum < nthSample; ++index) {
            sum += this.mappingDurationFrequencies[index];
        }
        // subtract one: the for-loop always post-increments index.
        double leftBoundary = index < 2 ? 0 : this.indexBoundaries[index - 2];
        double rightBoundary = this.indexBoundaries[index - 1];
        return (leftBoundary + rightBoundary) / 2D;
    }

    /**
     * @see #mappingDurationOf(long). Also returns the duration of the sample succeeding the given sample.
     */
    private double[] mappingDurationOfSampleAndSuccessor(long nthSample) {
        int x1 = -1, x2 = -1;

        int index = 0;
        long samples = 0;

        do {
            samples += this.mappingDurationFrequencies[index];
            if (x1 == -1 && samples >= nthSample) {
                x1 = index;
            }
            if (samples >= nthSample + 1) {
                x2 = index;
                break;
            }
            ++index;
        } while (index < this.mappingDurationFrequencies.length);

        // [n1, n2) -> mean is (n1 + n2) / 2
        double leftHandBoundaryX1 = x1 == 0? 0 : this.indexBoundaries[x1 - 1];
        double rightHandBoundaryX1 = this.indexBoundaries[x1];
        double leftHandBoundaryX2 = x2 == 0? 0 : this.indexBoundaries[x2 - 1];
        double rightHandBoundaryX2 = this.indexBoundaries[x2];

        // The boundaries are doubles as the intervals [0, 1) and [1, 2) have fraction means (0.5, 1.5)
        double durationX1 = (leftHandBoundaryX1 + rightHandBoundaryX1) / 2;
        double durationX2 = (leftHandBoundaryX2 + rightHandBoundaryX2) / 2;

        return new double[]{durationX1, durationX2};
    }

    public int[] getMappingDurationFrequencies() {
        return copyOf(this.mappingDurationFrequencies, this.mappingDurationFrequencies.length);
    }

    public int[] getMappingDurationIntervalBoundaries() {
        return copyOf(this.indexBoundaries, this.indexBoundaries.length);
    }
}
