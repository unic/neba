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

package io.neba.core.util;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * @author Olaf Otto
 */
@RunWith(MockitoJUnitRunner.class)
public class BundleUtilTest {
	@Mock
	private Bundle bundle;
	
	private String displayName;

    @Test
	public void testDisplayNameCreation() throws Exception {
		withVersion("1.0.0");
		withSymbolicName("symbolic name");

		getDisplayNameOfBundle();
		
		assertDisplayNameIs("symbolic name 1.0.0");
	}

    @Test(expected = IllegalArgumentException.class)
    public void testHandlingOfNullBundle() throws Exception {
        withBundle(null);
        getDisplayNameOfBundle();
    }

    private void withBundle(Bundle o) {
        this.bundle = o;
    }

    private void withSymbolicName(String value) {
		when(this.bundle.getSymbolicName()).thenReturn(value);
	}

	private void withVersion(String name) {
        Version version = new Version(name);
		when(this.bundle.getVersion()).thenReturn(version);
	}

	private void getDisplayNameOfBundle() {
		this.displayName = BundleUtil.displayNameOf(this.bundle);
	}

	private void assertDisplayNameIs(String value) {
		assertThat(this.displayName).isEqualTo(value);
	}
}
