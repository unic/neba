package io.neba.core;

import static java.lang.System.currentTimeMillis;
import static java.lang.Thread.sleep;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * A test mixin for cases requiring {@link #eventually(Runnable) eventual} assertions.
 *
 * @author Olaf Otto
 */
public interface Eventual {

    /**
     * Expects the execution of the provided runnable to stop failing within ten seconds, trying every 100 milliseconds.
     *
     * @param runnable not null. Expected to throw an exception should the embodied assertions fail.
     */
    default void eventually(Runnable runnable) {
        long max = SECONDS.toMillis(waitUntilSeconds()),
                waited = 0,
                interval = intervalInMillis();

        Throwable issue = null;

        while (waited < max) {
            try {
                runnable.run();
                return;
            } catch (Throwable t) {
                issue = t;
                long timeBeforeSleep = currentTimeMillis();
                try {
                    sleep(interval);
                } catch (InterruptedException e1) {
                    // continue
                }
                waited += (currentTimeMillis() - timeBeforeSleep);
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
