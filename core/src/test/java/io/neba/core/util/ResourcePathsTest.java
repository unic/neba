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
package io.neba.core.util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.function.Function;

import static io.neba.core.util.ResourcePaths.path;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * @author Olaf Otto
 */
@RunWith(MockitoJUnitRunner.class)
public class ResourcePathsTest {
    @Mock
    private Function<String, String> resolver;

    private String resolvedValue;

    @Test
    public void testNonVariablesAreNotResolved() {
        replace("");
        assertReplacedValueIs("");

        replace("$");
        assertReplacedValueIs("$");

        replace("${");
        assertReplacedValueIs("${");

        replace("${key");
        assertReplacedValueIs("${key");

        replace("${key${value");
        assertReplacedValueIs("${key${value");

        verifyNoResolutionAttemptIsMade();
    }

    @Test
    public void testVariableResolution() {
        withResolution("key", "value");

        replace("${key}");
        verifyResolverResolves("key");
        assertReplacedValueIs("value");

        withResolution("key2", "value2");

        replace("/${key}/${key2}/");
        verifyResolverResolves("key");
        verifyResolverResolves("key2");
        assertReplacedValueIs("/value/value2/");

        replace("test-${key}");
        assertReplacedValueIs("test-value");
    }

    @Test
    public void testDetectionOfVariables() {
        assertThat(path("").hasPlaceholders()).isFalse();
        assertThat(path("${variable}").hasPlaceholders()).isTrue();
    }

    @Test
    public void testPathRepresentationOfUnresolvedPathWithPlaceholders() {
        assertThat(path("/some/${placeholders}/in/${path}").getPath())
             .isEqualTo("/some/${placeholders}/in/${path}");
    }

    @Test
    public void testToStringOfPlaceWithoutPlaceholders() {
        assertThat(path("/path/without/placeholders"))
           .hasToString("/path/without/placeholders");
    }

    @Test
    public void testToStringOfPlaceWithPlaceholders() {
        assertThat(path("/path/${with}/placeholders"))
           .hasToString("/path/${with}/placeholders");
    }

    private void assertReplacedValueIs(String k) {
        assertThat(this.resolvedValue).isEqualTo(k);
    }

    private void verifyResolverResolves(String k) {
        verify(this.resolver, atLeast(1)).apply(k);
    }

    private void verifyNoResolutionAttemptIsMade() {
        verify(this.resolver, never()).apply(anyString());
    }

    private void withResolution(String k, Object v) {
        doReturn(v).when(this.resolver).apply(k);
    }

    private void replace(String s) {
        this.resolvedValue = path(s).resolve(this.resolver).getPath();
    }
}