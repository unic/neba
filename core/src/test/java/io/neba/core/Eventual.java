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

package io.neba.core;

import static java.lang.Thread.sleep;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * A test mixin for cases requiring {@link #eventually(MayFail) eventual} assertions.
 *
 * @author Olaf Otto
 */
public interface Eventual {

    @FunctionalInterface
    interface MayFail {
        void run() throws Exception;
    }

    /**
     * Expects the execution of the provided runnable to stop failing within ten seconds, trying every 100 milliseconds.
     *
     * @param mayFail not null. Expected to throw an exception should the embodied assertions fail.
     */
    default void eventually(MayFail mayFail) throws InterruptedException {
        long max = SECONDS.toMillis(waitUntilSeconds()),
                waited = 0,
                interval = intervalInMillis();

        Throwable issue = null;

        while (waited < max) {
            try {
                mayFail.run();
                return;
            } catch (Throwable t) {
                issue = t;
                sleep(interval);
                waited += interval;
            }
        }

        throw new AssertionError("Unable to satisfy within " + MILLISECONDS.toSeconds(max) + " seconds.", issue);
    }

    default int waitUntilSeconds() {
        return 10;
    }

    default int intervalInMillis() {
        return 100;
    }
}
