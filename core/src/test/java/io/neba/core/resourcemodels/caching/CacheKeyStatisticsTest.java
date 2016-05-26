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

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Olaf Otto
 */
public class CacheKeyStatisticsTest {
    private Object key = "JunitTestKey";

    private CacheKeyStatistics testee;

    @Before
    public void setUp() throws Exception {
        this.testee = new CacheKeyStatistics();
    }

    @Test
    public void testWrite() throws Exception {
        assertStatisticsReportIsEmpty();

        reportWrite();

        assertNumberOfReportsIs(1);
        assertReportToStringIs(0, "JunitTestKey: misses=0, hits=0, writes=1");

        reportWrite();

        assertNumberOfReportsIs(1);
        assertReportToStringIs(0, "JunitTestKey: misses=0, hits=0, writes=2");
    }

    @Test
    public void testMiss() throws Exception {
        assertStatisticsReportIsEmpty();

        reportMiss();

        assertNumberOfReportsIs(1);
        assertReportToStringIs(0, "JunitTestKey: misses=1, hits=0, writes=0");

        reportMiss();

        assertNumberOfReportsIs(1);
        assertReportToStringIs(0, "JunitTestKey: misses=2, hits=0, writes=0");
    }

    @Test
    public void testHit() throws Exception {
        assertStatisticsReportIsEmpty();

        reportHit();

        assertNumberOfReportsIs(1);
        assertReportToStringIs(0, "JunitTestKey: misses=0, hits=1, writes=0");

        reportHit();

        assertNumberOfReportsIs(1);
        assertReportToStringIs(0, "JunitTestKey: misses=0, hits=2, writes=0");
    }

    @Test
    public void testAdditionOfDifferentKeys() throws Exception {
        assertStatisticsReportIsEmpty();

        withKey("JunitTestKey1");
        reportHit();

        assertNumberOfReportsIs(1);
        assertReportToStringIs(0, "JunitTestKey1: misses=0, hits=1, writes=0");

        withKey("JunitTestKey2");
        reportHit();

        assertNumberOfReportsIs(2);
        assertReportToStringIs(0, "JunitTestKey1: misses=0, hits=1, writes=0");
        assertReportToStringIs(1, "JunitTestKey2: misses=0, hits=1, writes=0");
    }

    private void withKey(String key) {
        this.key = key;
    }

    private void reportHit() {
        this.testee.reportHit(this.key);
    }

    private void reportMiss() {
        this.testee.reportMiss(this.key);
    }

    private void reportWrite() {
        this.testee.reportWrite(this.key);
    }

    private void assertReportToStringIs(int index, String expected) {
        assertThat(this.testee.getKeyReports().get(index).toString()).isEqualTo(expected);
    }

    private void assertNumberOfReportsIs(int expected) {
        assertThat(this.testee.getKeyReports()).hasSize(expected);
    }

    private void assertStatisticsReportIsEmpty() {
        assertThat(this.testee.getKeyReports()).isEmpty();
    }
}
