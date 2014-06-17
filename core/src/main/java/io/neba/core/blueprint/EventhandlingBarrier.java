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

package io.neba.core.blueprint;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Contains a global {@link Lock} on which the event handlers changing the
 * framework state (e.g. registered {@link io.neba.api.annotations.ResourceModel models} or self tests)
 * may synchronize to prevent undefined state due to concurrent modifications.<br />
 * {@link #begin()} must always be followed by {@link #end()}, i.e. {@link #end()} should
 * be included in a finally block like so:<br />
 * 
 * <pre>
 * EventHandling.begin();
 * try {
 *    // do something
 * } finally {
 *    EventHandling.end();
 * }
 * </pre>
 * 
 * @author Olaf Otto
 */
public class EventhandlingBarrier {
    private static final Lock LOCK = new ReentrantLock();

    /**
     * Tries to obtain a lock. Waits up to ten minutes for the locking to succeed, or fails
     * with an {@link IllegalStateException}. Rationale: Waiting indefinitely for a lock is bad practice,
     * since it enables deadlocks.
     */
    public static void begin() {
        boolean locked;
        try {
            locked = LOCK.tryLock(10, MINUTES);
        } catch (InterruptedException e) {
            throw new IllegalStateException("Interrupted while attempting to obtain the event handling lock.", e);
        }
        if (!locked) {
            throw new IllegalStateException("Unable to obtain the event handling lock within ten minutes, giving up. " +
                                            "This may indicate a deadlocked process. Please create " +
                                             "a thread dump and consult the error.log.");
        }
    }
    
    /**
     * This method tries to obtain a lock and return <code>false</code> if this
     * does not succeed. This method can be used if the
     * execution of the synchronous code is optional and may otherwise
     * result in a deadlock.
     * 
     * @see Lock#tryLock(long, java.util.concurrent.TimeUnit)
     */
    public static boolean tryBegin() {
        try {
            return LOCK.tryLock(10, SECONDS);
        } catch (InterruptedException e) {
            return false;
        }
    }

    /**
     * @see #begin()
     */
    public static void end() {
        LOCK.unlock();
    }

    private EventhandlingBarrier() {}
}
