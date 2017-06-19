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
package io.neba.core.resourcemodels.registration;

import java.lang.reflect.Field;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.service.event.Event;
import org.slf4j.Logger;


import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.commons.lang3.reflect.FieldUtils.getField;
import static org.apache.commons.lang3.reflect.FieldUtils.writeField;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * @author Olaf Otto
 */
@RunWith(MockitoJUnitRunner.class)
public class MappableTypeHierarchyChangeListenerTest {
    @Mock
    private ModelRegistry modelRegistry;

    @Mock
    private Logger logger;

    @InjectMocks
    private MappableTypeHierarchyChangeListener testee;

    @Before
    public void setUp() throws Exception {
        Field field = getField(MappableTypeHierarchyChangeListener.class, "logger", true);
        writeField(field, this.testee, this.logger);
    }

    @Test
    public void testChangeListenerShutsDownExecutorUponDeactivation() throws Exception {
        activate();
        deactivate();

        withChangeOn("/apps/testapp/components/test");
        sleep();

        verifyModelRegistryCacheIsNotCleared();
    }

    @Test
    public void testActivateListenerClearsModelRegistryUponEvent() throws Exception {
        activate();

        withChangeOn("/apps/testapp/components/test");
        sleep();

        verifyModelRegistryCacheIsCleared();
    }

    /**
     * When multiple successive events are handled, only the first one shall cause the cache to be cleared, i.e.
     * events do not queue up while the cache is cleared, as it is sufficient to clear the cache once.
     */
    @Test
    public void testOnlyOneEventIsAccepted() throws Exception {
        withChangeOn("/apps/testapp/components/test");
        withChangeOn("/apps/testapp/components/test");
        withChangeOn("/apps/testapp/components/test");
        withChangeOn("/apps/testapp/components/test");
        withChangeOn("/apps/testapp/components/test");

        activate();
        sleep();

        verifyModelRegistryCacheIsCleared();
    }

    @Test
    public void testLoggingOfInvalidatingChangeWhenLogLevelIsTrace() throws Exception {
        activate();

        withTraceLogging();
        withChangeOn("/apps/testapp/components/test");

        sleep();

        verifyLoggerTraces(
                "Invalidating the resource model registry lookup cache due to changes to {}.",
                "/apps/testapp/components/test");
    }

    @Test
    public void testLoggingOfInterruptionWithoutShutdown() throws Exception {
        withInterruptedExceptionThrownWhileBlocked();
        activate();

        withChangeOn("/some/path");

        sleep();

        verifyLoggerDebugs("The type hierarchy change listener got interrupted, but was not shut down.");
    }

    private void verifyLoggerDebugs(String message) {
        verify(this.logger, atLeast(1)).debug(eq(message), isA(InterruptedException.class));
    }

    private void withInterruptedExceptionThrownWhileBlocked() throws IllegalAccessException, InterruptedException {
        Field field = getField(MappableTypeHierarchyChangeListener.class, "invalidationRequests", true);
        BlockingQueue queue = mock(BlockingQueue.class);
        writeField(field, this.testee, queue);
        doThrow(new InterruptedException("THIS IS AN EXPECTED TEST EXCEPTION"))
                .when(queue)
                .poll(anyLong(), isA(TimeUnit.class));
    }

    private void verifyLoggerTraces(String format, String arg) {
        verify(this.logger).trace(
                format,
                arg);
    }

    private void withTraceLogging() {
        doReturn(true).when(this.logger).isTraceEnabled();
    }

    private void verifyModelRegistryCacheIsClearedAtMost(int times) {
        verify(this.modelRegistry, atMost(times)).clearLookupCaches();
    }

    private void verifyModelRegistryCacheIsCleared() {
        verify(this.modelRegistry).clearLookupCaches();
    }

    private void activate() {
        this.testee.activate();
    }

    private void verifyModelRegistryCacheIsNotCleared() {
        verify(this.modelRegistry, never()).clearLookupCaches();
    }

    private void sleep() throws InterruptedException {
        Thread.sleep(SECONDS.toMillis(2));
    }

    private void withChangeOn(String path) {
        Dictionary<String, Object> properties = new Hashtable<>();
        properties.put("path", path);
        Event event = new Event("test/topic", properties);
        this.testee.handleEvent(event);
    }

    private void deactivate() {
        this.testee.deactivate();
    }
}