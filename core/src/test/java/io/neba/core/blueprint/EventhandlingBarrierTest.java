package io.neba.core.blueprint;


import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Semaphore;

import static java.util.concurrent.Executors.newFixedThreadPool;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Olaf Otto
 */
public class EventhandlingBarrierTest {
    private ExecutorService executorService;

    @Before
    public void setUp() throws Exception {
        executorService = newFixedThreadPool(2);
    }

    @After
    public void tearDown() throws Exception {
        this.executorService.shutdownNow();
    }

    @Test
    public void testBeginAndEnd() throws Exception {
        Semaphore obtainLock = new Semaphore(0);
        Semaphore releaseLock = new Semaphore(0);

        // Obtain a global event handling lock, then proceed with test execution. Do not release the event lock until
        // a permit to do so become available.
        executorService.submit(() -> {
            try {
                EventhandlingBarrier.begin();
                obtainLock.release();
                releaseLock.acquire();
            } finally {
                EventhandlingBarrier.end();
            }

            return null;
        });

        assertThat(obtainLock
                .tryAcquire(5, SECONDS))
                .describedAs("Obtaining the global event handling lock should not block, but blocked.").
                isTrue();

        // Try Obtaining a global event handling lock a second time. This must block as the lock was already acquired.
        executorService.submit(() -> {
            try {
                EventhandlingBarrier.begin();
                obtainLock.release();
            } finally {
                EventhandlingBarrier.end();
            }
        });

        assertThat(obtainLock
                .tryAcquire(5, SECONDS))
                .describedAs("Obtaining the global event handling lock for a second time  must block, but did not.").
                isFalse();

        // Allow releasing the global event lock. Subsequently, obtaining the lock should no longer block.
        releaseLock.release();

        assertThat(obtainLock
                .tryAcquire(5, SECONDS))
                .describedAs("Obtaining the global event handling lock should not block after it was released by its previous owner.").
                isTrue();
    }

    @Test
    public void testTryBegin() throws Exception {
        Semaphore obtainLock = new Semaphore(0);
        Semaphore releaseLock = new Semaphore(0);

        // Obtain a global event handling lock, then proceed with test execution. Do not release the event lock until
        // a permit to do so become available.
        executorService.submit(() -> {
            try {
                EventhandlingBarrier.tryBegin();
                obtainLock.release();
                releaseLock.acquire();
            } finally {
                EventhandlingBarrier.end();
            }
            return null;
        });

        assertThat(obtainLock
                .tryAcquire(5, SECONDS))
                .describedAs("Obtaining the global event handling lock should not block, but blocked.").
                isTrue();

        // Try Obtaining a global event handling lock a second time. This must block for 10 seconds as the lock was already acquired.
        executorService.submit(() -> {
            try {
                EventhandlingBarrier.tryBegin();
                obtainLock.release();
            } finally {
                EventhandlingBarrier.end();
            }
        });

        assertThat(obtainLock.tryAcquire(15, SECONDS))
                .describedAs("Obtaining the global event handling lock for a second time must block for up to 10 seconds, but was still blocked after 15 seconds.").
                isTrue();

        // Clean up: allow the lock owning thread to terminate gracefully.
        releaseLock.release();
    }
}