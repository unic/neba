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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Olaf Otto
 */
public class KeyTest {
	private Key key1;
	private Key key2;

	@Test
	public void testEqualKeys() throws Exception {
		withFirstKey("one", 2, "three");
		withSecondKey("one", 2, "three");
		assertKeysAreEqual();
	}
	
	@Test
	public void testUnequalKeys() throws Exception {
		withFirstKey("one", 2, 3);
		withSecondKey("one", 2, "three");
		assertKeysAreNotEqual();
	}

    @Test
    public void testNullValuesInKeysAreNotIgnored() throws Exception {
        withFirstKey("one", 2, null);
        withSecondKey("one", 2);
        assertKeysAreNotEqual();
    }

    @Test
    public void testHandlingOfNullKeys() throws Exception {
        withFirstKey((Object) null);
        withSecondKey((Object) null);
        assertKeysAreEqual();

        withFirstKey((Object[]) null);
        withSecondKey((Object[]) null);
        assertKeysAreEqual();
    }

    @Test
    public void testStringRepresentation() throws Exception {
        withFirstKey("one", 2, null);
        assertStringRepresentationIs("Key {one, 2, }");
    }

    private void assertStringRepresentationIs(String s) {
        assertThat(this.key1.toString()).isEqualTo(s);
    }

    private void assertKeysAreNotEqual() {
        assertThat(this.key1.hashCode()).isNotEqualTo(this.key2.hashCode());
		assertThat(this.key1).isNotEqualTo(key2);
		assertThat(this.key2).isNotEqualTo(key1);
	}

	private void assertKeysAreEqual() {
        assertThat(this.key1.hashCode()).isEqualTo(this.key2.hashCode());
		assertThat(this.key1).isEqualTo(key2);
		assertThat(this.key2).isEqualTo(key1);
	}
	
	private void withFirstKey(Object... contents) {
		this.key1 = new Key(contents);
	}

	private void withSecondKey(Object... contents) {
		this.key2 = new Key(contents);
	}
}
