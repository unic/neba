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

package io.neba.core.resourcemodels.caching;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Collections.sort;

/**
 * @author Olaf Otto
 */
public class CacheKeyStatistics {
    /**
     * @author Olaf Otto
     */
    public static class KeyReport {
        private final Object key;
        private final int misses;
        private final int hits;
        private final int writes;

        public KeyReport(Object key, int misses, int hits, int writes) {
            this.key = key;
            this.misses = misses;
            this.hits = hits;
            this.writes = writes;
        }

        @Override
        public String toString() {
            return  key +
                    ": misses=" + misses +
                    ", hits=" + hits +
                    ", writes=" + writes;
        }
    }

    /**
     * @author Olaf Otto
     */
    public static class ReportSummary {
        private final int totalNumberOfHits;
        private final int totalNumberOfMisses;
        private final int totalNumberOfWrites;

        public ReportSummary(int totalNumberOfHits, int totalNumberOfMisses, int totalNumberOfWrites) {
            this.totalNumberOfHits = totalNumberOfHits;
            this.totalNumberOfMisses = totalNumberOfMisses;
            this.totalNumberOfWrites = totalNumberOfWrites;
        }

        public int getTotalNumberOfHits() {
            return totalNumberOfHits;
        }

        public int getTotalNumberOfMisses() {
            return totalNumberOfMisses;
        }

        public int getTotalNumberOfWrites() {
            return totalNumberOfWrites;
        }
    }

    private final Set<Object> keys = new HashSet<>(1024);
    private final Map<Object, AtomicInteger> misses = new HashMap<>(1024);
    private final Map<Object, AtomicInteger> hits = new HashMap<>(1024);
    private final Map<Object, AtomicInteger> writes = new HashMap<>(1024);

    public void reportMiss(Object key) {
        if (key == null) {
            throw new IllegalArgumentException("Method argument key must not be null.");
        }
        this.keys.add(key);
        getMisses(key).incrementAndGet();
    }


    public void reportHit(Object key) {
        if (key == null) {
            throw new IllegalArgumentException("Method argument key must not be null.");
        }
        this.keys.add(key);
        getHits(key).incrementAndGet();
    }


    public void reportWrite(Object key) {
        if (key == null) {
            throw new IllegalArgumentException("Method argument key must not be null.");
        }
        this.keys.add(key);
        getWrites(key).incrementAndGet();
    }

    private AtomicInteger getOrCreate(Object key, Map<Object, AtomicInteger> map) {
        AtomicInteger i = map.get(key);
        if (i == null) {
            i = new AtomicInteger(0);
            map.put(key, i);
        }
        return i;
    }

    private AtomicInteger getMisses(Object key) {
        return getOrCreate(key, this.misses);
    }

    private AtomicInteger getHits(Object key) {
        return getOrCreate(key, this.hits);
    }

    private AtomicInteger getWrites(Object key) {
        return getOrCreate(key, this.writes);
    }

    public List<KeyReport> getKeyReports() {
        List<KeyReport> keyReports = new ArrayList<>(this.keys.size());
        for (Object key: this.keys) {
            KeyReport keyReport = new KeyReport(key,
                                       getMisses(key).intValue(),
                                       getHits(key).intValue(),
                                       getWrites(key).intValue());
            keyReports.add(keyReport);
        }

        sort(keyReports, (o1, o2) -> o2.hits - o1.hits);

        return keyReports;
    }

    public ReportSummary getReportSummary() {
        int totalHits = 0, totalMisses = 0, totalWrites = 0;
        for (Object key: this.keys) {
            totalHits += getHits(key).intValue();
            totalMisses += getMisses(key).intValue();
            totalWrites += getWrites(key).intValue();
        }
        return new ReportSummary(totalHits, totalMisses, totalWrites);
    }
}
