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

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;

import io.neba.core.util.OsgiBeanReference;
import io.neba.core.util.OsgiBeanSource;

/**
 * @author Olaf Otto
 */
@RunWith(MockitoJUnitRunner.class)
public class ReferenceConsistencyCheckerTest {
	@Mock
	private OsgiBeanReference<?> reference;
	@Mock
    private OsgiBeanSource<?> source;
	@Mock
    private BundleContext context;
	@Mock
    private Bundle bundle;

	private long bundleId = 123L;
    private boolean isValid;

	@InjectMocks
    private ReferenceConsistencyChecker testee;

    @Before
    public void prepareBundleContext() {
        when(this.context.getBundle(eq(this.bundleId))).thenReturn(this.bundle);
        when(this.reference.getBundleId()).thenReturn(this.bundleId);
        when(this.source.getBundleId()).thenReturn(this.bundleId);
    }

    @Test
    public void testValidityOfReferenceToActiveBundle() throws Exception {
        withActiveBundle();
        checkValidityOfReference();
        assertReferenceIsValid();
        
        checkValidityOfSource();
        assertReferenceIsValid();
    }

    @Test
    public void testValidityOfReferenceToInstalledBundle() throws Exception {
        withInstalledBundle();
        checkValidityOfReference();
        assertReferenceIsInvalid();

        checkValidityOfSource();
        assertReferenceIsInvalid();
    }

    private void withInstalledBundle() {
        when(this.bundle.getState()).thenReturn(Bundle.INSTALLED);
    }

    private void checkValidityOfReference() {
        this.isValid = this.testee.isValid(this.reference);
    }

    private void checkValidityOfSource() {
        this.isValid = this.testee.isValid(this.source);
    }

    private void withActiveBundle() {
        when(this.bundle.getState()).thenReturn(Bundle.ACTIVE);
    }

    private void assertReferenceIsInvalid() {
        assertThat(this.isValid).isFalse();
    }

    private void assertReferenceIsValid() {
        assertThat(this.isValid).isTrue();
    }
}
